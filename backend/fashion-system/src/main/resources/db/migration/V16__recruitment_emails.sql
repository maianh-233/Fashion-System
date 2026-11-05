-- Email cho ứng viên: mẫu email chỉnh được (placeholder dạng {{candidateName}}), nhật ký mọi email đã gửi / lỗi,
-- và liên kết ứng viên ↔ nhân viên sau khi tuyển.

CREATE TABLE recruitment_email_templates (
    code        VARCHAR(40) PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    subject     VARCHAR(300) NOT NULL,
    body        TEXT NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMP
);

CREATE TABLE recruitment_emails (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID REFERENCES job_applications(id) ON DELETE CASCADE,
    candidate_id   UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    template_code  VARCHAR(40),
    to_email       VARCHAR(255) NOT NULL,
    subject        VARCHAR(300) NOT NULL,
    body           TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'SENT', 'FAILED')),
    error          VARCHAR(500),
    sent_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    sent_at        TIMESTAMP
);
CREATE INDEX idx_recruitment_emails_application ON recruitment_emails (application_id, created_at DESC);
CREATE INDEX idx_recruitment_emails_candidate ON recruitment_emails (candidate_id, created_at DESC);

ALTER TABLE candidates ADD COLUMN employee_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX uq_candidates_employee ON candidates (employee_id) WHERE employee_id IS NOT NULL;

INSERT INTO recruitment_email_templates (code, name, subject, body) VALUES
('APPLICATION_RECEIVED', 'Xác nhận đã nhận hồ sơ',
 'Lunaria đã nhận hồ sơ ứng tuyển {{positionTitle}}',
 'Chào {{candidateName}},

Cảm ơn bạn đã quan tâm và ứng tuyển vị trí {{positionTitle}} tại {{workplace}}.
Mã hồ sơ của bạn: {{applicationCode}}.

Bộ phận tuyển dụng sẽ xem xét hồ sơ và liên hệ với bạn nếu hồ sơ phù hợp.

Trân trọng,
Bộ phận Tuyển dụng Lunaria'),
('INTERVIEW_INVITATION', 'Thư mời phỏng vấn',
 'Thư mời phỏng vấn – {{positionTitle}}',
 'Chào {{candidateName}},

Lunaria trân trọng mời bạn tham gia {{interviewTitle}} cho vị trí {{positionTitle}}.

Thời gian: {{interviewTime}}
Hình thức: {{interviewMode}}
{{interviewPlace}}

Vui lòng phản hồi email này để xác nhận tham gia. Nếu cần đổi lịch, hãy báo cho chúng tôi sớm nhất có thể.

Trân trọng,
Bộ phận Tuyển dụng Lunaria'),
('INTERVIEW_RESCHEDULED', 'Thông báo đổi lịch phỏng vấn',
 'Đổi lịch phỏng vấn – {{positionTitle}}',
 'Chào {{candidateName}},

Lịch {{interviewTitle}} cho vị trí {{positionTitle}} đã được đổi sang:

Thời gian: {{interviewTime}}
Hình thức: {{interviewMode}}
{{interviewPlace}}

Xin lỗi vì sự bất tiện và mong được gặp bạn.

Trân trọng,
Bộ phận Tuyển dụng Lunaria'),
('INTERVIEW_CANCELLED', 'Thông báo hủy lịch phỏng vấn',
 'Hủy lịch phỏng vấn – {{positionTitle}}',
 'Chào {{candidateName}},

Lịch {{interviewTitle}} lúc {{interviewTime}} cho vị trí {{positionTitle}} đã được hủy.
{{reason}}

Bộ phận tuyển dụng sẽ liên hệ lại với bạn nếu có lịch mới.

Trân trọng,
Bộ phận Tuyển dụng Lunaria'),
('OFFER_LETTER', 'Thư mời nhận việc (offer)',
 'Thư mời nhận việc – {{jobTitle}} tại Lunaria',
 'Chào {{candidateName}},

Chúc mừng bạn! Lunaria trân trọng mời bạn nhận vị trí {{jobTitle}} tại {{workplace}} với các điều kiện:

Mức lương: {{salary}}/tháng
Ngày bắt đầu làm việc: {{startDate}}
{{offerNote}}

Vui lòng phản hồi trước ngày {{responseDeadline}} để xác nhận nhận việc.

Trân trọng,
Bộ phận Tuyển dụng Lunaria'),
('REJECTION', 'Thông báo kết quả (không phù hợp)',
 'Kết quả ứng tuyển {{positionTitle}}',
 'Chào {{candidateName}},

Cảm ơn bạn đã dành thời gian ứng tuyển vị trí {{positionTitle}} tại Lunaria.
Sau khi cân nhắc, chúng tôi rất tiếc chưa thể tiếp tục với hồ sơ của bạn ở thời điểm này.

Chúng tôi sẽ lưu hồ sơ và liên hệ khi có vị trí phù hợp hơn. Chúc bạn nhiều thành công.

Trân trọng,
Bộ phận Tuyển dụng Lunaria'),
('ONBOARDING', 'Chào mừng nhân viên mới',
 'Chào mừng bạn gia nhập Lunaria',
 'Chào {{candidateName}},

Chào mừng bạn gia nhập Lunaria với vị trí {{jobTitle}} tại {{workplace}}!

Ngày bắt đầu làm việc: {{startDate}}
Mã nhân viên: {{employeeCode}}
Tên đăng nhập hệ thống: {{username}}

Mật khẩu đăng nhập ban đầu sẽ được bộ phận nhân sự gửi trực tiếp cho bạn vào ngày đầu tiên. Vui lòng đổi mật khẩu sau khi đăng nhập.

Hẹn gặp bạn!
Bộ phận Nhân sự Lunaria');

-- Lịch sử hồ sơ ghi cả email đã gửi cho ứng viên.
ALTER TABLE application_history DROP CONSTRAINT application_history_kind_check;
ALTER TABLE application_history ADD CONSTRAINT application_history_kind_check
    CHECK (kind IN ('STATUS', 'STAGE', 'EVALUATION', 'INTERVIEW', 'OFFER', 'DOCUMENT', 'NOTE', 'EMAIL'));
