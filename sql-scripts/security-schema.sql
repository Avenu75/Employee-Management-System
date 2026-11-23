-- =====================================================================
--  Security schema: Single Super Admin + Action-Based Permissions
-- =====================================================================
-- Replaces the old role-based "users"/"authorities" tables entirely.
-- Run this AFTER employee-directory.sql (it references the employee table).
--
-- NOTE: This script only creates the TABLES. The actual super admin account
-- and the permission catalog rows are seeded automatically at application
-- startup by DataInitializer.java (a Spring Boot CommandLineRunner) - not
-- by this SQL file - because the super admin's password must be BCrypt
-- hashed with the exact same PasswordEncoder bean the running application
-- uses to verify it at login time, and its value comes from an environment
-- variable rather than being hard-coded here.
-- =====================================================================

USE `employee_directory`;

DROP TABLE IF EXISTS `security_audit_log`;
DROP TABLE IF EXISTS `user_permission`;
DROP TABLE IF EXISTS `app_user`;
DROP TABLE IF EXISTS `permission`;

-- Legacy tables from the earlier role-based security model - no longer used.
DROP TABLE IF EXISTS `authorities`;
DROP TABLE IF EXISTS `users`;

-- ---------------------------------------------------------------------
-- Table: app_user
-- One row per login account. Deliberately separate from `employee`
-- (see AppUser.java javadoc) - a user account is not the same thing as
-- an HR record. `employee_id` is an OPTIONAL link between the two.
-- ---------------------------------------------------------------------
CREATE TABLE `app_user` (
  `id`          INT NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(50)  NOT NULL,
  `password`    VARCHAR(100) NOT NULL,
  `full_name`   VARCHAR(120) NOT NULL,
  `email`       VARCHAR(120) DEFAULT NULL,
  `enabled`     TINYINT(1)   NOT NULL DEFAULT 1,
  `super_admin` TINYINT(1)   NOT NULL DEFAULT 0,
  `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `employee_id` INT          DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_app_user_username` (`username`),
  KEY `fk_app_user_employee_idx` (`employee_id`),
  CONSTRAINT `fk_app_user_employee`
    FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- ---------------------------------------------------------------------
-- Table: permission
-- The fixed catalog of grantable actions, e.g. CAN_ADD_EMPLOYEE.
-- Rows are seeded automatically at startup from PermissionCode.ALL_CODES
-- (see DataInitializer.java) - this CREATE TABLE only defines the shape.
-- ---------------------------------------------------------------------
CREATE TABLE `permission` (
  `id`           INT NOT NULL AUTO_INCREMENT,
  `code`         VARCHAR(60)  NOT NULL,
  `display_name` VARCHAR(120) NOT NULL,
  `description`  VARCHAR(255) DEFAULT NULL,
  `category`     VARCHAR(60)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_permission_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- ---------------------------------------------------------------------
-- Table: user_permission
-- Join table between app_user and permission, WITH an audit trail
-- (granted_by, granted_at) instead of a bare many-to-many - see
-- UserPermission.java javadoc for the reasoning.
-- ---------------------------------------------------------------------
CREATE TABLE `user_permission` (
  `id`                INT NOT NULL AUTO_INCREMENT,
  `user_id`           INT NOT NULL,
  `permission_id`     INT NOT NULL,
  `granted_by_user_id` INT DEFAULT NULL,
  `granted_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_permission` (`user_id`, `permission_id`),
  KEY `fk_user_permission_permission_idx` (`permission_id`),
  KEY `fk_user_permission_granted_by_idx` (`granted_by_user_id`),
  CONSTRAINT `fk_user_permission_user`
    FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_user_permission_permission`
    FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_user_permission_granted_by`
    FOREIGN KEY (`granted_by_user_id`) REFERENCES `app_user` (`id`)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- ---------------------------------------------------------------------
-- Table: security_audit_log
-- Records who changed passwords, granted permissions, disabled users,
-- and other sensitive actions for traceability and incident response.
-- ---------------------------------------------------------------------
CREATE TABLE `security_audit_log` (
  `id`            INT NOT NULL AUTO_INCREMENT,
  `action`        VARCHAR(120) NOT NULL,
  `actor_username` VARCHAR(50) DEFAULT NULL,
  `target_username` VARCHAR(50) DEFAULT NULL,
  `details`       VARCHAR(500) DEFAULT NULL,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_security_audit_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- ---------------------------------------------------------------------
-- Table: attendance
-- One row per employee per day, gated entirely by CAN_MANAGE_ATTENDANCE.
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `attendance`;

CREATE TABLE `attendance` (
  `id`               INT NOT NULL AUTO_INCREMENT,
  `employee_id`      INT NOT NULL,
  `attendance_date`  DATE NOT NULL,
  `status`           VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
  `note`             VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_attendance_employee_date` (`employee_id`, `attendance_date`),
  CONSTRAINT `fk_attendance_employee`
    FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
