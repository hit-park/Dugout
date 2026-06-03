CREATE TABLE attendance_reminder_logs (
    id              BIGSERIAL PRIMARY KEY,
    match_id        BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    reminder_window VARCHAR(10) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_reminder UNIQUE (match_id, user_id, reminder_window)
);
