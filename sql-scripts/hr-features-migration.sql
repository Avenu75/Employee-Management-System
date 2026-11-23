-- VitalOps HR: leave, notifications, attendance check-in/out
-- Run ONCE against the employee_directory database before starting the new version
-- (the application uses spring.jpa.hibernate.ddl-auto=none, so it never creates tables).
--
--   mysql -u <user> -p employee_directory < sql-scripts/hr-features-migration.sql

USE employee_directory;

-- 1. Attendance: clock-in and clock-out times
ALTER TABLE attendance
    ADD COLUMN check_in  TIME NULL,
    ADD COLUMN check_out TIME NULL;

-- 2. Leave requests
CREATE TABLE IF NOT EXISTS leave_request (
                                             id                  INT          NOT NULL AUTO_INCREMENT,
                                             employee_id         INT          NOT NULL,
                                             leave_type          VARCHAR(20)  NOT NULL,
    start_date          DATE         NOT NULL,
    end_date            DATE         NOT NULL,
    days                INT          NOT NULL,
    reason              VARCHAR(500) NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id INT          NULL,
    review_comment      VARCHAR(500) NULL,
    created_at          DATETIME     NOT NULL,
    reviewed_at         DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_leave_employee (employee_id),
    KEY idx_leave_status (status),
    CONSTRAINT fk_leave_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES app_user (id) ON DELETE SET NULL
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 3. Notifications
CREATE TABLE IF NOT EXISTS notification (
                                            id         INT          NOT NULL AUTO_INCREMENT,
                                            user_id    INT          NOT NULL,
                                            message    VARCHAR(300) NOT NULL,
    link       VARCHAR(200) NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notification_user (user_id, is_read),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- The two new permissions (CAN_MANAGE_LEAVE, CAN_VIEW_PAYROLL) are created automatically
-- the next time the application starts. The Super Admin always has access.
