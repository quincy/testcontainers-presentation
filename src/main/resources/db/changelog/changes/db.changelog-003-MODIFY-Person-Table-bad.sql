-- liquibase formatted sql

-- changeset quincy:003-MODIFY-Person-Table-bad

ALTER TABLE Person
    ADD fullName VARCHAR(14) GENERATED ALWAYS AS (CONCAT(firstName, ' ', lastName)) NOT NULL;
