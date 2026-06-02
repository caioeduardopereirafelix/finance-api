CREATE TABLE transactions (
    id UUID PRIMARY KEY,

    description VARCHAR(255),

    amount NUMERIC(38,2),

    type VARCHAR(255),

    date TIMESTAMP,

    user_id UUID,

    category_id UUID,

    CONSTRAINT fk_transaction_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_transaction_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
);