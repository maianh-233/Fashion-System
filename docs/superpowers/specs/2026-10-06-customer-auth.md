# Customer Auth Design

## Scope

Complete customer password and Google authentication by reusing the existing auth and password-reset APIs. Do not change admin authentication behavior. Frontend changes are limited to `CustomerLogin.jsx` and `CustomerRegister.jsx`.

## Backend design

- Reuse `/api/auth/register/customer`, `/api/auth/login/customer`, and `/api/auth/login/customer/social`.
- Persist customer failed-login state with `failed_login_attempts` and `login_locked_until` on `customers`.
- Lock the customer row while checking a password. Five failed attempts create a 60-second temporary lock. Expired locks and successful logins clear the counters.
- Reuse the existing customer-aware password-reset endpoints. Customer OTPs expire after one minute, are single-use, and allow at most five failed verifications.
- Reject resend within 60 seconds for an existing account with HTTP 429 and `Retry-After`; preserve the generic response for unknown email addresses.
- Resetting a customer password also clears temporary login-lock state.

## Frontend design

- Login uses username/password, displays API errors and the remaining lock duration, and stores the returned customer access token in session storage.
- Forgot password is an accessible modal embedded in the login page with send, verify, and reset steps.
- Registration submits only the backend-supported fields: username, full name, email, and password. Duplicate email/username errors are displayed inline.
- Google Identity Services supplies an ID token to the existing social customer endpoint for both login and registration.

## Security

- Raw OTPs and passwords are never logged or persisted.
- Password and OTP checks remain BCrypt-backed; reset tokens remain SHA-256 hashed.
- The database-backed customer lock prevents concurrent requests from bypassing the failed-attempt threshold.

