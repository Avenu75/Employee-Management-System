-- VitalOps HR: configurable payroll (tax, deductions, adjustments, generated months)
-- Run ONCE against the employee_directory database before starting the new version
-- (the application uses spring.jpa.hibernate.ddl-auto=none, so it never creates tables).
--
--   mysql -u <user> -p employee_directory < sql-scripts/payroll-migration.sql

USE employee_directory;

-- 1. The rules set up before payroll is generated (tax, pension, dues, allowances)
CREATE TABLE IF NOT EXISTS payroll_item (
                                            id            INT           NOT NULL AUTO_INCREMENT,
                                            name          VARCHAR(60)   NOT NULL,
    kind          VARCHAR(12)   NOT NULL,                 -- EARNING or DEDUCTION
    calc_method   VARCHAR(10)   NOT NULL,                 -- PERCENT or FIXED
    item_value    DECIMAL(12,2) NOT NULL,                 -- 10.00 means 10% when PERCENT
    all_employees TINYINT(1)    NOT NULL DEFAULT 1,
    active        TINYINT(1)    NOT NULL DEFAULT 1,
    note          VARCHAR(200)  NULL,
    PRIMARY KEY (id)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 2. Which employees a rule applies to when it is not "all employees"
CREATE TABLE IF NOT EXISTS payroll_item_employee (
                                                     item_id     INT NOT NULL,
                                                     employee_id INT NOT NULL,
                                                     PRIMARY KEY (item_id, employee_id),
    CONSTRAINT fk_pie_item     FOREIGN KEY (item_id)     REFERENCES payroll_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_pie_employee FOREIGN KEY (employee_id) REFERENCES employee (id)     ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 3. One-off amounts for one employee in one month (advance, loan repayment, bonus)
CREATE TABLE IF NOT EXISTS payroll_adjustment (
                                                  id          INT           NOT NULL AUTO_INCREMENT,
                                                  employee_id INT           NOT NULL,
                                                  pay_month   VARCHAR(7)    NOT NULL,                   -- yyyy-MM
    label       VARCHAR(60)   NOT NULL,
    kind        VARCHAR(12)   NOT NULL,
    amount      DECIMAL(12,2) NOT NULL,
    note        VARCHAR(200)  NULL,
    created_by  VARCHAR(50)   NULL,
    created_at  DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_adj_month (pay_month),
    CONSTRAINT fk_adj_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 4. A generated month (DRAFT until finalised, then locked)
CREATE TABLE IF NOT EXISTS payroll_run (
                                           id           INT         NOT NULL AUTO_INCREMENT,
                                           pay_month    VARCHAR(7)  NOT NULL,
    status       VARCHAR(12) NOT NULL,
    working_days INT         NOT NULL,
    generated_by VARCHAR(50) NULL,
    generated_at DATETIME    NOT NULL,
    finalised_by VARCHAR(50) NULL,
    finalised_at DATETIME    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_payroll_run_month (pay_month)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 5. One saved line per employee (names are copied so history survives later edits)
CREATE TABLE IF NOT EXISTS payroll_line (
                                            id               INT           NOT NULL AUTO_INCREMENT,
                                            run_id           INT           NOT NULL,
                                            employee_id      INT           NULL,
                                            employee_name    VARCHAR(120)  NOT NULL,
    job_title        VARCHAR(100)  NULL,
    department       VARCHAR(100)  NULL,
    base             DECIMAL(12,2) NOT NULL,
    unpaid_days      INT           NOT NULL,
    unpaid_deduction DECIMAL(12,2) NOT NULL,
    earnings         DECIMAL(12,2) NOT NULL,
    gross            DECIMAL(12,2) NOT NULL,
    deductions       DECIMAL(12,2) NOT NULL,
    net              DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_line_run (run_id),
    CONSTRAINT fk_line_run FOREIGN KEY (run_id) REFERENCES payroll_run (id) ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 6. Every earning and deduction inside a line, with how it was worked out
CREATE TABLE IF NOT EXISTS payroll_line_item (
                                                 id      INT           NOT NULL AUTO_INCREMENT,
                                                 line_id INT           NOT NULL,
                                                 label   VARCHAR(60)   NOT NULL,
    kind    VARCHAR(12)   NOT NULL,
    basis   VARCHAR(100)  NULL,
    amount  DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_line_item_line (line_id),
    CONSTRAINT fk_line_item_line FOREIGN KEY (line_id) REFERENCES payroll_line (id) ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 7. Starting rules, the same 10% tax and 5% pension the old version used.
--    Change or delete them in the application (Payroll > Tax and deductions).
INSERT INTO payroll_item (name, kind, calc_method, item_value, all_employees, active, note)
SELECT 'Income tax', 'DEDUCTION', 'PERCENT', 10.00, 1, 1, 'Starting value: change to your local rate'
    WHERE NOT EXISTS (SELECT 1 FROM payroll_item WHERE name = 'Income tax');

INSERT INTO payroll_item (name, kind, calc_method, item_value, all_employees, active, note)
SELECT 'Pension', 'DEDUCTION', 'PERCENT', 5.00, 1, 1, 'Starting value: change to your local rate'
    WHERE NOT EXISTS (SELECT 1 FROM payroll_item WHERE name = 'Pension');
