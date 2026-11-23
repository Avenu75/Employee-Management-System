# VitalOps HR - Deployment & Operations Notes

> These notes describe how to run the application safely. Nothing here has been
> verified against a live production environment; test in staging first. Legal
> compliance (e.g. Ghana Data Protection Act 2012, Act 843) must be confirmed by
> a qualified professional - this project does not claim compliance.

## 1. Database setup / migration
1. Fresh install: run `sql-scripts/employee-directory.sql` then `sql-scripts/security-schema.sql`.
   (`employee-directory.sql` DROPS and recreates tables - never run it on a database with data.)
2. Existing database: run `sql-scripts/employee-document-migration.sql` once. It only
   creates the missing `employee_document` table (non-destructive).
3. Take a backup BEFORE any migration: `mysqldump -u <user> -p employee_directory > backup.sql`.

## 2. Required environment variables (production)
| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection (use a least-privilege DB user) |
| `SUPER_ADMIN_USERNAME`, `SUPER_ADMIN_PASSWORD` | Seeded Super Admin. **Change the default password.** |
| `SUPER_ADMIN_FULL_NAME`, `SUPER_ADMIN_EMAIL` | Super Admin details |
| `COOKIE_SECURE=true` | Mark the session cookie Secure (requires HTTPS) |
| `FORWARD_HEADERS=framework` | When behind a reverse proxy / load balancer |
| `LOGIN_MAX_ATTEMPTS`, `LOGIN_LOCK_MINUTES` | Login throttling (defaults 5 / 15) |
| `SHOW_SQL=false` | Keep SQL logging off in production |

Never commit real credentials. Serve the app only over HTTPS.

## 3. Uploads
- Max 10 MB per file / 25 MB per request (`spring.servlet.multipart.*`).
- Photos and documents are stored Base64-encoded in the database. If uploads fail with
  a packet-size error, raise MySQL `max_allowed_packet` (e.g. 64 MB).
- Documents are only downloadable by their owner or users with `CAN_VIEW_EMPLOYEE`/`SUPER_ADMIN`.

## 4. Build & run
```
./mvnw clean package
java -jar target/*.jar
```
Run `./mvnw test` first and fix any failure before deploying.

## 5. Backups & recovery
- Schedule automated daily database dumps; keep copies off the server; test a restore regularly.

## 6. Known limitations
- Login throttling is in-memory (per instance, resets on restart).
- Images/documents live in the database; for large scale move to private object storage.
- Single-instance sessions; use a shared session store to scale horizontally.

## Leave, attendance, notifications and payroll

Run `sql-scripts/hr-features-migration.sql` once before starting this version. It adds the
`check_in` / `check_out` columns to `attendance` and creates `leave_request` and `notification`.

| Variable | Default | Meaning |
|---|---|---|
| `LEAVE_ANNUAL_DAYS` | `21` | Annual leave allowance, working days per calendar year |
| `WORK_START` | `09:00` | Check-ins after this time plus the grace period are recorded as LATE |
| `WORK_GRACE_MINUTES` | `15` | Grace period for late check-in |
| `PAYROLL_TAX_RATE` | `0.10` | Flat illustrative tax rate |
| `PAYROLL_PENSION_RATE` | `0.05` | Flat illustrative pension rate |
| `APP_CURRENCY` | `$` | Currency symbol shown on payroll pages and PDFs |

New permissions (created automatically on startup, grant them under Privileges):
`CAN_MANAGE_LEAVE` (approve or reject leave) and `CAN_VIEW_PAYROLL` (payroll summary and exports).

Known limitations: public holidays are not modelled (only Monday to Friday count as working days);
payroll treats the stored salary as a monthly gross figure and uses flat rates, so it is a summary,
not a statutory payroll; leave requests cannot span two calendar years; notifications are in-app only
(no email).

## Announcements, messages, reception and dark mode

Run `sql-scripts/communication-migration.sql` once before starting this version. It creates `announcement`,
`announcement_reply`, `private_message` and `worker_attendance`.

New permissions (created on startup, grant them under Privileges):
`CAN_MANAGE_ANNOUNCEMENTS` (publish announcements, see and answer every suggestion and issue) and
`CAN_USE_RECEPTION` (the reception gate page, which takes attendance of other workers).

Rules worth knowing:
- Taking other people's attendance (`CAN_MANAGE_ATTENDANCE`, `CAN_USE_RECEPTION`) is delegated to staff. The Super
  Admin and the Admin preset do not hold it; they keep only their own clock in and clock out.
- Other workers (cleaners, drivers and so on) have NO login. Reception records their attendance. Fewer accounts
  means fewer passwords that can leak.
- Suggestions and issues are visible only to their author and to management. Private messages are readable only by
  the two people in them (not even the Super Admin).
- A disabled account is told so on the login page, but only after the correct password is entered, so the page
  never reveals which usernames exist.
