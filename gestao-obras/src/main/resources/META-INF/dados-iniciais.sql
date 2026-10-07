-- Dados de demonstração, carregados após a criação do esquema (apenas em desenvolvimento).
-- Uma instrução por linha: alguns providers JPA leem o script linha a linha.
INSERT INTO obra (nome, localizacao, status, versao, data_criacao) VALUES ('Edifício Aurora', 'Lisboa, Parque das Nações', 'EM_CURSO', 0, CURRENT_TIMESTAMP);
INSERT INTO obra (nome, localizacao, status, versao, data_criacao) VALUES ('Ponte Pedonal do Douro', 'Porto, Ribeira', 'PLANEADA', 0, CURRENT_TIMESTAMP);
INSERT INTO obra (nome, localizacao, status, versao, data_criacao) VALUES ('Escola Básica de Braga', 'Braga, São Vicente', 'SUSPENSA', 0, CURRENT_TIMESTAMP);
INSERT INTO obra (nome, localizacao, status, versao, data_criacao) VALUES ('Hospital de Faro - Ala Norte', 'Faro, Centro', 'CONCLUIDA', 0, CURRENT_TIMESTAMP);
INSERT INTO relatorio_seguranca (data_inspecao, descricao, obra_id) VALUES (DATE '2026-09-15', 'Andaimes do bloco A verificados. Guarda-corpos conformes.', (SELECT id FROM obra WHERE nome = 'Edifício Aurora'));
INSERT INTO relatorio_seguranca (data_inspecao, descricao, obra_id) VALUES (DATE '2026-09-30', 'Dois trabalhadores sem capacete na zona de cofragem. Ação corretiva aplicada.', (SELECT id FROM obra WHERE nome = 'Edifício Aurora'));
INSERT INTO relatorio_seguranca (data_inspecao, descricao, obra_id) VALUES (DATE '2026-08-20', 'Vedação do estaleiro danificada após temporal. Obra suspensa até reparação.', (SELECT id FROM obra WHERE nome = 'Escola Básica de Braga'));
INSERT INTO relatorio_seguranca (data_inspecao, descricao, obra_id) VALUES (DATE '2026-06-10', 'Inspeção final de segurança sem não-conformidades.', (SELECT id FROM obra WHERE nome = 'Hospital de Faro - Ala Norte'));
