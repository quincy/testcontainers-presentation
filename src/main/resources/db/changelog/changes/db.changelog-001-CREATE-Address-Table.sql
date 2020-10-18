-- liquibase formatted sql

-- changeset quincy:001-CREATE-Address-Table

create table Address
(
    id BIGINT auto_increment,
    streetAddress VARCHAR(100) not null,
    city VARCHAR(100) not null,
    state CHAR(2) not null,
    zipCode VARCHAR(10) not null,
    constraint Address_pk
        primary key (id)
);