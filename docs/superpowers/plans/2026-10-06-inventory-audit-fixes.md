# Inventory Audit Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct verified PostgreSQL filtering, date-boundary, damaged-stock, and reservation-race defects in the Fashion System warehouse module.

**Architecture:** Keep the existing receipt workflow and pessimistic stock locking. Replace nullable-parameter predicates with non-null boolean guards while preserving repository entry points, normalize receipt filters from `LocalDate` to half-open `LocalDateTime` ranges, update damaged stock atomically with available stock, and enforce one active reservation at the database boundary.

**Tech Stack:** Java 21, Spring Boot/Data JPA, PostgreSQL 16, JUnit 5/Mockito, React 19, Node test runner.

**Spec:** User request in this chat dated 2026-10-06 (no repository spec file).

## Global Constraints

- Do not use multi-agent execution.
- Fix defects when supported by concrete code/test evidence; do not only report them.
- Keep changes inside the warehouse/inventory module unless a database migration is required.
- Preserve transactional stock updates and reject negative or overflowing quantities.
- Verify with focused tests, compile/build, and a final diff review.

## Review Focus

- PostgreSQL must execute every optional warehouse filter when UUID/date parameters are null.
- An end date must include the entire selected calendar day without including the following day.
- DAMAGED issues must decrement the chosen available channel and increment damaged stock atomically.
- Concurrent active reservations for the same order/store/variant must not both persist.
- Repeated completion must remain protected by receipt locks and status transitions.

---

### Task 1: PostgreSQL-safe filters and receipt date ranges

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/*Warehouse-related repositories.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/Goods*Controller.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/Goods*Service.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/InventoryPostgresTest.java`
- Test: focused receipt service/controller tests

**Interfaces:**
- Consumes: optional UUID/string/date request filters.
- Produces: PostgreSQL-safe repository queries and half-open `[fromDate, toDate + 1 day)` ranges.

- [ ] Add a PostgreSQL regression test that executes all warehouse repository methods with null optional parameters and observe the current failure.
- [ ] Replace `:param is null` predicates with non-null guard parameters while preserving public repository entry points.
- [ ] Bind receipt dates as `LocalDate`, convert them to start-of-day/exclusive-next-day bounds, and reject reversed ranges/invalid enum filters with 400 errors.
- [ ] Run focused repository and receipt tests to green.

### Task 2: Damaged stock and reservation invariants

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/InventoryService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StockReservationService.java`
- Create: `backend/fashion-system/src/main/resources/db/migration/V23__warehouse_inventory_invariants.sql`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/InventoryServiceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/StockReservationServiceTest.java`

**Interfaces:**
- Consumes: DAMAGED goods issues and active reservation creation.
- Produces: atomic damaged counters and database-enforced single active reservation.

- [ ] Add failing tests for damaged increments/overflow atomicity and duplicate-reservation conflict mapping.
- [ ] Update the inventory mutation calculation so all bucket values are checked before mutation.
- [ ] Flush reservation inserts and map the unique-index violation to HTTP 409.
- [ ] Add migration checks/constraints for reservation quantity, status, and active uniqueness.
- [ ] Run focused tests to green.

### Task 3: Frontend date contract

**Files:**
- Modify: `frontend/react-app/src/pages/admin/warehouseLogic.js`
- Modify: `frontend/react-app/src/pages/admin/WarehouseManagement.jsx`
- Test: `frontend/react-app/src/pages/admin/warehouseLogic.test.js`

**Interfaces:**
- Consumes: browser date input values (`YYYY-MM-DD`).
- Produces: date-only API query parameters matching the backend contract.

- [ ] Add a failing helper test for date-only receipt query parameters.
- [ ] Implement the helper and use it in warehouse list requests.
- [ ] Run Node tests and the frontend build.

### Task 4: Final verification and regression review

**Files:**
- Review only: all changed files and warehouse-related call sites.

**Interfaces:**
- Consumes: Tasks 1-3.
- Produces: verified targeted suite, backend compile, frontend build, and a scoped diff.

- [ ] Run all relevant backend tests and PostgreSQL integration tests.
- [ ] Run backend compile and frontend tests/build.
- [ ] Inspect `git diff` for unintended changes and remaining warehouse risks.
