-- RBAC (perfis de acesso com permissões) e equipes de evento com funções.

CREATE TABLE access_roles (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    name         VARCHAR(60)  NOT NULL,
    description  VARCHAR(255),
    system_role  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6) NOT NULL,
    CONSTRAINT uk_access_roles_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT fk_access_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE access_role_permissions (
    role_id     BIGINT      NOT NULL,
    permission  VARCHAR(40) NOT NULL,
    PRIMARY KEY (role_id, permission),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES access_roles (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES access_roles (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE team_functions (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    name         VARCHAR(60)  NOT NULL,
    description  VARCHAR(255),
    created_at   DATETIME(6) NOT NULL,
    CONSTRAINT uk_team_functions_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT fk_team_functions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE team_function_permissions (
    function_id  BIGINT      NOT NULL,
    permission   VARCHAR(40) NOT NULL,
    PRIMARY KEY (function_id, permission),
    CONSTRAINT fk_function_permissions_function FOREIGN KEY (function_id) REFERENCES team_functions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE event_team_members (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    event_id     BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    function_id  BIGINT NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    CONSTRAINT uk_event_team_event_user UNIQUE (event_id, user_id),
    CONSTRAINT fk_team_members_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_function FOREIGN KEY (function_id) REFERENCES team_functions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_event_team_user ON event_team_members (user_id);

-- Dados: perfis e funções padrão para as organizações existentes, e migração do antigo campo users.role.
INSERT INTO access_roles (tenant_id, name, description, system_role, created_at)
SELECT id, 'Administrador', 'Acesso total à organização.', TRUE, CURRENT_TIMESTAMP FROM tenants;
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_VIEW' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_VIEW_ALL' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_CREATE' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_EDIT' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_DELETE' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'REGISTRATION_VIEW' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'REGISTRATION_MANAGE' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'CHECKIN' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'TEAM_MANAGE' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'TEAM_FUNCTION_MANAGE' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'USER_MANAGE' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'ROLE_MANAGE' FROM access_roles WHERE name = 'Administrador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'ORGANIZATION_MANAGE' FROM access_roles WHERE name = 'Administrador';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN access_roles r ON r.tenant_id = u.tenant_id AND r.name = 'Administrador' WHERE u.role = 'ADMIN';

INSERT INTO access_roles (tenant_id, name, description, system_role, created_at)
SELECT id, 'Organizador', 'Cria e gerencia eventos, inscrições, check-in e equipes.', FALSE, CURRENT_TIMESTAMP FROM tenants;
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_VIEW' FROM access_roles WHERE name = 'Organizador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_VIEW_ALL' FROM access_roles WHERE name = 'Organizador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_CREATE' FROM access_roles WHERE name = 'Organizador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_EDIT' FROM access_roles WHERE name = 'Organizador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_DELETE' FROM access_roles WHERE name = 'Organizador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'REGISTRATION_VIEW' FROM access_roles WHERE name = 'Organizador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'REGISTRATION_MANAGE' FROM access_roles WHERE name = 'Organizador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'CHECKIN' FROM access_roles WHERE name = 'Organizador';
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'TEAM_MANAGE' FROM access_roles WHERE name = 'Organizador';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN access_roles r ON r.tenant_id = u.tenant_id AND r.name = 'Organizador' WHERE u.role = 'ORGANIZER';

INSERT INTO access_roles (tenant_id, name, description, system_role, created_at)
SELECT id, 'Membro', 'Vê eventos publicados e se inscreve.', FALSE, CURRENT_TIMESTAMP FROM tenants;
INSERT INTO access_role_permissions (role_id, permission) SELECT id, 'EVENT_VIEW' FROM access_roles WHERE name = 'Membro';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN access_roles r ON r.tenant_id = u.tenant_id AND r.name = 'Membro' WHERE u.role = 'MEMBER';

INSERT INTO team_functions (tenant_id, name, description, created_at)
SELECT id, 'Coordenação', 'Coordena o evento: edita dados, inscrições, check-in e equipe.', CURRENT_TIMESTAMP FROM tenants;
INSERT INTO team_function_permissions (function_id, permission) SELECT id, 'EVENT_EDIT' FROM team_functions WHERE name = 'Coordenação';
INSERT INTO team_function_permissions (function_id, permission) SELECT id, 'REGISTRATION_VIEW' FROM team_functions WHERE name = 'Coordenação';
INSERT INTO team_function_permissions (function_id, permission) SELECT id, 'REGISTRATION_MANAGE' FROM team_functions WHERE name = 'Coordenação';
INSERT INTO team_function_permissions (function_id, permission) SELECT id, 'CHECKIN' FROM team_functions WHERE name = 'Coordenação';
INSERT INTO team_function_permissions (function_id, permission) SELECT id, 'TEAM_MANAGE' FROM team_functions WHERE name = 'Coordenação';

INSERT INTO team_functions (tenant_id, name, description, created_at)
SELECT id, 'Recepção', 'Recebe os participantes e faz o check-in.', CURRENT_TIMESTAMP FROM tenants;
INSERT INTO team_function_permissions (function_id, permission) SELECT id, 'REGISTRATION_VIEW' FROM team_functions WHERE name = 'Recepção';
INSERT INTO team_function_permissions (function_id, permission) SELECT id, 'CHECKIN' FROM team_functions WHERE name = 'Recepção';

INSERT INTO team_functions (tenant_id, name, description, created_at)
SELECT id, 'Apoio', 'Ajuda na execução; vê o evento mesmo antes de publicado.', CURRENT_TIMESTAMP FROM tenants;

-- O perfil fixo foi substituído pelos perfis de acesso.
ALTER TABLE users DROP COLUMN role;
