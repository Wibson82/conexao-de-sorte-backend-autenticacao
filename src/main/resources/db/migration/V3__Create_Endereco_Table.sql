-- ================================================================
-- MIGRATION V3: Criação da tabela Endereco
-- Compatível com Java 25 + Spring Boot 3.5.5 + R2DBC
-- ================================================================

-- Tabela de Endereços
CREATE TABLE IF NOT EXISTS endereco (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL DEFAULT 'RESIDENCIAL',
    logradouro VARCHAR(255) NOT NULL,
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100) NOT NULL,
    estado CHAR(2) NOT NULL,
    cep VARCHAR(10) NOT NULL,
    pais CHAR(2) NOT NULL DEFAULT 'BR',
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    referencia TEXT,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    criado_por BIGINT,
    atualizado_por BIGINT,
    versao BIGINT NOT NULL DEFAULT 0,
    
    -- Índices para performance
    INDEX idx_endereco_usuario (usuario_id),
    INDEX idx_endereco_principal (usuario_id, principal),
    INDEX idx_endereco_ativo (ativo),
    INDEX idx_endereco_tipo (tipo),
    INDEX idx_endereco_cep (cep),
    INDEX idx_endereco_cidade_estado (cidade, estado),
    INDEX idx_endereco_coordenadas (latitude, longitude),
    INDEX idx_endereco_data_criacao (data_criacao),
    
    -- Índice composto para consultas comuns
    INDEX idx_endereco_usuario_ativo (usuario_id, ativo),
    INDEX idx_endereco_usuario_tipo (usuario_id, tipo, ativo),
    
    -- Constraint para garantir apenas um endereço principal por usuário
    UNIQUE KEY uk_endereco_principal (usuario_id, principal),
    
    -- Check constraints
    CONSTRAINT chk_endereco_tipo CHECK (tipo IN ('RESIDENCIAL', 'COMERCIAL', 'ENTREGA', 'COBRANCA', 'TRABALHO')),
    CONSTRAINT chk_endereco_estado CHECK (estado REGEXP '^[A-Z]{2}$'),
    CONSTRAINT chk_endereco_cep CHECK (cep REGEXP '^[0-9]{5}-?[0-9]{3}$'),
    CONSTRAINT chk_endereco_pais CHECK (pais REGEXP '^[A-Z]{2}$'),
    CONSTRAINT chk_endereco_coordenadas CHECK (
        (latitude IS NULL AND longitude IS NULL) OR 
        (latitude IS NOT NULL AND longitude IS NOT NULL AND 
         latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    )
);

-- Trigger para garantir apenas um endereço principal por usuário
DELIMITER $$

CREATE TRIGGER IF NOT EXISTS endereco_principal_unico
    BEFORE INSERT ON endereco
    FOR EACH ROW
BEGIN
    -- Se está definindo como principal, remove principal dos outros
    IF NEW.principal = TRUE THEN
        UPDATE endereco 
        SET principal = FALSE, data_atualizacao = CURRENT_TIMESTAMP 
        WHERE usuario_id = NEW.usuario_id AND principal = TRUE;
    END IF;
    
    -- Se não tem nenhum endereço principal e é o primeiro, define como principal
    IF NEW.principal = FALSE AND NOT EXISTS (
        SELECT 1 FROM endereco 
        WHERE usuario_id = NEW.usuario_id AND principal = TRUE AND ativo = TRUE
    ) THEN
        SET NEW.principal = TRUE;
    END IF;
END$$

CREATE TRIGGER IF NOT EXISTS endereco_principal_update
    BEFORE UPDATE ON endereco
    FOR EACH ROW
BEGIN
    -- Se está definindo como principal, remove principal dos outros
    IF NEW.principal = TRUE AND OLD.principal = FALSE THEN
        UPDATE endereco 
        SET principal = FALSE, data_atualizacao = CURRENT_TIMESTAMP 
        WHERE usuario_id = NEW.usuario_id AND principal = TRUE AND id != NEW.id;
    END IF;
    
    -- Atualizar timestamp e versão
    SET NEW.data_atualizacao = CURRENT_TIMESTAMP;
    SET NEW.versao = OLD.versao + 1;
END$$

DELIMITER ;

-- Função para normalizar CEP (remove formatação)
DELIMITER $$

CREATE FUNCTION IF NOT EXISTS normalizar_cep(cep_input VARCHAR(10))
RETURNS VARCHAR(8)
READS SQL DATA
DETERMINISTIC
BEGIN
    RETURN REGEXP_REPLACE(cep_input, '[^0-9]', '');
END$$

DELIMITER ;

-- Índice funcional para CEP normalizado (para busca sem formatação)
CREATE INDEX idx_endereco_cep_normalizado ON endereco ((normalizar_cep(cep)));

-- Comentário da tabela
ALTER TABLE endereco COMMENT = 'Tabela de endereços dos usuários - Genérica e reutilizável em qualquer setor';