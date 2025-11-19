-- V3__add_account_lock_columns.sql
-- Adding fields that needed for locking account if user failed to put correct password for 5 times in an hour

ALTER TABLE um_users
ADD COLUMN failed_attempts INT DEFAULT 0;

ALTER TABLE um_users
ADD COLUMN last_failed_attempt TIMESTAMP NULL;

ALTER TABLE um_users
ADD COLUMN locked_until TIMESTAMP NULL;
