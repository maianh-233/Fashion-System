package com.fashionsystem.fashion_system.task;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.service.StoreAccessService;
import com.fashionsystem.fashion_system.service.UserScope;
import com.fashionsystem.fashion_system.service.UserScopeService;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.Actions;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.Person;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.Task;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Toàn bộ luật ai giao / xem / sửa được công việc. Người có TASK_ASSIGN giao trong phạm vi của mình (Global: mọi nhân viên,
 * cửa hàng: nhân viên cùng cửa hàng); quản lý trực tiếp giao được cho cấp dưới dù không có quyền đó.
 */
@Component
@RequiredArgsConstructor
class WorkTaskAccess {
    static final String VIEW = "TASK_VIEW";
    static final String ASSIGN = "TASK_ASSIGN";

    private static final String ACTIVE_EMPLOYEES = """
            select u.id, coalesce(u.full_name, u.username) as full_name, u.username, u.job_title, st.store_id, s.name as store_name
            from users u
            left join lateral (select ss.store_id from store_staffs ss where ss.user_id = u.id and ss.active
                               order by ss.created_at desc nulls last limit 1) st on true
            left join stores s on s.id = st.store_id
            where u.deleted_at is null and coalesce(u.active, false) and not coalesce(u.locked, false)
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final StoreAccessService storeAccess;
    private final UserScopeService scopes;

    /** Null là phạm vi toàn hệ thống (nhân viên Global). */
    UUID ownScope(UUID actor) {
        UserScope scope = scopes.resolve(actor);
        return scope.isGlobal() ? null : scope.storeId();
    }

    boolean isGlobal(UUID actor) {
        return scopes.resolve(actor).isGlobal();
    }

    boolean canViewScope(UUID actor, UUID storeOrAll) {
        return storeAccess.allowsScope(actor, storeOrAll, VIEW) || storeAccess.allowsScope(actor, storeOrAll, ASSIGN);
    }

    /** Phạm vi của danh sách / thống kê: nhân viên cửa hàng luôn bị giới hạn vào cửa hàng của mình. */
    UUID requireListScope(UUID actor, UUID requestedStore) {
        UUID store = scopes.resolveStoreId(actor, requestedStore);
        if (!canViewScope(actor, store)) throw BusinessException.forbidden("Bạn không có quyền xem công việc trong phạm vi này");
        return store;
    }

    List<Person> assignable(UUID actor, Collection<UUID> onlyIds) {
        UUID store = ownScope(actor);
        UUID manager = jdbc.queryForObject("select manager_id from users where id = :id",
                new MapSqlParameterSource("id", actor), UUID.class);
        MapSqlParameterSource p = new MapSqlParameterSource("actor", actor)
                .addValue("manager", manager, Types.OTHER)
                .addValue("store", store, Types.OTHER)
                .addValue("scoped", storeAccess.allowsScope(actor, store, ASSIGN));
        String sql = ACTIVE_EMPLOYEES + """
                  and u.id <> :actor and (cast(:manager as uuid) is null or u.id <> :manager)
                  and ((cast(:scoped as boolean) and (cast(:store as uuid) is null or st.store_id = :store)) or u.manager_id = :actor)
                """;
        if (onlyIds != null) {
            sql += " and u.id in (:ids)";
            p.addValue("ids", onlyIds);
        }
        return jdbc.query(sql + " order by 2", p, WorkTaskAccess::person);
    }

    List<Person> peopleInScope(UUID store) {
        return jdbc.query(ACTIVE_EMPLOYEES + " and (cast(:store as uuid) is null or st.store_id = :store) order by 2",
                new MapSqlParameterSource().addValue("store", store, Types.OTHER), WorkTaskAccess::person);
    }

    Actions actions(UUID actor, Task task) {
        boolean assignee = actor.equals(task.assigneeId());
        boolean assigner = actor.equals(task.assignerId());
        // Người được giao không tự trả lại / hủy việc của chính mình dù có quyền giao việc.
        boolean manager = !assignee && storeAccess.allowsScope(actor, task.storeId(), ASSIGN);
        boolean viewer = storeAccess.allowsScope(actor, task.storeId(), VIEW);
        boolean open = WorkTaskService.TODO.equals(task.status()) || WorkTaskService.IN_PROGRESS.equals(task.status());
        boolean done = WorkTaskService.DONE.equals(task.status());
        boolean involved = assignee || assigner || manager;
        return new Actions(involved || viewer, assignee && WorkTaskService.TODO.equals(task.status()), assignee && open,
                assignee && open, assigner && open, done && (assigner || manager), open && (assigner || manager),
                involved && !WorkTaskService.CANCELLED.equals(task.status()), assigner || manager || viewer);
    }

    private static Person person(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new Person(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("username"),
                rs.getString("job_title"), rs.getObject("store_id", UUID.class), rs.getString("store_name"));
    }
}
