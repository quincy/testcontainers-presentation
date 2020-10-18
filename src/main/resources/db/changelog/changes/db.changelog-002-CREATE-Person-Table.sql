-- liquibase formatted sql

-- changeset quincy:002-CREATE-Person-Table

CREATE TABLE Person
(
    id        BIGINT AUTO_INCREMENT,
    addressId BIGINT null,
    firstName VARCHAR(40)  NOT NULL,
    lastName  VARCHAR(100) NOT NULL,
    CONSTRAINT Person_pk
        PRIMARY KEY (id),
    constraint Person_Address_id_fk
        foreign key (addressId) references Address (id)
);

