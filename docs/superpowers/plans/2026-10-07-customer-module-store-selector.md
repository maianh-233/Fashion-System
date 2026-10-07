# Customer Module and Shared Store Selector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Store-scoped Customer management, independently persisted Customer authentication, safe website-account linking and a shared Store selector used by Customer, Purchase Receipt and Stock Export.

**Architecture:** Keep `customers` as the business identity and move Customer login into a one-to-one `customer_accounts` domain with separate refresh-token and password-reset persistence. Employee/Admin auth retains its current endpoints and User-backed services. Customer APIs combine existing RBAC with trusted Global/Store identity scope, while React uses a shared Store selector and server-driven Customer data.

**Tech Stack:** Java 21, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL migrations, Redis/Spring Cache, JUnit 5/Mockito, React 19, Vite and ESLint.

**Spec:** `docs/superpowers/specs/2026-10-07-customer-module-store-selector-design.md`

## Global Constraints

- Use one agent only; do not delegate or run multi-agent review.
- Customer auth business logic and persistence must not be shared with Employee/Admin auth.
- Employee/Admin login, refresh, logout and password-reset public contracts remain unchanged.
- JWT signing, password encoding, email sending and pure validation helpers may be reused.
- Preserve and adapt existing uncommitted Customer-auth work; do not overwrite unrelated user changes.
- Backend authorization is authoritative; frontend visibility is UX only.
- Database is the source of truth; Redis is optional cache infrastructure.
- Do not implement loyalty points, wallet, vouchers, marketing automation or a new notification subsystem.
- Use TDD for behavioral changes and run focused verification after each task.

## Review Focus

- Phone and verified email resolving to different Customers must fail atomically without an orphan account.
- A Customer refresh cookie must never rotate or revoke an Employee/Admin session, including equal UUID values.
- Store staff submitting another Store ID must receive forbidden and must not infer cross-store Customer detail.
- Linking a Store Member to website auth must preserve `source=STORE`, origin Store and existing Store address editability.
- Refresh-token replay, concurrent registration and phone-format aliases must remain safe under database concurrency.

---

### Task 1: Customer schema and migration invariants

**Files:**
- Modify: `backend/fashion-system/src/main/resources/db/migration/V24__customer_auth_security.sql`
- Create: `backend/fashion-system/src/main/resources/db/migration/V25__customer_domain_and_auth_isolation.sql`
- Create: `backend/fashion-system/scripts/verify_customer_migration.py`

**Interfaces:**
- Produces: `customer_accounts`, `customer_refresh_tokens`, `customer_password_reset_challenges`, Customer business columns, valid Customer foreign keys, address management source and seeded `REGULAR` tier.

- [ ] Add an idempotency guard to the existing V24 check constraint without removing its Customer lock columns before V25 backfill.
- [ ] Write a PostgreSQL verification script that applies V24/V25 to a disposable schema containing legacy Store/Website Customer fixtures and asserts data preservation, Customer-only backfill, tier/address/token constraints and zero changes to `users`/internal `user_tokens`.
- [ ] Run the script and verify it fails because V25 does not exist.
- [ ] Implement V25 with preflight ambiguity checks, Customer-only account backfill, business fields/indexes, tier seeding/assignment, address repair and Customer-only auth tables.
- [ ] Re-run the migration script twice against clean disposable schemas and expect all assertions to pass.

### Task 2: Customer business entities, normalization and repositories

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/Customer.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/CustomerAccount.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/CustomerSource.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/CustomerMembershipStatus.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/CustomerAddressSource.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/CustomerAddress.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerAccountRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerRepository.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/util/PhoneNormalizer.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/util/PhoneNormalizerTest.java`

**Interfaces:**
- Produces: `PhoneNormalizer.normalize(String)`, business-only `Customer`, account-only pessimistic lookup methods and Customer query primitives keyed by normalized phone/email/store/source.

- [ ] Write parameterized tests proving `0901234567`, `84901234567` and `+84901234567` normalize identically and malformed/internationally unsupported values are rejected.
- [ ] Run `mvn -q -Dtest=PhoneNormalizerTest test` and confirm failure from the missing utility.
- [ ] Implement the normalizer and Customer/account enums/entities without Employee/Admin dependencies.
- [ ] Refactor repository methods so username/login-email queries live only in `CustomerAccountRepository`, while Customer search/link queries use business fields.
- [ ] Re-run the focused test and `mvn -q -DskipTests compile`; expect success.

### Task 3: Customer account registration and website linking

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/auth/RegisterCustomerRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerAccountLinkService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerCodeGenerator.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerTierRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerTierAssignmentRepository.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CustomerAccountLinkServiceTest.java`

**Interfaces:**
- Consumes: `PhoneNormalizer`, `CustomerRepository`, `CustomerAccountRepository`.
- Produces: transactional `CustomerAccountLinkService.register(RegisterCustomerRequest)` and `linkVerifiedSocial(SocialProfile)` returning the linked Customer/CustomerAccount pair.

- [ ] Write failing tests for a new Website Customer, existing Store Member phone link, verified-email link, existing account conflict, phone/email split conflict, concurrent unique conflict and preservation of Store source/origin.
- [ ] Run the focused test and confirm failures come from missing registration service behavior.
- [ ] Add required phone validation to the Customer registration request and implement sequence-backed Customer codes plus REGULAR assignment.
- [ ] Implement transactional matching/linking and translate uniqueness violations to HTTP conflict without leaving orphan rows.
- [ ] Re-run the focused test and expect all cases to pass.

### Task 4: Independent Customer password/social auth

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CustomerAuthController.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerAuthService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuthService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/AuthController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/JwtService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/mapper/AuthResponseMapper.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/security/JwtAuthenticationFilter.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerSocialAccountService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CustomerAuthServiceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/CustomerAuthControllerTest.java`

**Interfaces:**
- Consumes: `CustomerAccountLinkService`, technical `PasswordEncoder`, `JwtService`, `SocialIdentityVerifier`.
- Produces: Customer-owned register/password login/Google login endpoints with unchanged login/register paths and `accountType=CUSTOMER` access tokens.

- [ ] Write failing service tests for correct/wrong password, fifth-attempt lock, expired lock, inactive Customer, existing/new Google identity and repeated social login without duplication.
- [ ] Write controller tests showing Customer endpoints call only Customer services and internal login endpoints retain their existing request/response contracts.
- [ ] Run the focused tests and confirm they fail before extraction.
- [ ] Implement `CustomerAuthService`, move Customer methods out of `AuthService`, and move Customer endpoint ownership out of `AuthController` without changing Employee/Admin methods.
- [ ] Adapt JWT/filter/response mapping to load Customer Accounts while preserving `accountType=CUSTOMER`, `customerId` and the current Employee/Admin claims.
- [ ] Re-run focused Customer auth and existing internal login-lock tests; expect success.

### Task 5: Independent Customer refresh and logout

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/CustomerRefreshToken.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerRefreshTokenRepository.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerRefreshTokenService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/CustomerRefreshTokenProperties.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/CustomerRefreshTokenCookieService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/UserToken.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserTokenRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/RefreshTokenService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/AuthController.java`
- Modify: `backend/fashion-system/src/main/resources/application.yaml`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CustomerRefreshTokenServiceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/AuthRefreshIsolationTest.java`

**Interfaces:**
- Produces: `/api/auth/customer/refresh`, `/api/auth/customer/logout`, distinct HttpOnly Customer cookie and Customer-only rotate/revoke persistence; internal `RefreshTokenService` becomes User-only.

- [ ] Write failing tests for issue/rotate/replay/logout, disabled Customer, distinct cookie name/path and equal Customer/User UUID isolation.
- [ ] Write regression tests pinning existing `/refresh` and `/logout` Employee/Admin behavior.
- [ ] Run focused tests and confirm Customer isolation is missing.
- [ ] Implement Customer refresh entity/repository/service/cookie and endpoints; remove Customer branches from internal `RefreshTokenService` only after the new path is covered.
- [ ] Re-run focused tests and verify both Customer isolation and unchanged internal contracts.

### Task 6: Independent Customer forgot/reset password

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/CustomerPasswordResetChallenge.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerPasswordResetChallengeRepository.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerPasswordResetService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CustomerPasswordResetController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/PasswordResetOtp.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/PasswordResetOtpRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/auth/PasswordResetOtpRequest.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/auth/VerifyPasswordResetOtpRequest.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PasswordResetService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/PasswordResetController.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CustomerPasswordResetServiceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/PasswordResetIsolationTest.java`

**Interfaces:**
- Produces: Customer-only `/api/auth/customer/password/*` flow; internal `/api/auth/password/*` becomes Employee/Admin-only with its existing contract.

- [ ] Write failing tests for generic unknown-email response, cooldown, expiry, maximum attempts, one-time token, password update, lock clear and Customer-session revocation.
- [ ] Add regression tests proving internal reset still reads/writes only User-backed persistence.
- [ ] Run focused tests and confirm isolation is absent.
- [ ] Implement Customer reset service/controller using the shared mail sender and encoder only; remove Customer branching from internal reset service/request handling.
- [ ] Re-run focused Customer and internal reset tests; expect success.

### Task 7: Store-scoped Customer management backend

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerAccessService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CustomerController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerRepository.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/customer/CreateStoreCustomerRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/customer/UpdateCustomerRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/customer/CustomerListResponse.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/customer/CustomerDetailResponse.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/customer/CustomerLookupResponse.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/CacheNames.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CustomerServiceScopeTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/CustomerControllerSecurityTest.java`

**Interfaces:**
- Produces: paged list/filter, detail, Store Member create/update, exact lookup, status, tier and a Customer-permission-aware Store-options endpoint with persisted Store scope.

- [ ] Write failing tests for Global filters, Store-forced list, cross-store IDOR, Website lookup/read-only behavior, server-assigned source/origin/tier and DTO over-posting rejection.
- [ ] Add cache tests proving create/update/link/status and old/new phone changes cannot return stale lookup results.
- [ ] Run focused tests and confirm current endpoints are under-scoped.
- [ ] Implement `CustomerAccessService`, request/response DTOs, Customer Store options, repository projections/specifications and Customer service operations with `@BusinessAudit("CUSTOMER")`.
- [ ] Add exact-phone Redis cache through existing Spring Cache and conservative namespace eviction on identity mutations.
- [ ] Re-run focused tests and expect success.

### Task 8: Address ownership and tier enforcement

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CustomerAddressController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerAddressService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/CustomerAddressDto.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CustomerTierAssignmentController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerTierAssignmentService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CustomerAddressAuthorizationTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CustomerTierAuthorizationTest.java`

**Interfaces:**
- Consumes: `CustomerAccessService`.
- Produces: staff-readable WEB addresses, staff-editable same-Store STORE addresses and permission-gated tier changes.

- [ ] Write failing tests for cross-store address access, WEB-address mutation rejection, linked Store Member STORE-address edit and Store staff tier rejection.
- [ ] Run focused tests and confirm current ownership shortcut is over-permissive.
- [ ] Replace employee-wide ownership checks with actor-aware service authorization and immutable address source.
- [ ] Gate tier mutations with `CUSTOMER_TIER_MANAGE` plus scope, leaving current tier/history reads available under view permission.
- [ ] Re-run focused tests and expect success.

### Task 9: Shared Store selector and warehouse migration

**Files:**
- Create: `frontend/react-app/src/components/common/StoreSelector.jsx`
- Create: `frontend/react-app/src/hooks/useStoreOptions.js`
- Create: `frontend/react-app/src/utils/storeSelection.js`
- Create: `frontend/react-app/src/utils/storeSelection.test.js`
- Modify: `frontend/react-app/src/pages/admin/WarehouseManagement.jsx`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/WarehouseLookupController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StoreAccessService.java`

**Interfaces:**
- Produces: controlled Store selector supporting required/optional/ALL/NO_STORE/readonly modes and shared endpoint-configured store-option loading; Warehouse passes only real Store UUIDs to existing receipt APIs.

- [ ] Write failing Node tests for ALL omission, NO_STORE translation, UUID translation, Store-scope coercion and required-value validation.
- [ ] Run the test and confirm the selection utility is missing.
- [ ] Implement selection constants/translation, an endpoint-configured shared option hook and accessible Store UI component; Warehouse uses `/api/inventory/stores` and Customer uses its Customer-permission-aware endpoint.
- [ ] Replace Warehouse local Store fetch/select while preserving current default Store, tabs, paging and request parameters for both import and export.
- [ ] Run the selection test, targeted ESLint and frontend build; expect success.

### Task 10: Customer API client and management UI

**Files:**
- Create: `frontend/react-app/src/api/customerManagementApi.js`
- Modify: `frontend/react-app/src/pages/admin/CustomerManagement.jsx`
- Modify: `frontend/react-app/src/components/admin/CustomerDialog.jsx`
- Modify: `frontend/react-app/src/components/admin/CustomerForm.jsx`
- Modify: `frontend/react-app/src/components/admin/CustomerView.jsx`
- Modify: `frontend/react-app/src/components/admin/CustomerAddress.jsx`
- Create: `frontend/react-app/src/pages/admin/customerManagementLogic.js`
- Create: `frontend/react-app/src/pages/admin/customerManagementLogic.test.js`

**Interfaces:**
- Consumes: Customer paged APIs, permissions context and `StoreSelector`.
- Produces: real `/admin/customers` list/search/filter/create/detail/update UX with debounced server requests and permission-aware controls.

- [ ] Write failing logic tests for debounced query parameters, ALL/NO_STORE mapping, Store-scope coercion and immutable field stripping.
- [ ] Run tests and confirm current mock page cannot satisfy them.
- [ ] Implement the API module and compact server-driven list/filter/pagination UI.
- [ ] Implement create/edit/detail/address presentation; WEB addresses, origin Store, Customer Account and unauthorized tier controls remain readonly.
- [ ] Re-run logic tests, targeted ESLint and production build; expect success.

### Task 11: Customer storefront auth client migration

**Files:**
- Modify: `frontend/react-app/src/api/auth/authApi.js`
- Modify: `frontend/react-app/src/api/auth/index.js`
- Modify: `frontend/react-app/src/pages/customer/CustomerLogin.jsx`
- Modify: `frontend/react-app/src/pages/customer/CustomerRegister.jsx`
- Create: `frontend/react-app/src/api/auth/customerSession.js`
- Test: `frontend/react-app/src/api/auth/customerSession.test.js`

**Interfaces:**
- Produces: Customer-only refresh/logout/password-reset calls and required-phone registration; internal `adminSession.js` remains on existing internal endpoints.

- [ ] Write failing tests that assert Customer uses `/customer/refresh`, `/customer/logout`, `/customer/password/*` while admin continues using `/refresh` and `/logout`.
- [ ] Run the tests and confirm current shared calls fail isolation expectations.
- [ ] Split Customer API/session functions, update register phone input and route forgot-password calls to Customer endpoints without changing admin imports.
- [ ] Re-run auth client tests, targeted ESLint and build; expect success.

### Task 12: Auth regression, full verification and documentation

**Files:**
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/auth/AuthDomainRegressionTest.java`
- Create: `docs/customer-module.md`
- Review: all files changed by Tasks 1-11.

**Interfaces:**
- Consumes: completed backend/frontend features.
- Produces: evidence that Customer and internal auth domains are isolated and deployment documentation is complete.

- [ ] Add end-to-end MockMvc regression cases for Customer register/login/refresh/logout/reset/social and Employee/Admin login/refresh/logout/reset using their separate tables/cookies.
- [ ] Run focused Customer, Employee/Admin, authorization and warehouse suites; record any pre-existing unrelated failure separately.
- [ ] Run `mvn -q -DskipTests clean compile`, then the full backend test suite.
- [ ] Run frontend logic tests, `npm run lint` and `npm run build`.
- [ ] Run migration verification, Hibernate validation, `git diff --check` and inspect the final diff for secrets, auth-domain branching, IDOR, N+1 queries and unrelated edits.
- [ ] Document Customer types, ownership, linking, auth isolation, APIs, Redis eviction, audit behavior, StoreSelector usage, notification limitation and future loyalty integration.
- [ ] Produce the required 22-point completion report with exact commands, results and known limitations.
