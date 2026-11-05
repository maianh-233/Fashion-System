-- Quản lý tuyển dụng: chiến dịch → vị trí (mỗi vị trí có quy trình vòng + tiêu chí riêng, cấu hình động) → hồ sơ
-- ứng tuyển của ứng viên (một ứng viên ứng tuyển nhiều vị trí, mỗi lần có trạng thái + lịch sử riêng) → kết quả từng
-- vòng, phiếu đánh giá của từng người, lịch phỏng vấn, offer, chuyển thành nhân viên.
-- File CV / tài liệu nằm ở kho private (R2/S3); bảng candidate_documents chỉ giữ metadata + storage key.
-- Phạm vi cửa hàng: mỗi chiến dịch thuộc một cửa hàng (store_id) hoặc khối văn phòng / toàn chuỗi (store_id null).
-- Nhân viên cửa hàng chỉ thấy tuyển dụng của cửa hàng mình (và chiến dịch chuỗi mà họ được phân công).

CREATE SEQUENCE recruitment_campaign_code_seq;
CREATE SEQUENCE recruitment_position_code_seq;
CREATE SEQUENCE candidate_code_seq;
CREATE SEQUENCE job_application_code_seq;

CREATE TABLE recruitment_campaigns (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(30) NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    store_id      UUID REFERENCES stores(id),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    owner_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    start_date    DATE NOT NULL,
    end_date      DATE,
    description   TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT', 'OPEN', 'PAUSED', 'CLOSED', 'CANCELLED')),
    created_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at    TIMESTAMP,
    closed_at     TIMESTAMP,
    CONSTRAINT chk_recruitment_campaigns_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_recruitment_campaigns_status ON recruitment_campaigns (status, created_at DESC);
CREATE INDEX idx_recruitment_campaigns_store ON recruitment_campaigns (store_id);

-- Người được phân công vào chiến dịch (HR Recruiter, Hiring Manager); người phụ trách (owner) luôn có quyền.
CREATE TABLE recruitment_campaign_members (
    campaign_id UUID NOT NULL REFERENCES recruitment_campaigns(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    member_role VARCHAR(20) NOT NULL CHECK (member_role IN ('RECRUITER', 'HIRING_MANAGER')),
    PRIMARY KEY (campaign_id, user_id)
);
CREATE INDEX idx_recruitment_campaign_members_user ON recruitment_campaign_members (user_id);

CREATE TABLE recruitment_positions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id         UUID NOT NULL REFERENCES recruitment_campaigns(id) ON DELETE CASCADE,
    code                VARCHAR(30) NOT NULL UNIQUE,
    title               VARCHAR(200) NOT NULL,
    department_id       UUID REFERENCES departments(id) ON DELETE SET NULL,
    org_position_id     UUID REFERENCES positions(id) ON DELETE SET NULL,
    hiring_manager_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    headcount           INTEGER NOT NULL CHECK (headcount > 0),
    employment_type     VARCHAR(30),
    work_location       VARCHAR(200),
    job_description     TEXT,
    requirements        TEXT,
    benefits            TEXT,
    skills              TEXT[] NOT NULL DEFAULT '{}',
    experience_min      NUMERIC(4, 1) CHECK (experience_min >= 0),
    experience_max      NUMERIC(4, 1) CHECK (experience_max >= 0),
    salary_min          NUMERIC(14, 2) CHECK (salary_min >= 0),
    salary_max          NUMERIC(14, 2) CHECK (salary_max >= 0),
    salary_negotiable   BOOLEAN NOT NULL DEFAULT FALSE,
    deadline            DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('DRAFT', 'OPEN', 'PAUSED', 'CLOSED', 'FILLED')),
    is_public           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMP,
    CONSTRAINT chk_recruitment_positions_salary CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_max >= salary_min),
    CONSTRAINT chk_recruitment_positions_exp CHECK (experience_min IS NULL OR experience_max IS NULL OR experience_max >= experience_min)
);
CREATE INDEX idx_recruitment_positions_campaign ON recruitment_positions (campaign_id);
CREATE INDEX idx_recruitment_positions_manager ON recruitment_positions (hiring_manager_id);

-- Các vòng tuyển dụng của một vị trí (số vòng không cố định).
-- pass_rule: AVERAGE_SCORE (điểm TB các phiếu ≥ pass_score), ALL_PASS (mọi phiếu đề xuất Đạt),
-- MAJORITY_PASS (quá nửa phiếu Đạt), MANUAL (người phụ trách tự quyết).
CREATE TABLE recruitment_stages (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    position_id        UUID NOT NULL REFERENCES recruitment_positions(id) ON DELETE CASCADE,
    name               VARCHAR(150) NOT NULL,
    description        TEXT,
    sort_order         INTEGER NOT NULL,
    stage_type         VARCHAR(20) NOT NULL
                       CHECK (stage_type IN ('SCREENING', 'TEST', 'INTERVIEW', 'OFFER', 'OTHER')),
    owner_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    scoring_mode       VARCHAR(20) NOT NULL DEFAULT 'SCORE' CHECK (scoring_mode IN ('SCORE', 'PASS_FAIL')),
    scale_max          NUMERIC(6, 2) NOT NULL DEFAULT 10 CHECK (scale_max > 0),
    pass_score         NUMERIC(6, 2) CHECK (pass_score >= 0),
    pass_rule          VARCHAR(20) NOT NULL DEFAULT 'AVERAGE_SCORE'
                       CHECK (pass_rule IN ('AVERAGE_SCORE', 'ALL_PASS', 'MAJORITY_PASS', 'MANUAL')),
    min_evaluations    INTEGER NOT NULL DEFAULT 1 CHECK (min_evaluations >= 1),
    auto_advance       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at         TIMESTAMP,
    CONSTRAINT chk_recruitment_stages_pass CHECK (pass_score IS NULL OR pass_score <= scale_max)
);
CREATE INDEX idx_recruitment_stages_position ON recruitment_stages (position_id, sort_order);

-- importance CRITICAL = tiêu chí loại trực tiếp: phiếu chấm dưới 50% điểm tối đa (hoặc Không đạt) thì vòng không đạt.
CREATE TABLE recruitment_stage_criteria (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stage_id    UUID NOT NULL REFERENCES recruitment_stages(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    max_score   NUMERIC(6, 2) NOT NULL DEFAULT 10 CHECK (max_score > 0),
    weight      NUMERIC(6, 2) NOT NULL DEFAULT 1 CHECK (weight > 0),
    importance  VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (importance IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    sort_order  INTEGER NOT NULL
);
CREATE INDEX idx_recruitment_stage_criteria_stage ON recruitment_stage_criteria (stage_id, sort_order);

-- Mẫu quy trình để tái sử dụng: danh sách vòng + tiêu chí lưu dạng JSON, áp dụng = sao chép vào vị trí.
CREATE TABLE recruitment_pipeline_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    stages      JSONB NOT NULL,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at  TIMESTAMP
);

CREATE TABLE candidates (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(30) NOT NULL UNIQUE,
    full_name        VARCHAR(200) NOT NULL,
    email            VARCHAR(255),
    phone            VARCHAR(20),
    date_of_birth    DATE,
    gender           VARCHAR(20),
    address          VARCHAR(500),
    current_title    VARCHAR(200),
    current_company  VARCHAR(200),
    years_experience NUMERIC(4, 1) CHECK (years_experience >= 0),
    skills           TEXT[] NOT NULL DEFAULT '{}',
    education        VARCHAR(500),
    summary          TEXT,
    source           VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    source_detail    VARCHAR(200),
    tags             TEXT[] NOT NULL DEFAULT '{}',
    linkedin_url     VARCHAR(500),
    portfolio_url    VARCHAR(500),
    notes            TEXT,
    -- Cửa hàng tiếp nhận ứng viên đầu tiên (null = văn phòng); nhân viên cửa hàng thấy ứng viên của cửa hàng mình.
    store_id         UUID REFERENCES stores(id),
    created_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at       TIMESTAMP,
    deleted_at       TIMESTAMP,
    CONSTRAINT chk_candidates_contact CHECK (email IS NOT NULL OR phone IS NOT NULL)
);
-- Phát hiện trùng: email (không phân biệt hoa thường) và số điện thoại chỉ giữ chữ số.
CREATE INDEX idx_candidates_email ON candidates (lower(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_candidates_phone ON candidates (regexp_replace(phone, '\D', '', 'g')) WHERE deleted_at IS NULL;
CREATE INDEX idx_candidates_created ON candidates (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_candidates_store ON candidates (store_id) WHERE deleted_at IS NULL;

CREATE TABLE job_applications (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(30) NOT NULL UNIQUE,
    candidate_id      UUID NOT NULL REFERENCES candidates(id),
    position_id       UUID NOT NULL REFERENCES recruitment_positions(id),
    status            VARCHAR(30) NOT NULL CHECK (status IN (
                          'NEW', 'SCREENING_PENDING', 'PROCESSING', 'QUALIFIED', 'REJECTED', 'INTERVIEW_PENDING',
                          'INTERVIEWING', 'EVALUATION_PENDING', 'ROUND_PASSED', 'ROUND_FAILED', 'OFFER_PENDING',
                          'OFFER_SENT', 'OFFER_ACCEPTED', 'OFFER_DECLINED', 'HIRED', 'WITHDRAWN')),
    current_stage_id  UUID REFERENCES recruitment_stages(id) ON DELETE SET NULL,
    source            VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    cover_note        TEXT,
    applied_at        TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    closed_reason     VARCHAR(500),
    hired_employee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    hired_at          TIMESTAMP,
    created_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_at        TIMESTAMP,
    CONSTRAINT uq_job_applications_candidate_position UNIQUE (candidate_id, position_id)
);
CREATE INDEX idx_job_applications_position_status ON job_applications (position_id, status);
CREATE INDEX idx_job_applications_candidate ON job_applications (candidate_id);
CREATE INDEX idx_job_applications_applied ON job_applications (applied_at DESC);

-- Tài liệu của ứng viên: CV, thư xin việc, portfolio... Mỗi lần thay thế tạo phiên bản mới (version tăng dần theo
-- ứng viên + loại), bản cũ còn giữ để xem lịch sử. Không lưu nội dung file.
CREATE TABLE candidate_documents (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id   UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    application_id UUID REFERENCES job_applications(id) ON DELETE SET NULL,
    doc_type       VARCHAR(20) NOT NULL CHECK (doc_type IN ('CV', 'COVER_LETTER', 'PORTFOLIO', 'CERTIFICATE', 'OTHER')),
    version        INTEGER NOT NULL CHECK (version >= 1),
    is_current     BOOLEAN NOT NULL DEFAULT TRUE,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(150) NOT NULL,
    size_bytes     BIGINT NOT NULL CHECK (size_bytes >= 0),
    checksum       VARCHAR(64) NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    storage_key    VARCHAR(500) NOT NULL UNIQUE,
    note           VARCHAR(500),
    uploaded_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at    TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    deleted_at     TIMESTAMP,
    CONSTRAINT uq_candidate_documents_version UNIQUE (candidate_id, doc_type, version)
);
CREATE INDEX idx_candidate_documents_candidate ON candidate_documents (candidate_id, doc_type, version DESC);

-- Kết quả của hồ sơ ở từng vòng (trạng thái vòng tách khỏi trạng thái tổng thể của hồ sơ).
CREATE TABLE application_stage_results (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    stage_id       UUID NOT NULL REFERENCES recruitment_stages(id) ON DELETE CASCADE,
    status         VARCHAR(30) NOT NULL CHECK (status IN (
                       'PENDING', 'IN_PROGRESS', 'AWAITING_EVALUATION', 'PASSED', 'FAILED', 'SKIPPED')),
    score          NUMERIC(6, 2),
    decided_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    decided_at     TIMESTAMP,
    decision_note  VARCHAR(1000),
    started_at     TIMESTAMP,
    CONSTRAINT uq_application_stage_results UNIQUE (application_id, stage_id)
);
CREATE INDEX idx_application_stage_results_stage ON application_stage_results (stage_id, status);

-- Người được phân công đánh giá hồ sơ ở một vòng.
CREATE TABLE application_stage_evaluators (
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    stage_id       UUID NOT NULL REFERENCES recruitment_stages(id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at    TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    PRIMARY KEY (application_id, stage_id, user_id)
);
CREATE INDEX idx_application_stage_evaluators_user ON application_stage_evaluators (user_id);

-- Phiếu đánh giá: mỗi người một phiếu cho mỗi hồ sơ ở mỗi vòng.
CREATE TABLE application_evaluations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    stage_id       UUID NOT NULL REFERENCES recruitment_stages(id) ON DELETE CASCADE,
    evaluator_id   UUID NOT NULL REFERENCES users(id),
    status         VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED')),
    total_score    NUMERIC(6, 2),
    recommendation VARCHAR(20) CHECK (recommendation IN ('PASS', 'FAIL', 'CONSIDER')),
    comment        TEXT,
    critical_failed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at     TIMESTAMP,
    submitted_at   TIMESTAMP,
    CONSTRAINT uq_application_evaluations UNIQUE (application_id, stage_id, evaluator_id)
);
CREATE INDEX idx_application_evaluations_evaluator ON application_evaluations (evaluator_id, status);

CREATE TABLE application_evaluation_scores (
    evaluation_id UUID NOT NULL REFERENCES application_evaluations(id) ON DELETE CASCADE,
    criterion_id  UUID NOT NULL REFERENCES recruitment_stage_criteria(id) ON DELETE CASCADE,
    score         NUMERIC(6, 2) CHECK (score >= 0),
    passed        BOOLEAN,
    comment       VARCHAR(1000),
    PRIMARY KEY (evaluation_id, criterion_id)
);

-- Lịch sử hồ sơ: đổi trạng thái tổng thể, trạng thái vòng, ghi chú; ai làm, lúc nào, lý do.
CREATE TABLE application_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    kind           VARCHAR(20) NOT NULL CHECK (kind IN ('STATUS', 'STAGE', 'EVALUATION', 'INTERVIEW', 'OFFER', 'DOCUMENT', 'NOTE')),
    stage_id       UUID REFERENCES recruitment_stages(id) ON DELETE SET NULL,
    from_status    VARCHAR(30),
    to_status      VARCHAR(30),
    message        VARCHAR(1000),
    reason         VARCHAR(1000),
    actor_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_application_history_application ON application_history (application_id, created_at DESC);

CREATE TABLE recruitment_interviews (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    stage_id       UUID REFERENCES recruitment_stages(id) ON DELETE SET NULL,
    title          VARCHAR(200) NOT NULL,
    starts_at      TIMESTAMP NOT NULL,
    ends_at        TIMESTAMP NOT NULL,
    mode           VARCHAR(20) NOT NULL CHECK (mode IN ('ONSITE', 'ONLINE', 'PHONE')),
    location       VARCHAR(300),
    meeting_url    VARCHAR(500),
    status         VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                   CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    note           TEXT,
    result_note    TEXT,
    created_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at     TIMESTAMP,
    CONSTRAINT chk_recruitment_interviews_time CHECK (ends_at > starts_at)
);
CREATE INDEX idx_recruitment_interviews_time ON recruitment_interviews (starts_at);
CREATE INDEX idx_recruitment_interviews_application ON recruitment_interviews (application_id);

CREATE TABLE recruitment_interview_participants (
    interview_id UUID NOT NULL REFERENCES recruitment_interviews(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (interview_id, user_id)
);
CREATE INDEX idx_recruitment_interview_participants_user ON recruitment_interview_participants (user_id);

-- Offer: tạo → (duyệt) → gửi → ứng viên chấp nhận / từ chối. Một hồ sơ chỉ có một offer còn hiệu lực.
CREATE TABLE recruitment_offers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id    UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    job_title         VARCHAR(200) NOT NULL,
    salary            NUMERIC(14, 2) NOT NULL CHECK (salary >= 0),
    start_date        DATE NOT NULL,
    response_deadline DATE NOT NULL,
    note              TEXT,
    status            VARCHAR(20) NOT NULL CHECK (status IN (
                          'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CANCELLED')),
    approved_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at       TIMESTAMP,
    sent_at           TIMESTAMP,
    responded_at      TIMESTAMP,
    decline_reason    VARCHAR(1000),
    created_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at        TIMESTAMP
);
CREATE UNIQUE INDEX uq_recruitment_offers_active ON recruitment_offers (application_id)
    WHERE status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT', 'ACCEPTED');
CREATE INDEX idx_recruitment_offers_status ON recruitment_offers (status, created_at DESC);

-- ------------------------------------------------------------------ phân quyền

INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:RECRUITMENT'),1,8) || '-' || substr(md5('permission_group:RECRUITMENT'),9,4) || '-' ||
        substr(md5('permission_group:RECRUITMENT'),13,4) || '-' || substr(md5('permission_group:RECRUITMENT'),17,4) || '-' ||
        substr(md5('permission_group:RECRUITMENT'),21,12))::uuid,
       'Tuyển dụng', 'RECRUITMENT', 'Chiến dịch, hồ sơ ứng viên, quy trình, đánh giá, phỏng vấn và offer', current_timestamp, m.id
FROM modules m WHERE m.code = 'HUMAN_RESOURCE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('RECRUITMENT_VIEW', 'Xem tuyển dụng', 'Vào module tuyển dụng; chỉ thấy chiến dịch / hồ sơ được phân công'),
    ('RECRUITMENT_MANAGE', 'Quản lý tuyển dụng', 'Quản lý chiến dịch, vị trí, hồ sơ, quy trình, lịch phỏng vấn và offer được phân công'),
    ('RECRUITMENT_EVALUATE', 'Đánh giá ứng viên', 'Chấm phiếu đánh giá cho hồ sơ / vòng được phân công'),
    ('RECRUITMENT_APPROVE', 'Phê duyệt tuyển dụng', 'Duyệt offer và quyết định kết quả vòng của vị trí mình phụ trách'),
    ('RECRUITMENT_ADMIN', 'Quản trị tuyển dụng', 'Toàn quyền mọi chiến dịch, hồ sơ và cấu hình tuyển dụng')
) AS p(code, name, description)
WHERE g.code = 'RECRUITMENT'
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(code, name, description) VALUES
    ('HR_ADMIN', 'HR Admin', 'Toàn quyền quản lý và cấu hình tuyển dụng'),
    ('HR_RECRUITER', 'HR Recruiter', 'Quản lý chiến dịch, hồ sơ và quy trình được phân công'),
    ('INTERVIEWER', 'Người phỏng vấn', 'Xem hồ sơ được phân công và thực hiện đánh giá'),
    ('HIRING_MANAGER', 'Hiring Manager', 'Theo dõi, đánh giá và phê duyệt ứng viên theo quyền')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE (r.code IN ('SUPER_ADMIN', 'ADMIN', 'HR_ADMIN') AND p.code LIKE 'RECRUITMENT\_%')
   OR (r.code = 'HR_RECRUITER' AND p.code IN ('RECRUITMENT_VIEW', 'RECRUITMENT_MANAGE', 'RECRUITMENT_EVALUATE'))
   OR (r.code = 'INTERVIEWER' AND p.code IN ('RECRUITMENT_VIEW', 'RECRUITMENT_EVALUATE'))
   OR (r.code = 'HIRING_MANAGER' AND p.code IN ('RECRUITMENT_VIEW', 'RECRUITMENT_EVALUATE', 'RECRUITMENT_APPROVE'))
   -- Quản lý cửa hàng tự tuyển cho cửa hàng mình: quyền quản trị tuyển dụng nhưng dữ liệu bị giới hạn theo cửa hàng.
   OR (r.code = 'MANAGER' AND p.code LIKE 'RECRUITMENT\_%')
   -- HR Admin tạo tài khoản nhân viên cho ứng viên trúng tuyển (module Nhân sự).
   OR (r.code = 'HR_ADMIN' AND p.code IN ('USER_VIEW', 'USER_CREATE', 'DEPARTMENT_VIEW', 'POSITION_VIEW'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Mẫu quy trình mặc định (theo yêu cầu nghiệp vụ).
INSERT INTO recruitment_pipeline_templates(name, description, stages) VALUES
('Lập trình viên Frontend', 'Sàng lọc CV → Kiểm tra chuyên môn → Phỏng vấn kỹ thuật → Phỏng vấn quản lý → Offer', '[
  {"name":"Sàng lọc CV","stageType":"SCREENING","scoringMode":"SCORE","scaleMax":10,"passScore":6,"passRule":"AVERAGE_SCORE","minEvaluations":1,"autoAdvance":true,
   "criteria":[{"name":"Kinh nghiệm","maxScore":10,"weight":2,"importance":"HIGH"},{"name":"Kỹ năng","maxScore":10,"weight":2,"importance":"HIGH"},{"name":"Dự án thực tế","maxScore":10,"weight":1,"importance":"MEDIUM"}]},
  {"name":"Kiểm tra chuyên môn","stageType":"TEST","scoringMode":"SCORE","scaleMax":100,"passScore":60,"passRule":"AVERAGE_SCORE","minEvaluations":1,"autoAdvance":false,
   "criteria":[{"name":"Kiến thức nền tảng","maxScore":10,"weight":1,"importance":"HIGH"},{"name":"Kỹ năng lập trình","maxScore":10,"weight":2,"importance":"CRITICAL"},{"name":"Tư duy logic","maxScore":10,"weight":1,"importance":"MEDIUM"}]},
  {"name":"Phỏng vấn kỹ thuật","stageType":"INTERVIEW","scoringMode":"SCORE","scaleMax":10,"passScore":7,"passRule":"AVERAGE_SCORE","minEvaluations":2,"autoAdvance":false,
   "criteria":[{"name":"Giải quyết vấn đề","maxScore":10,"weight":2,"importance":"HIGH"},{"name":"Giao tiếp","maxScore":10,"weight":1,"importance":"MEDIUM"},{"name":"Kinh nghiệm thực tế","maxScore":10,"weight":1,"importance":"MEDIUM"}]},
  {"name":"Phỏng vấn quản lý","stageType":"INTERVIEW","scoringMode":"PASS_FAIL","scaleMax":10,"passScore":null,"passRule":"ALL_PASS","minEvaluations":1,"autoAdvance":false,
   "criteria":[{"name":"Phù hợp văn hóa","maxScore":10,"weight":1,"importance":"HIGH"},{"name":"Định hướng phát triển","maxScore":10,"weight":1,"importance":"MEDIUM"}]},
  {"name":"Offer","stageType":"OFFER","scoringMode":"SCORE","scaleMax":10,"passScore":null,"passRule":"MANUAL","minEvaluations":1,"autoAdvance":false,"criteria":[]}
]'::jsonb),
('Nhân viên kinh doanh', 'Sàng lọc CV → Phỏng vấn HR → Phỏng vấn quản lý → Offer', '[
  {"name":"Sàng lọc CV","stageType":"SCREENING","scoringMode":"PASS_FAIL","scaleMax":10,"passScore":null,"passRule":"ALL_PASS","minEvaluations":1,"autoAdvance":true,
   "criteria":[{"name":"Kinh nghiệm bán hàng","maxScore":10,"weight":1,"importance":"HIGH"},{"name":"Ngoại hình, tác phong","maxScore":10,"weight":1,"importance":"MEDIUM"}]},
  {"name":"Phỏng vấn HR","stageType":"INTERVIEW","scoringMode":"SCORE","scaleMax":10,"passScore":6,"passRule":"AVERAGE_SCORE","minEvaluations":1,"autoAdvance":false,
   "criteria":[{"name":"Giao tiếp","maxScore":10,"weight":2,"importance":"CRITICAL"},{"name":"Thái độ","maxScore":10,"weight":1,"importance":"HIGH"},{"name":"Mức độ gắn bó","maxScore":10,"weight":1,"importance":"MEDIUM"}]},
  {"name":"Phỏng vấn quản lý","stageType":"INTERVIEW","scoringMode":"SCORE","scaleMax":10,"passScore":7,"passRule":"AVERAGE_SCORE","minEvaluations":1,"autoAdvance":false,
   "criteria":[{"name":"Kỹ năng thuyết phục","maxScore":10,"weight":2,"importance":"HIGH"},{"name":"Hiểu sản phẩm thời trang","maxScore":10,"weight":1,"importance":"MEDIUM"}]},
  {"name":"Offer","stageType":"OFFER","scoringMode":"SCORE","scaleMax":10,"passScore":null,"passRule":"MANUAL","minEvaluations":1,"autoAdvance":false,"criteria":[]}
]'::jsonb);
