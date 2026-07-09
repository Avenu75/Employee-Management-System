-- =====================================================================
--  Employee Management System - Database Schema & Seed Data
--  Small Business Edition
-- =====================================================================
CREATE DATABASE IF NOT EXISTS `employee_directory`;
USE `employee_directory`;

-- Drop dependent tables first (in case this script is being re-run after
-- security-schema.sql has already created tables with foreign keys pointing
-- at `employee`) - this makes running this script standalone or in any
-- order against a fresh OR partially-set-up database always work cleanly.
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `attendance`;
DROP TABLE IF EXISTS `user_permission`;
DROP TABLE IF EXISTS `app_user`;

-- ---------------------------------------------------------------------
-- Table: department
-- A small business is organised into departments (1 department -> many employees)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `employee`;
DROP TABLE IF EXISTS `department`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `department` (
  `id`       INT NOT NULL AUTO_INCREMENT,
  `name`     VARCHAR(60)  NOT NULL,
  `location` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO `department` (`id`, `name`, `location`) VALUES
  (1, 'Sales',            'Accra HQ'),
  (2, 'Human Resources',  'Accra HQ'),
  (3, 'IT & Support',     'Accra HQ'),
  (4, 'Finance',          'Accra HQ'),
  (5, 'Customer Service', 'Kumasi Branch');

-- ---------------------------------------------------------------------
-- Table: employee
-- Extended with realistic small-business HR fields
-- ---------------------------------------------------------------------
CREATE TABLE `employee` (
  `id`            INT NOT NULL AUTO_INCREMENT,
  `first_name`    VARCHAR(45)   NOT NULL,
  `last_name`     VARCHAR(45)   NOT NULL,
  `email`         VARCHAR(80)   NOT NULL,
  `phone_number`  VARCHAR(20)   DEFAULT NULL,
  `job_title`     VARCHAR(60)   DEFAULT NULL,
  `salary`        DECIMAL(10,2) DEFAULT 0.00,
  `hire_date`     DATE          DEFAULT NULL,
  `status`        VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
  `department_id` INT           DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_employee_email` (`email`),
  KEY `fk_employee_department_idx` (`department_id`),
  CONSTRAINT `fk_employee_department`
    FOREIGN KEY (`department_id`) REFERENCES `department` (`id`)
    ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;

INSERT INTO `employee`
  (`first_name`,`last_name`,`email`,`phone_number`,`job_title`,`salary`,`hire_date`,`status`,`department_id`)
VALUES
  ('Rachel','Green',    'rachel@company.com',  '024-555-0101','Sales Executive',        3200.00,'2021-03-15','ACTIVE',1),
  ('Ross','Geller',     'ross@company.com',    '024-555-0102','IT Support Officer',     3600.00,'2020-07-01','ACTIVE',3),
  ('Chandler','Bing',   'chandler@company.com','024-555-0103','Finance Officer',        4200.00,'2019-11-20','ACTIVE',4),
  ('Joey','Tribbiani',  'joey@company.com',    '024-555-0104','Customer Service Rep',   2500.00,'2022-01-10','ACTIVE',5),
  ('Monica','Geller',   'monica@company.com',  '024-555-0105','HR Manager',             4500.00,'2018-05-05','ACTIVE',2),
  ('Phoebe','Buffay',   'phoebe@company.com',  '024-555-0106','Sales Executive',        3100.00,'2023-02-14','INACTIVE',1);
