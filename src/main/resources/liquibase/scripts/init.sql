-- liquibase formatted sql

-- changeset AlexKutin:1
CREATE TABLE notification_tasks
(
    id                     BIGSERIAL PRIMARY KEY,
    message                TEXT,
    chat_id                BIGINT    NOT NULL,
    notification_date_time TIMESTAMP NOT NULL
);
