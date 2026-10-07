-- Dados de demonstração, carregados após a criação do esquema (somente em desenvolvimento).
-- Uma instrução por linha: alguns providers JPA leem o script linha a linha.
INSERT INTO obra (nome, localizacao, status, versao, data_criacao) VALUES ('Edifício Aurora', 'São Paulo, Vila Olímpia', 'EM_ANDAMENTO', 0, CURRENT_TIMESTAMP);
INSERT INTO obra (nome, localizacao, status, versao, data_criacao) VALUES ('Passarela do Rio Capibaribe', 'Recife, Boa Vista', 'PLANEJADA', 0, CURRENT_TIMESTAMP);
INSERT INTO obra (nome, localizacao, status, versao, data_criacao) VALUES ('Escola Municipal Jardim Botânico', 'Curitiba, Jardim Botânico', 'SUSPENSA', 0, CURRENT_TIMESTAMP);
INSERT INTO obra (nome, localizacao, status, versao, data_criacao) VALUES ('Hospital Regional - Ala Norte', 'Fortaleza, Aldeota', 'CONCLUIDA', 0, CURRENT_TIMESTAMP);
INSERT INTO relatorio_seguranca (data_inspecao, descricao, obra_id) VALUES (DATE '2026-09-15', 'Andaimes do bloco A verificados. Guarda-corpos em conformidade com a NR-18.', (SELECT id FROM obra WHERE nome = 'Edifício Aurora'));
INSERT INTO relatorio_seguranca (data_inspecao, descricao, obra_id) VALUES (DATE '2026-09-30', 'Dois trabalhadores sem capacete na área de concretagem. Ação corretiva aplicada.', (SELECT id FROM obra WHERE nome = 'Edifício Aurora'));
INSERT INTO relatorio_seguranca (data_inspecao, descricao, obra_id) VALUES (DATE '2026-08-20', 'Tapume do canteiro danificado após temporal. Obra suspensa até o reparo.', (SELECT id FROM obra WHERE nome = 'Escola Municipal Jardim Botânico'));
INSERT INTO relatorio_seguranca (data_inspecao, descricao, obra_id) VALUES (DATE '2026-06-10', 'Inspeção final de segurança sem não conformidades.', (SELECT id FROM obra WHERE nome = 'Hospital Regional - Ala Norte'));
