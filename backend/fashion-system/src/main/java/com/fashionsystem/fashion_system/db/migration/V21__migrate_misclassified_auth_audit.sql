-- Copy historical business evidence; keep original authentication rows intact.
-- Legacy rows have no reliable row IDs/snapshots: row_count is unknown and changes empty.
INSERT INTO audit_logs (id, event_id, actor_type, actor_user_id, username, action,
    request_id, row_count, changes, new_data, ip_address, user_agent, occurred_at, created_at)
SELECT md5('auth-audit-business-row:' || a.id::text)::uuid,
    md5('auth-audit-business-event:' || a.id::text)::uuid,
    CASE WHEN a.user_id IS NULL THEN 'SYSTEM' ELSE 'EMPLOYEE' END,
    a.user_id, 'legacy-auth', a.action,
    'legacy-auth:' || a.id::text, NULL, '[]'::jsonb,
    jsonb_build_object('migratedFromAuthAudit', true, 'authAuditLogId', a.id::text,
        'description', a.description),
    a.ip_address, a.user_agent,
    COALESCE(a.created_at, CURRENT_TIMESTAMP AT TIME ZONE 'UTC') AT TIME ZONE 'UTC',
    COALESCE(a.created_at, CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
FROM auth_audit_logs a
WHERE a.action IN ('EMPLOYEE_CREATED', 'EMPLOYEE_UPDATED', 'SUBORDINATE_ASSIGNED', 'SUBORDINATE_REMOVED')
ON CONFLICT (event_id) DO NOTHING;
