-- Run this once against the employee_directory database.
-- The EmployeeDocument entity maps to this table, but it was missing from the
-- original scripts. Without it, viewing a profile and saving an employee fail.
USE employee_directory;

CREATE TABLE IF NOT EXISTS `employee_document` (
                                                   `id`           INT NOT NULL AUTO_INCREMENT,
                                                   `employee_id`  INT NOT NULL,
                                                   `file_name`    VARCHAR(255) NOT NULL,
    `content_type` VARCHAR(120) NOT NULL,
    `size_bytes`   BIGINT DEFAULT NULL,
    `uploaded_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `file_data`    LONGTEXT NOT NULL,
    `description`  VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_employee_document_employee_idx` (`employee_id`),
    CONSTRAINT `fk_employee_document_employee`
    FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
