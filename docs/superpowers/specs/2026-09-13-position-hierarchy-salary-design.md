# Position Hierarchy and Salary Range Design

**Date:** 2026-09-13

## Goal

Extend position management with a simple numeric hierarchy and a VND salary range. A manager may manage only lower-level employees in the same department. Position codes become immutable after creation. Changes that invalidate existing reporting relationships require explicit confirmation and then remove only the invalid relationships.

## Scope

This change covers:

- position data, validation, APIs, Redis caching, and the `/admin/positions` UI;
- reporting-line validation when assigning a subordinate;
- impact preview and confirmation when a position definition or an employee's position changes;
- automatic removal of reporting lines that become invalid after confirmed changes;
- tests for backend business rules, API contracts, cache behavior, and frontend helpers.

It does not add payroll calculation, an employee-specific salary, salary history, multiple currencies, or cross-department management.

## Data Model

Add these columns to `positions` in the next available Flyway migration:

| Column | Database type | Rules |
| --- | --- | --- |
| `hierarchy_level` | `INTEGER` | Required, positive, default `1` for existing rows |
| `min_salary` | `BIGINT` | Required, VND, no decimal part, non-negative, default `0` |
| `max_salary` | `BIGINT` | Required, VND, no decimal part, `max_salary >= min_salary`, default `0` |

Database check constraints enforce the numeric invariants. Java represents salaries as `Long` because the values are whole VND amounts. Multiple positions may share a hierarchy level.

No new reporting-line table is introduced. The existing `users.manager_id` remains the source of truth; invalidated relationships are disabled by setting the subordinate's `manager_id` to `NULL`.

## Position API Contract

Use separate request DTOs so immutability is enforced structurally:

- `CreatePositionRequest`: `departmentId`, `code`, `name`, `description`, `active`, `hierarchyLevel`, `minSalary`, `maxSalary`.
- `UpdatePositionRequest`: all mutable fields above except `code`, plus `resetInvalidRelations`.
- `PositionDto`: response fields, including hierarchy level and salary range.

`PUT /api/positions/{id}` never accepts or modifies the position code. Existing code is preserved by the service and mapper.

Position codes are globally unique. Creation trims the supplied code, converts it to uppercase, and checks uniqueness against the normalized value before saving. The database unique constraint remains the final concurrency-safe guard. A duplicate code returns HTTP 409. This feature does not relax the existing uniqueness rules for department codes, employee codes, or other business identifiers.

Add:

`GET /api/positions/{id}/hierarchy-impact?departmentId={id}&hierarchyLevel={level}`

The endpoint returns a `HierarchyImpactResponse` containing:

- `affectedRelationCount`;
- `affectedEmployeeCount`;
- a human-readable summary for display.

The preview is advisory. The update service recalculates the impact inside its write transaction. If invalid relationships exist and `resetInvalidRelations` is not true, it returns HTTP 409. If confirmation is true, it updates the position and clears only the now-invalid `manager_id` values.

Changing a position's department is treated the same as changing its hierarchy level because it can invalidate same-department reporting lines.

## Employee API Contract

Add:

`GET /api/employees/{id}/hierarchy-impact?departmentId={id}&positionId={id}`

Extend `UpdateEmployeeRequest` with `resetInvalidRelations`. When department or position changes, the service evaluates both directions:

- the employee as a subordinate of their current manager;
- every active, non-deleted employee currently reporting to that employee.

Without confirmation, an invalidating update returns HTTP 409. With confirmation, the employee update proceeds and only invalid reporting lines are cleared.

Creating an employee does not require an impact preview because the new employee has no reporting relationships.

## Reporting-Line Rules

A manager-to-subordinate relationship is valid only when all conditions hold:

1. Both employees exist, are not soft-deleted, and have positions.
2. Both positions exist and belong to the same department.
3. `managerPosition.hierarchyLevel > subordinatePosition.hierarchyLevel`.
4. The manager is a full-time employee, preserving the existing rule.
5. The relationship does not create a management cycle.

Positions at the same level cannot manage one another. Cross-department management is not allowed.

`assignSubordinate` applies these checks on every request. Frontend filtering improves usability but is not a security or integrity boundary.

## Impact Calculation and Invalidating Relationships

Create a focused organization-hierarchy service responsible for:

- validating a proposed manager/subordinate pair;
- calculating affected reporting lines for a proposed employee assignment change;
- calculating affected reporting lines for a proposed position level or department change;
- clearing a deduplicated set of invalid subordinate `manager_id` values after confirmation.

For a position update, inspect reporting lines where either the manager or subordinate holds the edited position. Evaluate each relation using the proposed position department and level. Count each reporting line once even if both employees hold the same edited position.

For an employee update, evaluate the employee's incoming reporting line and all outgoing reporting lines using the proposed department and position.

All preview queries respect the same employee data-scope authorization as the associated update operation. The write path recalculates inside a transaction to handle concurrent changes safely.

## Position Management UI

Update `/admin/positions` as follows:

- Add table columns for hierarchy level and salary range.
- Format salary as Vietnamese whole-number currency, for example `12.000.000 ₫ – 18.000.000 ₫`.
- Add hierarchy level, minimum salary, and maximum salary fields to create/edit/view dialogs.
- Disable the code input in edit and view modes. Update payloads omit `code`.
- Validate positive level, non-negative salary values, and maximum not below minimum before submission.
- Before a level or department change, call the position impact endpoint. If impact is non-zero, show a `SystemNotification.confirm` dialog containing the affected count. A confirmed second request sets `resetInvalidRelations=true`.
- Replace the page's current inline notices and `window.confirm` with `SystemNotification` success, warning, error, and confirmation components.

## Employee UI

In the employee dialog:

- Show each position's hierarchy level and formatted salary range in the selector.
- Before saving a changed department or position, call the employee impact endpoint.
- If affected relationships exist, show `SystemNotification.confirm` with the count.
- Send `resetInvalidRelations=true` only after confirmation.
- Refresh employee/subordinate data after a confirmed update.

The subordinate selector should show only active employees in the same department whose position level is strictly lower than the selected manager's level. Backend validation remains authoritative.

## Redis Cache Boundary

Cache position detail reads under a registered `POSITION_DETAIL` cache using the existing catalog TTL and resilient database fallback.

- `getById`: `@Cacheable`.
- successful update: `@CachePut` with the complete updated response.
- delete: `@CacheEvict`.

Hierarchy validation and impact calculation always read current database state inside their transaction and never use cached position data for decisions.

## Error Handling

- Field validation errors use HTTP 400.
- Duplicate position code uses HTTP 409 on creation.
- Missing or inactive department uses the existing business error behavior.
- An update requiring relationship removal but lacking confirmation uses HTTP 409 with a stable hierarchy-confirmation error code and impact counts.
- Unauthorized scope and permission failures retain the existing 403 behavior.
- UI feedback and confirmations use `SystemNotification`; browser-native dialogs are not used.

## Testing

Backend tests cover:

- positive hierarchy level and valid salary range;
- negative salaries, zero/negative level, and maximum salary below minimum;
- immutable position code on update;
- same-department and strict higher-level assignment;
- rejection of same-level and cross-department assignment;
- cycle and full-time constraints remain effective;
- employee and position impact counts;
- rejection without confirmation and targeted relationship clearing with confirmation;
- deduplication when both ends use the edited position;
- Redis position-detail reads, refresh, eviction, and fallback;
- controller security and structured impact responses.

Frontend tests cover salary formatting, request payloads, impact branching, and confirmation flags. Verification includes targeted lint, production build, Spring context startup, and the complete backend test suite.

## Compatibility and Rollout

Existing positions receive level `1` and a `0–0 VND` range. Existing reporting lines are not removed by the migration. They are validated when edited or when a new subordinate assignment is attempted. This avoids destructive migration-time changes while ensuring all future mutations obey the hierarchy rules.
