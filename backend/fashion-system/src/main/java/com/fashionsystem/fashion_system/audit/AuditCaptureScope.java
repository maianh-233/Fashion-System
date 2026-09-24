package com.fashionsystem.fashion_system.audit;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Binds a collector to the EntityManager resource of the current transaction. */
public final class AuditCaptureScope implements AutoCloseable {
    private static final ThreadLocal<State> STATE = new ThreadLocal<>();
    private final EntityManagerHolder holder;
    private boolean closed;

    private AuditCaptureScope(EntityManagerHolder holder) { this.holder = holder; }

    public static AuditCaptureScope open(AuditChangeCollector collector) {
        Objects.requireNonNull(collector, "collector");
        EntityManagerHolder holder = currentHolder().orElseThrow(
                () -> new IllegalStateException("Audit capture requires a bound EntityManagerHolder"));
        State state = STATE.get();
        if (state == null) {
            state = new State();
            STATE.set(state);
        }
        AuditCaptureScope scope = new AuditCaptureScope(holder);
        state.stack.push(new Binding(scope, state.collectors.put(holder, collector)));
        return scope;
    }

    public static Optional<AuditChangeCollector> current() {
        State state = STATE.get();
        return state == null ? Optional.empty()
                : currentHolder().map(state.collectors::get);
    }

    @Override
    public void close() {
        if (closed) return;
        State state = STATE.get();
        if (state == null || state.stack.isEmpty() || state.stack.peek().scope != this) {
            throw new IllegalStateException("Audit capture scopes must close in reverse order");
        }
        Binding binding = state.stack.pop();
        if (binding.previous == null) state.collectors.remove(holder);
        else state.collectors.put(holder, binding.previous);
        if (state.stack.isEmpty()) STATE.remove();
        closed = true;
    }

    private static Optional<EntityManagerHolder> currentHolder() {
        for (Map.Entry<Object, Object> resource : TransactionSynchronizationManager.getResourceMap().entrySet()) {
            if (resource.getKey() instanceof EntityManagerFactory
                    && resource.getValue() instanceof EntityManagerHolder holder) return Optional.of(holder);
        }
        return Optional.empty();
    }

    private static final class State {
        private final IdentityHashMap<EntityManagerHolder, AuditChangeCollector> collectors = new IdentityHashMap<>();
        private final Deque<Binding> stack = new ArrayDeque<>();
    }

    private record Binding(AuditCaptureScope scope, AuditChangeCollector previous) {}
}
