import { useCallback, useEffect, useState } from "react";
import { requestAdmin } from "../api/auth/adminSession";

const cache = new Map();

export default function useStoreOptions({ endpoint = "/api/inventory/stores", enabled = true } = {}) {
  const [revision, setRevision] = useState(0);
  const [state, setState] = useState(() => ({ stores: cache.get(endpoint) || [], loading: enabled && !cache.has(endpoint), error: "" }));
  const reload = useCallback(() => { cache.delete(endpoint); setState(current => ({ ...current, loading: true, error: "" })); setRevision(value => value + 1); }, [endpoint]);

  useEffect(() => {
    if (!enabled) return undefined;
    const cached = cache.get(endpoint);
    if (cached) {
      Promise.resolve().then(() => setState({ stores: cached, loading: false, error: "" }));
      return undefined;
    }
    const controller = new AbortController();
    requestAdmin(endpoint, { signal: controller.signal })
      .then(result => {
        if (controller.signal.aborted) return;
        const stores = Array.isArray(result) ? result : result?.content || [];
        cache.set(endpoint, stores);
        setState({ stores, loading: false, error: "" });
      })
      .catch(error => {
        if (!controller.signal.aborted) setState({ stores: [], loading: false, error: error.message || "Không thể tải cửa hàng." });
      });
    return () => controller.abort();
  }, [enabled, endpoint, revision]);

  return enabled ? { ...state, reload } : { stores: [], loading: false, error: "", reload };
}
