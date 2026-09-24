package com.fashionsystem.fashion_system.audit;

/** A row's identity within one transaction's change set. */
public record AuditChangeKey(String table, String rowId) {}
