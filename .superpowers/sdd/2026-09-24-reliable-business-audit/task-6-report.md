### Task 6 report

Implemented request-level audit persistence with validation, a separate database transaction, duplicate event handling, and manual Kafka acknowledgement after commit. Reconciliation verifies every instance spool for a date, deduplicates event IDs, queries audit rows in batches of 500, republishes missing events, and retains files while hashes are invalid, events are missing, or any outbox delivery is unfinished. A complete date is deleted only on a later full verification. The scheduler runs at 00:05 Asia/Saigon and revisits retained prior dates.

Verification: `mvn -q -DskipTests compile` passed. `git diff --check` passed. Focused test execution was stopped at the user's budget instruction; no test result is claimed.

Concern: the prior-verification marker is in memory, so a process restart adds one more verification cycle before deletion. This is conservative for retention. A pending outbox row on any date also conservatively delays deletion of all verified spool files.
