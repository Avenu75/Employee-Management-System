-- VitalOps HR: announcements, private messages, reception attendance
-- Run ONCE against the employee_directory database before starting the new version.
--
--   mysql -u <user> -p employee_directory < sql-scripts/communication-migration.sql
--
-- If you already ran an EARLIER version of this file (it also added app_user.other_worker_id),
-- run sql-scripts/remove-worker-accounts.sql instead of this one.

USE employee_directory;

-- 1. Announcements (Administration / Public) and replies
CREATE TABLE IF NOT EXISTS announcement (
                                            id         INT           NOT NULL AUTO_INCREMENT,
                                            category   VARCHAR(20)   NOT NULL,
    kind       VARCHAR(20)   NOT NULL,
    title      VARCHAR(150)  NOT NULL,
    body       VARCHAR(4000) NOT NULL,
    author_id  INT           NOT NULL,
    created_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_announcement_category (category, created_at),
    CONSTRAINT fk_announcement_author FOREIGN KEY (author_id) REFERENCES app_user (id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS announcement_reply (
                                                  id              INT           NOT NULL AUTO_INCREMENT,
                                                  announcement_id INT           NOT NULL,
                                                  author_id       INT           NOT NULL,
                                                  body            VARCHAR(2000) NOT NULL,
    created_at      DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reply_announcement (announcement_id),
    CONSTRAINT fk_reply_announcement FOREIGN KEY (announcement_id) REFERENCES announcement (id) ON DELETE CASCADE,
    CONSTRAINT fk_reply_author FOREIGN KEY (author_id) REFERENCES app_user (id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Private messages
CREATE TABLE IF NOT EXISTS private_message (
                                               id           INT           NOT NULL AUTO_INCREMENT,
                                               sender_id    INT           NOT NULL,
                                               recipient_id INT           NOT NULL,
                                               body         VARCHAR(2000) NOT NULL,
    created_at   DATETIME      NOT NULL,
    read_at      DATETIME      NULL,
    PRIMARY KEY (id),
    KEY idx_message_recipient (recipient_id, read_at),
    KEY idx_message_sender (sender_id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_recipient FOREIGN KEY (recipient_id) REFERENCES app_user (id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Reception attendance for other workers
CREATE TABLE IF NOT EXISTS worker_attendance (
                                                 id                  INT         NOT NULL AUTO_INCREMENT,
                                                 other_worker_id     INT         NOT NULL,
                                                 attendance_date     DATE        NOT NULL,
                                                 check_in            TIME        NULL,
                                                 check_out           TIME        NULL,
                                                 status              VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    recorded_by_user_id INT         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_worker_attendance (other_worker_id, attendance_date),
    CONSTRAINT fk_worker_att_worker FOREIGN KEY (other_worker_id) REFERENCES other_worker (id) ON DELETE CASCADE,
    CONSTRAINT fk_worker_att_user FOREIGN KEY (recorded_by_user_id) REFERENCES app_user (id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
