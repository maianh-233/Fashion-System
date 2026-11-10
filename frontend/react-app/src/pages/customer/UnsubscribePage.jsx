import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { LoaderCircle, MailX } from "lucide-react";
import { marketingUnsubscribeApi } from "../../api/storefront/marketingUnsubscribeApi";

/**
 * Trang hủy nhận email khuyến mãi, mở từ link trong email. Cần bấm xác nhận (không tự hủy khi mở trang) để trình quét
 * link của hộp thư không hủy hộ khách.
 */
export default function UnsubscribePage() {
  const [params] = useSearchParams();
  const token = params.get("token") || "";
  const [info, setInfo] = useState(null);
  const [error, setError] = useState(token ? "" : "Link hủy đăng ký không hợp lệ.");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!token) return undefined;
    const controller = new AbortController();
    marketingUnsubscribeApi.info(token, { signal: controller.signal })
      .then(setInfo)
      .catch((failure) => { if (!controller.signal.aborted) setError(failure.message); });
    return () => controller.abort();
  }, [token]);

  const confirm = async () => {
    setBusy(true);
    setError("");
    try {
      setInfo(await marketingUnsubscribeApi.unsubscribe(token));
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="mx-auto max-w-lg px-4 py-16 text-center">
      <MailX className="mx-auto mb-4" size={36} aria-hidden="true" />
      <h1 className="mb-3 text-2xl font-semibold">Hủy nhận email ưu đãi</h1>
      {error && <p role="alert" className="mb-4 text-sm text-red-600">{error}</p>}
      {!error && !info && <p className="inline-flex items-center gap-2 text-sm"><LoaderCircle className="animate-spin" size={15} /> Đang tải...</p>}
      {info && (info.subscribed ? (
        <>
          <p className="mb-6 text-sm">Email <b>{info.email}</b> sẽ không nhận email khuyến mãi từ Lunaria nữa. Email về đơn hàng vẫn được gửi.</p>
          <button type="button" onClick={confirm} disabled={busy} className="rounded-lg bg-amber-700 px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60">
            {busy ? "Đang xử lý..." : "Xác nhận hủy đăng ký"}
          </button>
        </>
      ) : (
        <p className="mb-6 text-sm">Email <b>{info.email || "của bạn"}</b> đã được hủy nhận email khuyến mãi. Bạn có thể đăng ký lại trong trang hồ sơ.</p>
      ))}
      <p className="mt-8 text-sm"><Link to="/" className="underline">Về trang chủ</Link></p>
    </main>
  );
}
