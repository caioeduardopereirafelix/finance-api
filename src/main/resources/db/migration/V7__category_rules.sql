-- Categoria que o usuario escolheu para um estabelecimento: vale nas proximas importacoes.
CREATE TABLE category_rules (
    id         UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,
    match_key  VARCHAR(120)             NOT NULL,
    type       VARCHAR(20)              NOT NULL,
    category   VARCHAR(30)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_category_rules PRIMARY KEY (id),
    CONSTRAINT fk_category_rules_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_category_rules UNIQUE (user_id, match_key, type)
);
