-- ================================================================
-- MIGRATION V2: Criação das tabelas Papel e UsuarioPapel
-- Compatível com Java 25 + Spring Boot 3.5.5 + R2DBC
-- ================================================================

-- Tabela de Papéis (Roles)
CREATE TABLE IF NOT EXISTS papel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE,
    descricao TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    permissoes JSON,
    nivel_acesso INT NOT NULL DEFAULT 1,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    criado_por BIGINT,
    atualizado_por BIGINT,
    versao BIGINT NOT NULL DEFAULT 0,
    
    INDEX idx_papel_nome (nome),
    INDEX idx_papel_ativo (ativo),
    INDEX idx_papel_nivel (nivel_acesso),
    INDEX idx_papel_data_criacao (data_criacao)
);

-- Tabela de relacionamento Usuario-Papel
CREATE TABLE IF NOT EXISTS usuario_papel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    papel_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_atribuicao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_expiracao DATETIME NULL,
    atribuido_por BIGINT,
    observacoes TEXT,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,
    
    -- Índices para performance
    INDEX idx_usuario_papel_usuario (usuario_id),
    INDEX idx_usuario_papel_papel (papel_id),
    INDEX idx_usuario_papel_ativo (ativo),
    INDEX idx_usuario_papel_expiracao (data_expiracao),
    INDEX idx_usuario_papel_atribuicao (data_atribuicao),
    INDEX idx_usuario_papel_composto (usuario_id, papel_id, ativo),
    
    -- Constraint única para evitar duplicatas
    UNIQUE KEY uk_usuario_papel (usuario_id, papel_id),
    
    -- Foreign Keys (quando as tabelas usuarios existirem)
    CONSTRAINT fk_usuario_papel_papel FOREIGN KEY (papel_id) REFERENCES papel(id) ON DELETE CASCADE
);

-- Inserir papéis padrão do sistema
INSERT INTO papel (nome, descricao, nivel_acesso, permissoes) VALUES
('ADMIN', 'Administrador do sistema com acesso total', 5, JSON_OBJECT('all', true)),
('MANAGER', 'Gerente com acesso a operações críticas', 4, JSON_OBJECT('manage_users', true, 'view_reports', true)),
('MODERATOR', 'Moderador com acesso a moderação de conteúdo', 3, JSON_OBJECT('moderate_content', true, 'manage_basic_users', true)),
('USER', 'Usuário padrão do sistema', 1, JSON_OBJECT('basic_access', true)),
('GUEST', 'Visitante com acesso limitado', 0, JSON_OBJECT('read_only', true))
ON DUPLICATE KEY UPDATE
    descricao = VALUES(descricao),
    nivel_acesso = VALUES(nivel_acesso),
    permissoes = VALUES(permissoes),
    data_atualizacao = CURRENT_TIMESTAMP;

-- Trigger para atualizar automaticamente data_atualizacao
DELIMITER $$

CREATE TRIGGER IF NOT EXISTS papel_update_timestamp
    BEFORE UPDATE ON papel
    FOR EACH ROW
BEGIN
    SET NEW.data_atualizacao = CURRENT_TIMESTAMP;
    SET NEW.versao = OLD.versao + 1;
END$$

CREATE TRIGGER IF NOT EXISTS usuario_papel_update_timestamp
    BEFORE UPDATE ON usuario_papel
    FOR EACH ROW
BEGIN
    SET NEW.data_atualizacao = CURRENT_TIMESTAMP;
    SET NEW.versao = OLD.versao + 1;
END$$

DELIMITER ;

-- Comentários das tabelas
ALTER TABLE papel COMMENT = 'Tabela de papéis/roles do sistema - Genérica e reutilizável';
ALTER TABLE usuario_papel COMMENT = 'Relacionamento muitos-para-muitos entre usuários e papéis';