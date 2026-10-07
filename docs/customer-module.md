# Customer module

## Model and customer types

`customers` is the business profile. `customer_accounts` is the optional website login identity and never references the Employee/Admin `users` account. A sale may still have no Customer (anonymous walk-in).

- Store Member: `source=STORE`, immutable `originStoreId`, `membershipStatus=MEMBER`, default tier `REGULAR`, optional web account.
- Website Customer: `source=WEBSITE`, `originStoreId=null`, default tier `REGULAR`, required web account.
- When a Store Member later registers on the website, normalized phone is matched first and verified email second. The new account links to that Customer without changing `source` or `originStoreId`.

Customer codes are generated server-side (`CUS...`). Phone lookup uses the normalized E.164-like Vietnam representation; names are never identity keys.

## Authorization and ownership

Customer management reuses the existing RBAC and trusted `/api/me/scope` identity.

- Global staff may list/filter all stores, website-only Customers and linked-account status.
- Store staff are forced to their authenticated store by the backend. They can view/edit only Store Members whose `originStoreId` matches their store.
- Store staff may lookup a Customer for a sale, but lookup does not grant edit rights.
- `originStoreId`, tier and account linkage are not accepted from normal update requests.
- WEB-managed addresses are staff read-only. STORE-managed addresses require same-store edit permission.
- Tier mutation requires global `CUSTOMER_TIER_MANAGE`.

Important permissions: `CUSTOMER_VIEW`, `CUSTOMER_CREATE`, `CUSTOMER_UPDATE`, `CUSTOMER_STATUS_MANAGE`, `CUSTOMER_TIER_MANAGE`.

## Customer authentication isolation

Customer register, password/social login, login lock, password reset, refresh persistence and logout are owned by Customer services and Customer tables. Employee/Admin retain the existing `AuthService`, `RefreshTokenService`, `PasswordResetService`, endpoints and token contract.

Only technical primitives are shared: password encoder, JWT signing/parsing and mail transport. Customer JWTs contain `accountType=CUSTOMER`; Customer refresh tokens use a distinct cookie and persistence table.

Customer endpoints:

- `POST /api/auth/register/customer`
- `POST /api/auth/login/customer`
- `POST /api/auth/login/customer/social`
- `POST /api/auth/customer/refresh`
- `POST /api/auth/customer/logout`
- `POST /api/auth/customer/password/{request-otp,resend-otp,verify-otp,reset}`

## Management API

- `GET/POST /api/customers`
- `GET/PUT /api/customers/{id}`
- `PATCH /api/customers/{id}/{activate|deactivate}`
- `GET /api/customers/lookup`
- `GET /api/customers/store-options`
- `GET /api/customers/{id}/addresses`

The list uses server-side pagination and supports search, store/no-store, source, tier, web-account and active filters.

## StoreSelector

The shared React `StoreSelector` supports required, optional, All Stores, No Store/Website and read-only current-store modes. Options are loaded through the cached `useStoreOptions` hook. Customer and Warehouse (including Import Receipt and Export Receipt routes) use the same component while retaining their original API parameters.

## Redis, notification and audit

Database remains the source of truth. Redis uses the existing Spring cache infrastructure for normalized-phone lookup only. Customer create/update/status changes and account linking evict that cache.

UI success/failure feedback reuses `SystemNotification`. No backend system-notification event was added because the repository has notification entities but no active delivery service; this avoids creating a second subsystem or notification spam.

Customer management, address, tier, social and account-link mutations use the existing business-audit mechanism. Passwords, JWTs, refresh tokens and OTP secrets are not audit payloads.

## Database and future work

Migrations V25/V26 add the isolated Customer auth schema, business fields/indexes/constraints, address ownership source, `REGULAR` tier and Customer permissions. They do not migrate or modify Employee/Admin account data.

Future loyalty work can consume tier assignments, but points, wallets, campaigns and voucher automation are intentionally out of scope.
