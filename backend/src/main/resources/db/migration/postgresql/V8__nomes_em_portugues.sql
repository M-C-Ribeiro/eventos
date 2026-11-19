-- Tradução dos nomes do banco para português: tabelas e colunas.
-- Os dados são mantidos; só os nomes mudam. Chaves estrangeiras, índices e restrições únicas
-- acompanham a renomeação automaticamente (os nomes das restrições continuam os originais).
-- Valores gravados (ex.: PUBLISHED, EVENT_EDIT) não mudam: correspondem às enumerações do código.

-- Esta restrição usa colunas que serão renomeadas (o MySQL não permite renomeá-las com ela ativa).
ALTER TABLE demand_assignments DROP CONSTRAINT ck_assignment_target;

-- tenants -> organizacoes
ALTER TABLE tenants RENAME COLUMN name TO nome;
ALTER TABLE tenants RENAME COLUMN slug TO identificador;
ALTER TABLE tenants RENAME COLUMN created_at TO criado_em;

-- users -> usuarios
ALTER TABLE users RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE users RENAME COLUMN name TO nome;
ALTER TABLE users RENAME COLUMN password_hash TO senha_hash;
ALTER TABLE users RENAME COLUMN active TO ativo;
ALTER TABLE users RENAME COLUMN created_at TO criado_em;

-- access_roles -> perfis_acesso
ALTER TABLE access_roles RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE access_roles RENAME COLUMN name TO nome;
ALTER TABLE access_roles RENAME COLUMN description TO descricao;
ALTER TABLE access_roles RENAME COLUMN system_role TO perfil_sistema;
ALTER TABLE access_roles RENAME COLUMN created_at TO criado_em;

-- access_role_permissions -> perfil_acesso_permissoes
ALTER TABLE access_role_permissions RENAME COLUMN role_id TO perfil_id;
ALTER TABLE access_role_permissions RENAME COLUMN permission TO permissao;

-- user_roles -> usuario_perfis
ALTER TABLE user_roles RENAME COLUMN user_id TO usuario_id;
ALTER TABLE user_roles RENAME COLUMN role_id TO perfil_id;

-- team_functions -> funcoes_equipe
ALTER TABLE team_functions RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE team_functions RENAME COLUMN name TO nome;
ALTER TABLE team_functions RENAME COLUMN description TO descricao;
ALTER TABLE team_functions RENAME COLUMN created_at TO criado_em;

-- team_function_permissions -> funcao_equipe_permissoes
ALTER TABLE team_function_permissions RENAME COLUMN function_id TO funcao_id;
ALTER TABLE team_function_permissions RENAME COLUMN permission TO permissao;

-- events -> eventos
ALTER TABLE events RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE events RENAME COLUMN title TO titulo;
ALTER TABLE events RENAME COLUMN description TO descricao;
ALTER TABLE events RENAME COLUMN location TO localizacao;
ALTER TABLE events RENAME COLUMN start_at TO inicio;
ALTER TABLE events RENAME COLUMN end_at TO termino;
ALTER TABLE events RENAME COLUMN capacity TO vagas;
ALTER TABLE events RENAME COLUMN status TO situacao;
ALTER TABLE events RENAME COLUMN organizer_id TO responsavel_id;
ALTER TABLE events RENAME COLUMN created_at TO criado_em;

-- event_team_members -> membros_equipe_evento
ALTER TABLE event_team_members RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE event_team_members RENAME COLUMN event_id TO evento_id;
ALTER TABLE event_team_members RENAME COLUMN user_id TO usuario_id;
ALTER TABLE event_team_members RENAME COLUMN function_id TO funcao_id;
ALTER TABLE event_team_members RENAME COLUMN created_at TO criado_em;

-- registrations -> inscricoes
ALTER TABLE registrations RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE registrations RENAME COLUMN event_id TO evento_id;
ALTER TABLE registrations RENAME COLUMN user_id TO usuario_id;
ALTER TABLE registrations RENAME COLUMN name TO nome;
ALTER TABLE registrations RENAME COLUMN status TO situacao;
ALTER TABLE registrations RENAME COLUMN registered_at TO inscrito_em;
ALTER TABLE registrations RENAME COLUMN checked_in_at TO checkin_em;

-- event_schedule_items -> itens_cronograma
ALTER TABLE event_schedule_items RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE event_schedule_items RENAME COLUMN event_id TO evento_id;
ALTER TABLE event_schedule_items RENAME COLUMN title TO titulo;
ALTER TABLE event_schedule_items RENAME COLUMN item_date TO data;
ALTER TABLE event_schedule_items RENAME COLUMN start_time TO hora;
ALTER TABLE event_schedule_items RENAME COLUMN duration_minutes TO duracao_minutos;
ALTER TABLE event_schedule_items RENAME COLUMN speaker TO palestrante;
ALTER TABLE event_schedule_items RENAME COLUMN contact TO contato;
ALTER TABLE event_schedule_items RENAME COLUMN created_at TO criado_em;

-- prep_meetings -> reunioes_preparatorias
ALTER TABLE prep_meetings RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE prep_meetings RENAME COLUMN event_id TO evento_id;
ALTER TABLE prep_meetings RENAME COLUMN title TO titulo;
ALTER TABLE prep_meetings RENAME COLUMN meeting_date TO data;
ALTER TABLE prep_meetings RENAME COLUMN start_time TO hora;
ALTER TABLE prep_meetings RENAME COLUMN location TO localizacao;
ALTER TABLE prep_meetings RENAME COLUMN created_at TO criado_em;

-- meeting_attendance -> presencas_reuniao
ALTER TABLE meeting_attendance RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE meeting_attendance RENAME COLUMN meeting_id TO reuniao_id;
ALTER TABLE meeting_attendance RENAME COLUMN user_id TO usuario_id;
ALTER TABLE meeting_attendance RENAME COLUMN present TO presente;
ALTER TABLE meeting_attendance RENAME COLUMN justification TO justificativa;
ALTER TABLE meeting_attendance RENAME COLUMN updated_at TO atualizado_em;

-- demands -> demandas
ALTER TABLE demands RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE demands RENAME COLUMN event_id TO evento_id;
ALTER TABLE demands RENAME COLUMN title TO titulo;
ALTER TABLE demands RENAME COLUMN description TO descricao;
ALTER TABLE demands RENAME COLUMN due_date TO data_entrega;
ALTER TABLE demands RENAME COLUMN created_at TO criado_em;

-- demand_assignments -> atribuicoes_demanda
ALTER TABLE demand_assignments RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE demand_assignments RENAME COLUMN demand_id TO demanda_id;
ALTER TABLE demand_assignments RENAME COLUMN function_id TO funcao_id;
ALTER TABLE demand_assignments RENAME COLUMN user_id TO usuario_id;

-- demand_completions -> conclusoes_demanda
ALTER TABLE demand_completions RENAME COLUMN tenant_id TO organizacao_id;
ALTER TABLE demand_completions RENAME COLUMN demand_id TO demanda_id;
ALTER TABLE demand_completions RENAME COLUMN user_id TO usuario_id;
ALTER TABLE demand_completions RENAME COLUMN completed_at TO concluido_em;
ALTER TABLE demand_completions RENAME COLUMN marked_by TO registrado_por;

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
