import { useCallback, useEffect, useState } from "react";
import { sizeProfileApi } from "../api/customer/accountApi";
import { storefrontApi } from "../api/storefront/storefrontApi";

/**
 * Gợi ý size của một sản phẩm. Khách đăng nhập dùng API có lịch sử mua và số đo đã lưu; khách chưa đăng nhập chỉ
 * có bảng size và số đo nhập tay. {@code check} hỏi lại với số đo mới, {@code saveMeasurements} lưu vào hồ sơ.
 */
export function useSizeGuide(productId, loggedIn) {
  const [state, setState] = useState({ productId: null, guide: null, error: "" });
  const [checking, setChecking] = useState(false);

  const fetchGuide = useCallback((measurements, options) => (loggedIn
    ? sizeProfileApi.guide(productId, measurements, options)
    : storefrontApi.sizeGuide(productId, measurements, options)), [productId, loggedIn]);

  useEffect(() => {
    if (!productId) return undefined;
    const controller = new AbortController();
    fetchGuide({}, { signal: controller.signal })
      .then((guide) => setState({ productId, guide, error: "" }))
      .catch((failure) => {
        if (!controller.signal.aborted) setState({ productId, guide: null, error: failure.message });
      });
    return () => controller.abort();
  }, [productId, fetchGuide]);

  const check = useCallback(async (measurements) => {
    setChecking(true);
    try {
      const guide = await fetchGuide(measurements);
      setState({ productId, guide, error: "" });
    } catch (failure) {
      setState((current) => ({ ...current, error: failure.message }));
    } finally {
      setChecking(false);
    }
  }, [productId, fetchGuide]);

  const saveMeasurements = useCallback(async (measurements) => {
    try {
      await sizeProfileApi.saveMeasurements(measurements);
    } catch (failure) {
      setState((current) => ({ ...current, error: failure.message }));
    }
  }, []);

  const loading = state.productId !== productId || checking;
  return { guide: state.productId === productId ? state.guide : null, error: state.error, loading, check, saveMeasurements };
}
