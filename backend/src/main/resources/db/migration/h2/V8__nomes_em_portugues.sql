-- Tradução dos nomes do banco para português: tabelas e colunas.
-- Os dados são mantidos; só os nomes mudam. Chaves estrangeiras, índices e restrições únicas
-- acompanham a renomeação automaticamente (os nomes das restrições continuam os originais).
-- Valores gravados (ex.: PUBLISHED, EVENT_EDIT) não mudam: correspondem às enumerações do código.

-- Esta restrição usa colunas que serão renomeadas (o MySQL não permite renomeá-las com ela ativa).
ALTER TABLE demand_assignments DROP CONSTRAINT ck_assignment_target;

-- tenants -> organizacoes
ALTER TABLE tenants ALTER COLUMN name RENAME TO nome;
ALTER TABLE tenants ALTER COLUMN slug RENAME TO identificador;
ALTER TABLE tenants ALTER COLUMN created_at RENAME TO criado_em;

-- users -> usuarios
ALTER TABLE users ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE users ALTER COLUMN name RENAME TO nome;
ALTER TABLE users ALTER COLUMN password_hash RENAME TO senha_hash;
ALTER TABLE users ALTER COLUMN active RENAME TO ativo;
ALTER TABLE users ALTER COLUMN created_at RENAME TO criado_em;

-- access_roles -> perfis_acesso
ALTER TABLE access_roles ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE access_roles ALTER COLUMN name RENAME TO nome;
ALTER TABLE access_roles ALTER COLUMN description RENAME TO descricao;
ALTER TABLE access_roles ALTER COLUMN system_role RENAME TO perfil_sistema;
ALTER TABLE access_roles ALTER COLUMN created_at RENAME TO criado_em;

-- access_role_permissions -> perfil_acesso_permissoes
ALTER TABLE access_role_permissions ALTER COLUMN role_id RENAME TO perfil_id;
ALTER TABLE access_role_permissions ALTER COLUMN permission RENAME TO permissao;

-- user_roles -> usuario_perfis
ALTER TABLE user_roles ALTER COLUMN user_id RENAME TO usuario_id;
ALTER TABLE user_roles ALTER COLUMN role_id RENAME TO perfil_id;

-- team_functions -> funcoes_equipe
ALTER TABLE team_functions ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE team_functions ALTER COLUMN name RENAME TO nome;
ALTER TABLE team_functions ALTER COLUMN description RENAME TO descricao;
ALTER TABLE team_functions ALTER COLUMN created_at RENAME TO criado_em;

-- team_function_permissions -> funcao_equipe_permissoes
ALTER TABLE team_function_permissions ALTER COLUMN function_id RENAME TO funcao_id;
ALTER TABLE team_function_permissions ALTER COLUMN permission RENAME TO permissao;

-- events -> eventos
ALTER TABLE events ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE events ALTER COLUMN title RENAME TO titulo;
ALTER TABLE events ALTER COLUMN description RENAME TO descricao;
ALTER TABLE events ALTER COLUMN location RENAME TO localizacao;
ALTER TABLE events ALTER COLUMN start_at RENAME TO inicio;
ALTER TABLE events ALTER COLUMN end_at RENAME TO termino;
ALTER TABLE events ALTER COLUMN capacity RENAME TO vagas;
ALTER TABLE events ALTER COLUMN status RENAME TO situacao;
ALTER TABLE events ALTER COLUMN organizer_id RENAME TO responsavel_id;
ALTER TABLE events ALTER COLUMN created_at RENAME TO criado_em;

-- event_team_members -> membros_equipe_evento
ALTER TABLE event_team_members ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE event_team_members ALTER COLUMN event_id RENAME TO evento_id;
ALTER TABLE event_team_members ALTER COLUMN user_id RENAME TO usuario_id;
ALTER TABLE event_team_members ALTER COLUMN function_id RENAME TO funcao_id;
ALTER TABLE event_team_members ALTER COLUMN created_at RENAME TO criado_em;

-- registrations -> inscricoes
ALTER TABLE registrations ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE registrations ALTER COLUMN event_id RENAME TO evento_id;
ALTER TABLE registrations ALTER COLUMN user_id RENAME TO usuario_id;
ALTER TABLE registrations ALTER COLUMN name RENAME TO nome;
ALTER TABLE registrations ALTER COLUMN status RENAME TO situacao;
ALTER TABLE registrations ALTER COLUMN registered_at RENAME TO inscrito_em;
ALTER TABLE registrations ALTER COLUMN checked_in_at RENAME TO checkin_em;

-- event_schedule_items -> itens_cronograma
ALTER TABLE event_schedule_items ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE event_schedule_items ALTER COLUMN event_id RENAME TO evento_id;
ALTER TABLE event_schedule_items ALTER COLUMN title RENAME TO titulo;
ALTER TABLE event_schedule_items ALTER COLUMN item_date RENAME TO data;
ALTER TABLE event_schedule_items ALTER COLUMN start_time RENAME TO hora;
ALTER TABLE event_schedule_items ALTER COLUMN duration_minutes RENAME TO duracao_minutos;
ALTER TABLE event_schedule_items ALTER COLUMN speaker RENAME TO palestrante;
ALTER TABLE event_schedule_items ALTER COLUMN contact RENAME TO contato;
ALTER TABLE event_schedule_items ALTER COLUMN created_at RENAME TO criado_em;

-- prep_meetings -> reunioes_preparatorias
ALTER TABLE prep_meetings ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE prep_meetings ALTER COLUMN event_id RENAME TO evento_id;
ALTER TABLE prep_meetings ALTER COLUMN title RENAME TO titulo;
ALTER TABLE prep_meetings ALTER COLUMN meeting_date RENAME TO data;
ALTER TABLE prep_meetings ALTER COLUMN start_time RENAME TO hora;
ALTER TABLE prep_meetings ALTER COLUMN location RENAME TO localizacao;
ALTER TABLE prep_meetings ALTER COLUMN created_at RENAME TO criado_em;

-- meeting_attendance -> presencas_reuniao
ALTER TABLE meeting_attendance ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE meeting_attendance ALTER COLUMN meeting_id RENAME TO reuniao_id;
ALTER TABLE meeting_attendance ALTER COLUMN user_id RENAME TO usuario_id;
ALTER TABLE meeting_attendance ALTER COLUMN present RENAME TO presente;
ALTER TABLE meeting_attendance ALTER COLUMN justification RENAME TO justificativa;
ALTER TABLE meeting_attendance ALTER COLUMN updated_at RENAME TO atualizado_em;

-- demands -> demandas
ALTER TABLE demands ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE demands ALTER COLUMN event_id RENAME TO evento_id;
ALTER TABLE demands ALTER COLUMN title RENAME TO titulo;
ALTER TABLE demands ALTER COLUMN description RENAME TO descricao;
ALTER TABLE demands ALTER COLUMN due_date RENAME TO data_entrega;
ALTER TABLE demands ALTER COLUMN created_at RENAME TO criado_em;

-- demand_assignments -> atribuicoes_demanda
ALTER TABLE demand_assignments ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE demand_assignments ALTER COLUMN demand_id RENAME TO demanda_id;
ALTER TABLE demand_assignments ALTER COLUMN function_id RENAME TO funcao_id;
ALTER TABLE demand_assignments ALTER COLUMN user_id RENAME TO usuario_id;

-- demand_completions -> conclusoes_demanda
ALTER TABLE demand_completions ALTER COLUMN tenant_id RENAME TO organizacao_id;
ALTER TABLE demand_completions ALTER COLUMN demand_id RENAME TO demanda_id;
ALTER TABLE demand_completions ALTER COLUMN user_id RENAME TO usuario_id;
ALTER TABLE demand_completions ALTER COLUMN completed_at RENAME TO concluido_em;
ALTER TABLE demand_completions ALTER COLUMN marked_by RENAME TO registrado_por;

-- Tabelas
ALTER TABLE tenants RENAME TO organizacoes;
ALTER TABLE users RENAME TO usuarios;
ALTER TABLE access_roles RENAME TO perfis_acesso;
ALTER TABLE access_role_permissions RENAME TO perfil_acesso_permissoes;
ALTER TABLE user_roles RENAME TO usuario_perfis;
ALTER TABLE team_functions RENAME TO funcoes_equipe;
ALTER TABLE team_function_permissions RENAME TO funcao_equipe_permissoes;
ALTER TABLE events RENAME TO eventos;
ALTER TABLE event_team_members RENAME TO membros_equipe_evento;
ALTER TABLE registrations RENAME TO inscricoes;
ALTER TABLE event_schedule_items RENAME TO itens_cronograma;
ALTER TABLE prep_meetings RENAME TO reunioes_preparatorias;
ALTER TABLE meeting_attendance RENAME TO presencas_reuniao;
ALTER TABLE demands RENAME TO demandas;
ALTER TABLE demand_assignments RENAME TO atribuicoes_demanda;
ALTER TABLE demand_completions RENAME TO conclusoes_demanda;

-- Recria a restrição: exatamente um entre funcao_id e usuario_id.
ALTER TABLE atribuicoes_demanda ADD CONSTRAINT ck_atribuicao_alvo CHECK ((funcao_id IS NULL AND usuario_id IS NOT NULL)
                                                                     OR (funcao_id IS NOT NULL AND usuario_id IS NULL));
