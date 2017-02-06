--liquibase formatted sql

--changeset carl:1
CREATE TABLE collection_history
(
    collection_history_event_id serial PRIMARY KEY NOT NULL,
    collection_id VARCHAR(255) NOT NULL,
    collection_name VARCHAR(255) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    event_type INT NOT NULL,
    exception_text VARCHAR(255),
    uri VARCHAR(255),
    florence_user VARCHAR(255) NOT NULL
);
--rollback drop table collection_history;

--changeset carl:2
CREATE TABLE history_event_meta_data
(
    history_event_meta_data_id serial PRIMARY KEY NOT NULL,
    meta_data_key VARCHAR(255) NOT NULL,
    meta_data_value VARCHAR(255) NOT NULL,
    event_collection_history_event_id BIGINT,
    FOREIGN KEY (event_collection_history_event_id) REFERENCES collection_history (collection_history_event_id)
);
--rollback drop table history_event_meta_data;

--changeset carl:3
CREATE TABLE deleted_content (
    deleted_content_event_id serial PRIMARY KEY NOT NULL,
    collection_id character varying(255) NOT NULL,
    collection_name character varying(255) NOT NULL,
    event_date timestamp without time zone NOT NULL,
    page_title character varying(255),
    uri character varying(255)
);
--rollback drop table deleted_content;

--changeset carl:4
CREATE TABLE deleted_file (
    deleted_file_id serial PRIMARY KEY NOT NULL,
    uri character varying(255),
    deleted_content_event_id BIGINT,
    FOREIGN KEY (deleted_content_event_id) REFERENCES deleted_content(deleted_content_event_id)
);
--rollback drop table deleted_file;
