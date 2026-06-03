CREATE TABLE notification_preferences (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE,
    match_created       BOOLEAN NOT NULL DEFAULT true,
    lineup_confirmed    BOOLEAN NOT NULL DEFAULT true,
    attendance_reminder BOOLEAN NOT NULL DEFAULT true,
    attendance_changed  BOOLEAN NOT NULL DEFAULT true,
    dnd_enabled         BOOLEAN NOT NULL DEFAULT true,
    dnd_start           TIME NOT NULL DEFAULT '22:00',
    dnd_end             TIME NOT NULL DEFAULT '08:00',
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);
