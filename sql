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

create table users(
id uuid not null primary key,
first_name varchar(100) not null,
email varchar(150) not null,
password varchar(150) not null,
role varchar(40) ,
created_by varchar(100),
created_date timestamp,
last_modified_by varchar(100),
last_modified_date timestamp
)