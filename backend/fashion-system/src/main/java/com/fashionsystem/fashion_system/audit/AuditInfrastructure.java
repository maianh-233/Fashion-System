package com.fashionsystem.fashion_system.audit;

import java.lang.annotation.*;

/** Explicit classification for mutations excluded from business history. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditInfrastructure {
    String reason();
}
