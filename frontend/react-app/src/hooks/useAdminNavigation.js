import { useMemo } from "react";
import { useLocation } from "react-router-dom";
import {
  buildAdminNavigation,
  findActiveNavigation,
} from "../components/admin/adminNavigation";
import { useAdminPermissions } from "../contexts/AdminPermissionsContext";

export default function useAdminNavigation() {
  const location = useLocation();
  const permissions = useAdminPermissions();
  const includeDemoFallback = import.meta.env.VITE_ENABLE_ADMIN_NAVIGATION_FALLBACK === "true";

  const modules = useMemo(
    () => buildAdminNavigation(permissions.modules, includeDemoFallback),
    [includeDemoFallback, permissions.modules],
  );

  const active = useMemo(
    () => findActiveNavigation(modules, location.pathname),
    [location.pathname, modules],
  );

  return {
    ...permissions,
    modules,
    activeModule: active.activeModule,
    activeGroup: active.activeGroup,
    activeModuleCode: active.activeModule?.code || null,
    activeGroupCode: active.activeGroup?.code || null,
    isDemoFallback: includeDemoFallback,
  };
}
