-- Cronograma do evento: data, hora, duração, palestrante e contato de cada item.

CREATE TABLE event_schedule_items (
    id                BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL,
    event_id          BIGINT       NOT NULL,
    item_date         DATE         NOT NULL,
    start_time        TIME         NOT NULL,
    duration_minutes  INT          NOT NULL,
    speaker           VARCHAR(120) NOT NULL,
    contact           VARCHAR(160),
    created_at        DATETIME(6) NOT NULL,
    CONSTRAINT fk_schedule_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_schedule_event_date ON event_schedule_items (event_id, item_date, start_time);
