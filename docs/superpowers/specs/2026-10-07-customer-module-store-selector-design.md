# Customer Module and Shared Store Selector Design

## Status

Approved direction as of 2026-10-07. This document supersedes the Customer-auth architecture in
`docs/superpowers/specs/2026-10-06-customer-auth.md` where the two documents overlap. Existing
uncommitted Customer-auth work must be preserved and adapted, not discarded.

## Goals

- Build a production Customer-management module with server-side search, filters, pagination,
  store ownership, membership source, tier and website-account status.
- Keep anonymous walk-in checkout valid: an Order/Sale may have no Customer.
- Link a Store Member to a website account without creating another Customer or changing the
  Customer's original source/store.
- Isolate Customer authentication business logic and persistence from Employee/Admin authentication.
- Provide a reusable Store selector and migrate Customer, Purchase Receipt and Stock Export to it.
- Reuse the current permission, Redis cache, audit and UI-notification infrastructure without
  creating parallel subsystems.

## Non-goals

- Loyalty points, wallet, vouchers, marketing automation, campaigns or full CRM.
- Migrating Employee/Admin accounts into `customer_accounts`.
- Changing Employee/Admin login, refresh, logout, password-reset contracts or token behavior.
- Making Redis a source of truth.
- Building a new system-notification subsystem when the current project has no application service
  that publishes notifications.

## Current-state findings

- `Customer` currently contains both business-profile and login fields.
- Customer endpoints do not enforce RBAC plus Global/Store data scope.
- Customer Address authorizes any Employee and cannot distinguish website-managed addresses.
- `/admin/customers` is backed by mock data.
- Purchase Receipt and Stock Export share `WarehouseManagement`, whose Store select and Store load
  are currently local to that page.
- JWT already distinguishes `accountType=USER` and `accountType=CUSTOMER`.
- Customer and internal refresh tokens currently share `user_tokens`; the live schema requires
  `user_id`, making the Customer branch invalid and coupling both account domains.
- Password reset currently uses one polymorphic service/table for Employee and Customer.
- Google verification already rejects tokens whose email is not verified.
- Redis Cache, `@BusinessAudit`, audit outbox and Kafka delivery already exist.
- Notification entities exist but there is no usable notification-publishing application service.
- `orders.customer_id` is nullable, so anonymous walk-in remains supported without an Order change.

## Authentication boundary

Customer and Employee/Admin are separate authentication domains.

### Customer-only components

- `CustomerAccount`, `CustomerAccountRepository`
- `CustomerAuthController`, `CustomerAuthService`
- `CustomerRefreshToken`, `CustomerRefreshTokenRepository`, `CustomerRefreshTokenService`
- `CustomerPasswordResetChallenge`, its repository and `CustomerPasswordResetService`
- Customer login failure/lock handling
- Customer Google login/register and social-account linking
- Customer refresh cookie configuration/service

### Employee/Admin components

After extraction, the existing `AuthController`, `AuthService`, `RefreshTokenService`,
`PasswordResetController` and `PasswordResetService` serve only internal User accounts. Their public
paths and behavior remain unchanged:

- `POST /api/auth/login`
- `POST /api/auth/login/employee`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- existing Employee/Admin registration and password-reset endpoints

### Shared technical infrastructure

Only stateless technical utilities may be shared:

- JWT signing/parsing in `JwtService`
- `PasswordEncoder`
- secure token/hash helpers with no account-domain branching
- email sender
- general validation/normalization helpers
- exception and HTTP response infrastructure

No shared service may branch on CUSTOMER versus EMPLOYEE/ADMIN to implement login, registration,
forgot password, refresh or logout business rules.

## Customer data model

### `customers`

`customers` is the business identity:

- `id UUID`
- `customer_code VARCHAR`, server-generated and unique (`CUS` plus a zero-padded sequence)
- `full_name`, `phone`, `normalized_phone`, contact `email`, birthday, gender and avatar
- `source`: `STORE` or `WEBSITE`
- `membership_status`: initially `MEMBER`
- `origin_store_id NULL REFERENCES stores(id)`
- `active`, timestamps

Rules:

- Store Member: `source=STORE`, non-null `origin_store_id`, current tier `REGULAR`.
- Website Customer: `source=WEBSITE`, null `origin_store_id`, and a Customer Account.
- Linking a Store Member creates a Customer Account but does not change `source` or
  `origin_store_id`.
- A Customer row is not created for anonymous walk-in.

### `customer_accounts`

`customer_accounts` is a Customer-only login identity:

- independent UUID primary key
- unique `customer_id REFERENCES customers(id)`
- unique normalized `username` and `login_email`
- nullable password hash to support social-only accounts
- failed-attempt count and `login_locked_until`
- timestamps

It has no foreign key to `users`, Employee/Admin account tables or roles.

### Customer auth persistence

- `customer_refresh_tokens` stores hashed Customer refresh tokens, family, parent, expiry and
  revocation. It references only `customer_accounts`.
- `customer_password_reset_challenges` stores Customer-only OTP/reset state and references only
  `customer_accounts`.
- `customer_social_accounts` remains Customer-specific, gains a valid Customer foreign key, and
  retains the unique provider/provider-user constraint.
- Internal `user_tokens` and internal password-reset data are no longer read or written by Customer
  services.

### Tier and address

- Seed a `REGULAR` tier if absent.
- Continue using `customer_tier_assignments` as tier history; enforce one unexpired assignment and
  add missing Customer/Tier foreign keys.
- Add address management source `STORE` or `WEB`.
- Staff may edit only `STORE` addresses when they also have Customer edit access.
- Customer website flows own `WEB` addresses; staff may view but cannot mutate them.
- Repair the legacy `customer_addresses.user_id NOT NULL` drift so JPA inserts through
  `customer_id` are valid.

## Migration strategy

1. Create Customer-only account, refresh-token and reset-challenge tables.
2. Backfill one `customer_accounts` row for every existing Customer using only columns from
   `customers`; do not read or mutate Employee/Admin `users`.
3. Move current Customer login-lock values into the new account row.
4. Add business Customer columns, normalize existing phone data and generate missing codes.
5. Backfill `source=WEBSITE` for legacy Customers because all existing rows represented website
   accounts; leave `origin_store_id` null.
6. Seed `REGULAR` and create missing current assignments.
7. Add validated Customer/social/tier/address constraints and indexes after backfill checks.
8. Remove Customer branches and Customer columns from internal token/reset persistence. Internal
   User data is not migrated or rewritten.
9. Remove auth-only columns from `customers` after the account backfill succeeds atomically.

The migration must fail with an explanatory error when pre-existing normalized phones or emails
are ambiguous rather than silently merging Customers.

## Phone and identity matching

- A shared, technical `PhoneNormalizer` converts Vietnamese `0xxxxxxxxx`, `84xxxxxxxxx` and
  `+84xxxxxxxxx` representations to one canonical value.
- Database uniqueness is on `normalized_phone`, not presentation text.
- Website registration requires phone.
- Matching order is existing account relation, normalized phone, then verified email.
- Name is never an identity key.
- If phone and verified email resolve to different Customers, registration fails with conflict and
  creates neither Customer nor account.
- Concurrent registration relies on database unique constraints and maps constraint errors to a
  conflict response.

## Customer auth flows

### Endpoints

Customer endpoints remain under `/api/auth` for compatibility where possible, but are owned by
`CustomerAuthController`:

- existing `POST /register/customer`
- existing `POST /login/customer`
- existing `POST /login/customer/social`
- new `POST /customer/refresh`
- new `POST /customer/logout`
- new Customer-only password paths under `/customer/password/*`

The frontend moves Customer refresh/logout/password-reset calls to the Customer-only paths. Internal
frontend APIs continue using `/refresh`, `/logout` and `/password/*` unchanged.

### Registration

Within one transaction:

1. Validate and normalize username, email and phone.
2. Reject an existing Customer Account username/email.
3. Resolve an existing Customer using normalized phone, then verified email rules.
4. Reject conflicting identities or an already linked Customer.
5. Create a Website Customer only when no safe match exists.
6. Create the Customer Account and assign `REGULAR` when the Customer has no current tier.
7. Commit before issuing Customer access/refresh tokens.

### Password login and lock

`CustomerAuthService` locks the Customer Account row, validates Customer active status and account
lock, increments failures, applies the configured temporary lock, and clears the counter after a
successful login. No internal User repository is involved.

### Google login/register

- Verify issuer, signature, audience, expiry and `email_verified` through the existing technical
  verifier.
- Reuse an existing provider/subject link first.
- Otherwise link an existing Customer Account with the same login email, or safely link an unlinked
  Customer with the verified contact email.
- If no match exists, create a Website Customer plus social-only Customer Account.
- Never create a Customer on every login and never merge on unverified data or name.

### Customer refresh and logout

- Customer refresh tokens are persisted only in `customer_refresh_tokens`.
- Customer cookie uses a distinct name and `/api/auth/customer` path.
- Refresh rotation and family replay revocation are Customer-only.
- Customer logout revokes only the matching Customer family and revokes the Customer access token
  through the existing technical JWT-revocation mechanism.
- Internal refresh cookies/sessions cannot be read, rotated or revoked by Customer endpoints, and
  the inverse is also true.

### Customer forgot/reset password

- Customer-only request, verify and reset methods use `customer_password_reset_challenges`.
- Email enumeration protection, resend cooldown, maximum attempts, one-time reset token and password
  hashing remain required.
- Successful reset clears Customer Account login lock and revokes all Customer refresh sessions.
- Employee/Admin reset behavior and persistence remain unchanged.

## Customer management API and authorization

- List supports page, size, sort, server-side search, store, source, tier, web-account and status.
- Search covers customer code, name, normalized phone and email.
- Create Staff Customer accepts business fields only; backend sets `source=STORE`, account absent and
  tier `REGULAR`.
- Update DTO cannot carry origin store, source, tier or account identifiers.
- Tier changes use a separate permission-protected operation.
- Exact lookup returns a limited response for sales attachment and does not grant edit access.

Access combines current RBAC permission with `UserScopeService`:

- Global identity with applicable permission can list/filter all Customers.
- Store identity is forced to its persisted Store regardless of submitted filters.
- Store staff can create/edit Store Members from their Store only.
- Store staff cannot edit other-store or Website Customers, origin store, tier, credentials or WEB
  addresses.
- Detail/update/address operations load persisted ownership before authorization to prevent IDOR.

Current Customer permission codes are reused; no new permission framework is introduced.
Customer and Warehouse expose separate permission-aware Store-option endpoints. The shared frontend
hook accepts the endpoint as configuration, so Customer access never depends on warehouse
permissions and warehouse access never depends on Customer permissions.

## Shared Store selector

Create a presentation component and shared store-options hook. The component accepts controlled
`value`/`onChange` plus configuration for required, optional, All Stores, No Store/Website and
readonly current Store modes.

- UI-only constants are string values `ALL` and `NO_STORE`, never numeric magic values.
- API adapters translate `ALL` to an omitted Store filter, `NO_STORE` to an explicit no-origin
  query, and a UUID to a specific Store filter.
- Global users see permitted active Stores; Store users see a disabled current Store.
- Business APIs still enforce scope; the selector is not a security boundary.
- `WarehouseManagement` adopts the shared selector, thereby covering Purchase Receipt and Stock
  Export without changing their store defaults, pagination or receipt APIs.
- Customer filters adopt All/No Store/specific Store modes.

## Frontend Customer module

- Replace mock data with Customer API calls.
- Use debounced server search and server pagination.
- Filters: Store, source, tier, web-account status and active status.
- Table: Customer/code, phone, email, source, origin Store, tier, web account, status and actions.
- Add form requires full name and phone; Global staff chooses Store and Store staff use current Store.
- Detail drawer separates Customer, membership, contact, origin Store, web account, addresses and
  system data.
- Controls reflect permissions for UX, while backend remains authoritative.
- Use the existing toast/notification component for user feedback.

## Redis, audit and notification

- Add a short-lived exact-phone lookup cache using the existing Spring Cache/Redis configuration.
- Database remains authoritative.
- Create, phone/email update, account link and status mutation evict Customer lookup entries. Phone
  changes invalidate both old and new values by evicting the small lookup cache namespace.
- Reuse `@BusinessAudit("CUSTOMER")` for create, update, account link, status and tier changes.
- Passwords, OTPs and access/refresh tokens are never audit payloads.
- Do not add system notifications until a project-wide publisher exists; use toast for CRUD and audit
  `CUSTOMER_ACCOUNT_LINKED` as the durable business event.

## Verification

- Migration test against PostgreSQL plus Hibernate validation.
- Unit/integration tests for Store/Website creation, safe linking, conflicting identity, scope/IDOR,
  address source, tier default and phone normalization.
- Customer auth tests for password/social login, lock, register, refresh rotation/replay, logout and
  forgot/reset password.
- Explicit Employee/Admin login, refresh, logout and password-reset regression tests using unchanged
  internal endpoints.
- Store selector tests for Global All/specific/No Store and Store readonly behavior.
- Purchase Receipt and Stock Export regression tests/build checks.
- Backend focused tests, full test suite and clean compile.
- Frontend lint and production build.
