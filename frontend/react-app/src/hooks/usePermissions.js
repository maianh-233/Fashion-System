import { useCallback, useEffect, useMemo, useState } from "react";
import { requestAdmin } from "../api/auth/adminSession";

const PERMISSIONS_ENDPOINT = "/api/me/permissions";
const SCOPE_ENDPOINT = "/api/me/scope";

async function requestPermissions(signal) {
  const data = await requestAdmin(PERMISSIONS_ENDPOINT, { signal });
  return Array.isArray(data?.modules) ? data.modules : [];
}

async function requestSecurityContext(signal) {
  const [modules, scope] = await Promise.all([
    requestPermissions(signal),
    requestAdmin(SCOPE_ENDPOINT, { signal }),
  ]);
  return { modules, scope };
}

export default function usePermissions() {
  const [modules, setModules] = useState([]);
  const [scope, setScope] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadPermissions = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const next = await requestSecurityContext();
      setModules(next.modules);
      setScope(next.scope);
    } catch (requestError) {
      setModules([]);
      setScope(null);
      setError(requestError);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    requestSecurityContext(controller.signal)
      .then((next) => {
        setModules(next.modules);
        setScope(next.scope);
        setError(null);
      })
      .catch((requestError) => {
        if (requestError.name === "AbortError") return;
        setModules([]);
        setScope(null);
        setError(requestError);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, []);

  const permissionIndex = useMemo(() => {
    const index = new Map();
    modules.forEach((module) => {
      (module.groups || []).forEach((group) => {
        (group.permissions || []).forEach((permission) => {
          index.set(permission.code, permission);
        });
      });
    });
    return index;
  }, [modules]);

  return {
    modules,
    scope,
    isGlobal: scope?.scope === "GLOBAL",
    isStore: scope?.scope === "STORE",
    currentStoreId: scope?.storeId || null,
    currentStoreName: scope?.storeName || null,
    loading,
    error,
    reload: loadPermissions,
    hasPermission: (permissionCode) => permissionIndex.has(permissionCode),
    getPermission: (permissionCode) => permissionIndex.get(permissionCode) || null,
    getScope: (permissionCode) => permissionIndex.get(permissionCode)?.scope || null,
  };
}
