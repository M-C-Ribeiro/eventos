-- Título de cada item do cronograma. Itens já cadastrados recebem "Sem título" e podem ser editados.
ALTER TABLE event_schedule_items ADD COLUMN title VARCHAR(160) NOT NULL DEFAULT 'Sem título';
