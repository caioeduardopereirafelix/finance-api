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