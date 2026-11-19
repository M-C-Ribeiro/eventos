-- Reuniões preparatórias do evento e lista de presença da equipe.

CREATE TABLE prep_meetings (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    event_id      BIGINT       NOT NULL,
    title         VARCHAR(160) NOT NULL,
    meeting_date  DATE         NOT NULL,
    start_time    TIME         NOT NULL,
    location      VARCHAR(255) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    CONSTRAINT fk_meetings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_meetings_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_meetings_event_date ON prep_meetings (event_id, meeting_date, start_time);

-- Quem não tem registro é considerado ausente.
CREATE TABLE meeting_attendance (
    id             BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    meeting_id     BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    present        BOOLEAN      NOT NULL,
    justification  VARCHAR(500),
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT uk_attendance_meeting_user UNIQUE (meeting_id, user_id),
    CONSTRAINT fk_attendance_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_meeting FOREIGN KEY (meeting_id) REFERENCES prep_meetings (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
