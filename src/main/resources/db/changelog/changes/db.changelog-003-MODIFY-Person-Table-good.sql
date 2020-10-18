-- liquibase formatted sql

-- changeset quincy:003-MODIFY-Person-Table-good

ALTER TABLE Person
    ADD fullName VARCHAR(141) GENERATED ALWAYS AS (CONCAT(firstName, ' ', lastName)) NOT NULL;
