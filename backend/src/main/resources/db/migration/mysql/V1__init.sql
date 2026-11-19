-- Versão MySQL 8+ do esquema (mesmas tabelas das versões H2/PostgreSQL).
-- Multi-tenancy por coluna discriminadora: toda tabela de negócio tem tenant_id.
-- Datas são gravadas em UTC (hibernate.jdbc.time_zone=UTC), por isso DATETIME(6).

CREATE TABLE tenants (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(60)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_tenants_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    name           VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE events (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT        NOT NULL,
    title         VARCHAR(160)  NOT NULL,
    description   VARCHAR(4000),
    location      VARCHAR(255),
    start_at      DATETIME(6)   NOT NULL,
    end_at        DATETIME(6)   NOT NULL,
    capacity      INT,
    status        VARCHAR(20)   NOT NULL,
    organizer_id  BIGINT        NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    CONSTRAINT fk_events_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users (id),
    INDEX idx_events_tenant_start (tenant_id, start_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE registrations (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    event_id       BIGINT       NOT NULL,
    user_id        BIGINT,
    name           VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    registered_at  DATETIME(6)  NOT NULL,
    checked_in_at  DATETIME(6),
    CONSTRAINT uk_registrations_event_email UNIQUE (event_id, email),
    CONSTRAINT fk_registrations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_registrations_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_registrations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_registrations_tenant_event (tenant_id, event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
