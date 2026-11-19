-- Conclusão de cada demanda por responsável. Sem registro = pendente.

CREATE TABLE demand_completions (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    demand_id     BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    completed_at  DATETIME(6) NOT NULL,
    marked_by     BIGINT,
    CONSTRAINT uk_completion_demand_user UNIQUE (demand_id, user_id),
    CONSTRAINT fk_completion_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_completion_demand FOREIGN KEY (demand_id) REFERENCES demands (id) ON DELETE CASCADE,
    CONSTRAINT fk_completion_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_completion_marked_by FOREIGN KEY (marked_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
