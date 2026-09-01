import { useCallback, useEffect, useMemo, useState } from "react";

const PERMISSIONS_ENDPOINT = "/api/me/permissions";

async function requestPermissions(signal) {
  const response = await fetch(PERMISSIONS_ENDPOINT, {
    method: "GET",
    credentials: "include",
    headers: { Accept: "application/json" },
    signal,
  });

  if (!response.ok) {
    throw new Error(`Không thể tải quyền người dùng (${response.status})`);
  }

  const data = await response.json();
  return Array.isArray(data?.modules) ? data.modules : [];
}

export default function usePermissions() {
  const [modules, setModules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadPermissions = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      setModules(await requestPermissions());
    } catch (requestError) {
      setModules([]);
      setError(requestError);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    requestPermissions(controller.signal)
      .then((nextModules) => {
        setModules(nextModules);
        setError(null);
      })
      .catch((requestError) => {
        if (requestError.name === "AbortError") return;
        setModules([]);
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
    loading,
    error,
    reload: loadPermissions,
    hasPermission: (permissionCode) => permissionIndex.has(permissionCode),
    getPermission: (permissionCode) => permissionIndex.get(permissionCode) || null,
    getScope: (permissionCode) => permissionIndex.get(permissionCode)?.scope || null,
  };
}
