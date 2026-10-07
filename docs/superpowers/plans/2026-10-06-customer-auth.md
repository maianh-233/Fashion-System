# Customer Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete secure customer password, Google, registration, and forgot-password flows across the existing backend and two customer auth pages.

**Architecture:** Extend the existing database-backed auth services instead of adding controllers. Store customer login throttling state on the customer row, strengthen the existing password-reset lifecycle, and consume those APIs directly from the two allowed React pages.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, PostgreSQL/Flyway, JUnit 5/Mockito, React 19, Vite, ESLint, Google Identity Services.

**Spec:** `docs/superpowers/specs/2026-10-06-customer-auth.md`

## Global Constraints

- Do not use multi-agent execution.
- Reuse existing controllers and services; do not change admin auth behavior.
- Frontend product-code changes are limited to `CustomerLogin.jsx` and `CustomerRegister.jsx`.
- OTP validity and temporary login lock are exactly 60 seconds.
- Never log an OTP or password.

## Review Focus

- Concurrent wrong-password requests must not bypass the fifth-attempt lock.
- An expired customer lock must clear automatically before a correct login.
- Unknown reset emails must retain a generic response without account enumeration.
- OTP resend before 60 seconds must return a useful `Retry-After` value.
- Google configuration absence and invalid tokens must surface as readable UI errors.

---

### Task 1: Customer Login Lock

**Files:**
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/AuthServiceLoginLockTest.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/Customer.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuthService.java`
- Create: `backend/fashion-system/src/main/resources/db/migration/V24__customer_auth_security.sql`

**Interfaces:**
- Produces: `CustomerRepository.findByUsernameForUpdate(String)` and persisted customer failed-login fields.

- [ ] Add failing tests for fifth failure, active lock, expired lock, and successful reset.
- [ ] Run `mvn -q -Dtest=AuthServiceLoginLockTest test` and confirm failures are caused by missing customer behavior.
- [ ] Add the entity fields, repository lock query, migration, and minimal customer login logic.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Customer OTP Lifecycle

**Files:**
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/PasswordResetServiceTest.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/PasswordResetProperties.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/PasswordResetOtpRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PasswordResetService.java`
- Modify: `backend/fashion-system/src/main/resources/application.yaml`

**Interfaces:**
- Produces: one-minute OTP expiry and `BusinessException` 429 resend cooldown with `Retry-After`.

- [ ] Add failing tests for one-minute expiry, resend cooldown, unknown email, max attempts, single use, and clearing customer login locks on reset.
- [ ] Run `mvn -q -Dtest=PasswordResetServiceTest test` and confirm expected failures.
- [ ] Implement the minimal property, query, service, and configuration changes.
- [ ] Re-run the focused test and confirm it passes.

### Task 3: Customer Login UI

**Files:**
- Modify: `frontend/react-app/src/pages/customer/CustomerLogin.jsx`

**Interfaces:**
- Consumes: existing auth API functions and customer backend error contracts.
- Produces: password/Google login and modal password-reset flow.

- [ ] Replace demo submission with controlled username/password API login and readable error handling.
- [ ] Add Google Identity Services loading and social API submission.
- [ ] Add the three-step forgot-password modal with resend countdown and password validation.
- [ ] Run ESLint against the file and resolve errors.

### Task 4: Customer Registration UI

**Files:**
- Modify: `frontend/react-app/src/pages/customer/CustomerRegister.jsx`

**Interfaces:**
- Consumes: existing register and social customer endpoints.
- Produces: controlled backend-aligned registration form and Google registration.

- [ ] Replace unsupported profile fields with username and confirmation password.
- [ ] Submit the exact register contract and display duplicate/validation errors.
- [ ] Add Google Identity Services registration through the existing social endpoint.
- [ ] Run ESLint against the file and resolve errors.

### Task 5: Verification

**Files:**
- Review: all changed files.

- [ ] Run backend focused tests, full tests, and clean compile.
- [ ] Run frontend ESLint and production build.
- [ ] Inspect `git diff --check`, `git status`, and the final diff for scope and regressions.
