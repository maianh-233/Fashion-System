package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.filestorage.FileStorageService;
import com.fashionsystem.fashion_system.filestorage.FileStorageService.StoredDocument;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Document;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.SignedUrl;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Tài liệu của ứng viên (CV, thư xin việc, portfolio, chứng chỉ). File nằm ở kho private qua {@link FileStorageService};
 * database chỉ giữ metadata + storage key. Tải bản mới cùng loại = phiên bản mới, bản cũ vẫn xem được.
 * Kiểm tra quyền do service gọi tới đảm nhận.
 */
@Service
@Transactional(readOnly = true)
public class CandidateDocumentService {
    static final String[] DOC_TYPES = {"CV", "COVER_LETTER", "PORTFOLIO", "CERTIFICATE", "OTHER"};

    private final NamedParameterJdbcTemplate jdbc;
    private final FileStorageService files;

    public CandidateDocumentService(NamedParameterJdbcTemplate jdbc, FileStorageService files) {
        this.jdbc = jdbc;
        this.files = files;
    }

    public List<Document> list(UUID candidateId) {
        return jdbc.query("""
                select d.*, u.full_name as uploaded_by_name from candidate_documents d
                left join users u on u.id = d.uploaded_by
                where d.candidate_id = :c and d.deleted_at is null
                order by case d.doc_type when 'CV' then 0 when 'COVER_LETTER' then 1 when 'PORTFOLIO' then 2 else 3 end,
                         d.version desc""", new MapSqlParameterSource("c", candidateId), (rs, i) -> new Document(
                rs.getObject("id", UUID.class), rs.getString("doc_type"), rs.getInt("version"), rs.getBoolean("is_current"),
                rs.getString("file_name"), rs.getString("content_type"), rs.getLong("size_bytes"), rs.getString("checksum"),
                rs.getString("storage_provider"), rs.getString("note"), rs.getObject("application_id", UUID.class),
                rs.getString("uploaded_by_name"), ApplicationWorkflow.time(rs, "uploaded_at")));
    }

    /**
     * Tải lên (hoặc thay thế) tài liệu: file được lưu trước, metadata ghi sau trong transaction; transaction lỗi thì
     * object vừa tải bị xóa để không để lại file mồ côi.
     */
    @Transactional
    public Document upload(UUID candidateId, MultipartFile file, String docType, String note, UUID applicationId, UUID actor) {
        String type = ApplicationWorkflow.requireOneOf(docType == null ? "CV" : docType, "Loại tài liệu", DOC_TYPES);
        if (applicationId != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from job_applications where id = :a and candidate_id = :c)",
                new MapSqlParameterSource("a", applicationId).addValue("c", candidateId), Boolean.class)))
            throw BusinessException.badRequest("Hồ sơ ứng tuyển không thuộc ứng viên này");
        // Khóa ứng viên để đánh số phiên bản không bị trùng khi tải song song.
        jdbc.queryForList("select id from candidates where id = :c for update", new MapSqlParameterSource("c", candidateId), UUID.class);
        StoredDocument stored = files.store(file, "recruitment/candidates/" + candidateId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) files.deleteQuietly(stored.storageKey());
                }
            });
        }
        Integer version = jdbc.queryForObject("""
                select coalesce(max(version), 0) + 1 from candidate_documents where candidate_id = :c and doc_type = :t""",
                new MapSqlParameterSource("c", candidateId).addValue("t", type), Integer.class);
        jdbc.update("update candidate_documents set is_current = false where candidate_id = :c and doc_type = :t and is_current",
                new MapSqlParameterSource("c", candidateId).addValue("t", type));
        UUID id = jdbc.queryForObject("""
                insert into candidate_documents (candidate_id, application_id, doc_type, version, is_current, file_name,
                    content_type, size_bytes, checksum, storage_provider, storage_key, note, uploaded_by)
                values (:c, :a, :t, :v, true, :name, :ct, :size, :checksum, :provider, :key, :note, :actor) returning id""",
                new MapSqlParameterSource("c", candidateId).addValue("a", applicationId).addValue("t", type)
                        .addValue("v", version).addValue("name", stored.fileName()).addValue("ct", stored.contentType())
                        .addValue("size", stored.size()).addValue("checksum", stored.checksum())
                        .addValue("provider", stored.provider()).addValue("key", stored.storageKey())
                        .addValue("note", ApplicationWorkflow.cut(ApplicationWorkflow.trim(note), 500)).addValue("actor", actor), UUID.class);
        jdbc.update("update candidates set updated_at = now() at time zone 'UTC' where id = :c", new MapSqlParameterSource("c", candidateId));
        return list(candidateId).stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }

    /** Link đã ký có hạn; {@code inline} để xem trước (PDF / ảnh), ngược lại tải xuống. */
    public SignedUrl signedUrl(UUID candidateId, UUID documentId, boolean inline) {
        record Row(String key, String name, String type) {
        }
        List<Row> rows = jdbc.query("""
                select storage_key, file_name, content_type from candidate_documents
                where id = :d and candidate_id = :c and deleted_at is null""",
                new MapSqlParameterSource("d", documentId).addValue("c", candidateId),
                (rs, i) -> new Row(rs.getString("storage_key"), rs.getString("file_name"), rs.getString("content_type")));
        if (rows.isEmpty()) throw BusinessException.notFound("Tài liệu không tồn tại");
        Row row = rows.getFirst();
        var link = files.signedUrl(row.key(), row.name(), row.type(), inline);
        return new SignedUrl(link.url(), link.expiresInSeconds());
    }

    /** Xóa một phiên bản; nếu là bản hiện hành thì bản mới nhất còn lại trở thành hiện hành. */
    @Transactional
    public void delete(UUID candidateId, UUID documentId) {
        record Row(String key, String type, boolean current) {
        }
        List<Row> rows = jdbc.query("""
                select storage_key, doc_type, is_current from candidate_documents
                where id = :d and candidate_id = :c and deleted_at is null for update""",
                new MapSqlParameterSource("d", documentId).addValue("c", candidateId),
                (rs, i) -> new Row(rs.getString("storage_key"), rs.getString("doc_type"), rs.getBoolean("is_current")));
        if (rows.isEmpty()) throw BusinessException.notFound("Tài liệu không tồn tại");
        Row row = rows.getFirst();
        jdbc.update("update candidate_documents set deleted_at = now() at time zone 'UTC', is_current = false where id = :d",
                new MapSqlParameterSource("d", documentId));
        if (row.current()) {
            jdbc.update("""
                    update candidate_documents set is_current = true where id = (
                        select id from candidate_documents where candidate_id = :c and doc_type = :t and deleted_at is null
                        order by version desc limit 1)""", new MapSqlParameterSource("c", candidateId).addValue("t", row.type()));
        }
        // Xóa object thật sau khi commit (rollback thì file vẫn còn).
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    files.deleteQuietly(row.key());
                }
            });
        }
    }
}
