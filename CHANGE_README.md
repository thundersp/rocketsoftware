# Change README

## Feature Summary
This change adds automatic login generation during database seeding.

The seed script now ensures:
1. Every user entity represented in `Employee` (including ADMIN, MANAGER, and EMPLOYEE users) has login credentials in `User_Credentials`.

## What This Feature Does
1. Creates missing login credentials for users that do not already have a credentials row.
2. Covers login generation for ADMIN, MANAGER, and EMPLOYEE users because all are sourced from the `Employee` table.
3. Uses email as login username (as described in the SQL comment) and assigns a default password hash value: `{noop}Password123!`.
4. Avoids duplicates by inserting only records that do not already exist.

## Difference From Previous File
Before this change:
1. The file relied on static seed rows only.
2. Users outside the static credential inserts (including ADMIN/MANAGER/EMPLOYEE users) could be left without login credentials.

After this change:
1. Added a dynamic `INSERT INTO User_Credentials ... SELECT ... FROM Employee` block to generate logins for missing role-based users.
2. Added a role filter so this generation targets ADMIN/MANAGER/EMPLOYEE users (`RoleId IN (1, 4, 5)`).
3. Seeding behavior is now coverage-based instead of only static-row-based for login creation.

## Exact Changes Added In database/insert.sql
1. Added comment and SQL block for role-based login generation (ADMIN, MANAGER, EMPLOYEE) not covered by static seed rows.
2. New logic computes `CredentialId` as `MaxCredentialId + ROW_NUMBER()` to keep IDs unique for newly inserted rows.
3. Added `EXISTS` filter on `Employee_Roles` with `RoleId IN (1, 4, 5)` so credential generation is scoped to Admin/Manager/Employee users.
4. Added `LEFT JOIN User_Credentials` plus `WHERE uc.EmployeeId IS NULL` to keep inserts idempotent for existing credentials.

## Why This Change Was Needed
1. Prevents incomplete login setup for Admin, Manager, and Employee users not explicitly listed in static inserts.
2. Reduces manual DB fixes after seeding.
3. Makes local/test onboarding more reliable and repeatable.

## Scope
1. Changed file: `database/insert.sql`
2. No backend API/controller/service code changes.
3. No frontend changes.

## Impact
1. New seeded environments will automatically generate login credentials for missing Admin/Manager/Employee users.
2. Existing environments are affected only if this script is executed again.

## Verification
1. Run seed script.
2. Verify missing credentials are created:
	- Check that users mapped to ADMIN, MANAGER, and EMPLOYEE roles have corresponding `User_Credentials` rows.
	- Check that users without roles 1, 4, or 5 are not part of this auto-generation scope.
3. Re-run script and verify no duplicate rows are inserted by the new block.

## Risk / Notes
1. Default password value is seeded as `{noop}Password123!`; change policy if stronger defaults are required.
2. Role IDs are assumed as ADMIN=1, EMPLOYEE=4, MANAGER=5 based on seed data in `Roles`.

## Suggested PR Title
Auto-generate missing login credentials for Admin/Manager/Employee users

## Suggested PR Description
1. Added a dynamic credentials insert in `database/insert.sql`.
2. The insert auto-creates missing `User_Credentials` rows for Admin/Manager/Employee users.
3. Added role-based `EXISTS` filter `RoleId IN (1, 4, 5)` to match login generation task scope and support multi-role users safely.
4. Insert remains idempotent for existing credentials via `LEFT JOIN ... IS NULL`.
