import { useEffect, useState } from "react";

/**
 * Tải một bản ghi chi tiết theo id từ API storefront. Thiếu id hoặc 404 thì trả {@code notFoundMessage}.
 *
 * @param load (id, {signal}) => Promise
 */
export function useStorefrontResource(id, load, notFoundMessage) {
  const [state, setState] = useState({ id: null, data: null, error: "" });

  useEffect(() => {
    if (!id) return undefined;
    const controller = new AbortController();
    load(id, { signal: controller.signal })
      .then((data) => setState({ id, data, error: "" }))
      .catch((failure) => {
        if (controller.signal.aborted) return;
        // id sai định dạng: backend trả 400, với khách cũng là "không tồn tại".
        const missing = failure.status === 404 || failure.status === 400;
        setState({ id, data: null, error: missing ? notFoundMessage : failure.message });
      });
    return () => controller.abort();
  }, [id, load, notFoundMessage]);

  if (!id) return { data: null, loading: false, error: notFoundMessage };
  const loading = state.id !== id;
  return { data: loading ? null : state.data, loading, error: loading ? "" : state.error };
}
