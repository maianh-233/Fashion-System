import { useEffect, useState } from "react";
import { Eye, EyeOff, MessageSquareReply, RotateCcw, Search, Star } from "lucide-react";
import Pagination from "../../components/common/Pagination";
import Button from "../../components/common/Button";
import { useSystemNotification } from "../../components/common/SystemNotification";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import { reviewAdminApi } from "../../api/admin/reviewApi";
import { EMPTY_REVIEW_FILTERS, normalizeReviewPage, reviewQuery } from "./reviewManagementLogic";

const PAGE_SIZE = 20;
const control = "h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition focus:border-amber-400";
const formatTime = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "");

function StarsCell({ value }) {
  return (
    <span className="inline-flex text-amber-400" aria-label={`${value} sao`}>
      {[1, 2, 3, 4, 5].map((star) => <Star key={star} size={13} fill={star <= value ? "currentColor" : "none"} />)}
    </span>
  );
}

/** Đánh giá của khách: lọc, ẩn/hiện có lý do và trả lời (phản hồi hiện dưới đánh giá trên trang sản phẩm). */
export default function ReviewManagement() {
  const notification = useSystemNotification();
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState(EMPTY_REVIEW_FILTERS);
  const [filters, setFilters] = useState(EMPTY_REVIEW_FILTERS);
  const [version, setVersion] = useState(0);
  const [data, setData] = useState({ rows: [], totalPages: 1, totalElements: 0 });
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const [dialog, setDialog] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    reviewAdminApi.list(reviewQuery(filters, page, PAGE_SIZE), { signal: controller.signal })
      .then((response) => { setData(normalizeReviewPage(response)); setState("ready"); })
      .catch((failure) => {
        if (!controller.signal.aborted) { setError(failure.message || "Không tải được đánh giá."); setState("error"); }
      });
    return () => controller.abort();
  }, [filters, page, version]);

  const reload = () => { setState("loading"); setError(""); };
  const update = (key) => (event) => setDraft((current) => ({ ...current, [key]: event.target.value }));
  const apply = (event) => { event.preventDefault(); reload(); setPage(1); setFilters({ ...draft }); };
  const reset = () => { reload(); setDraft(EMPTY_REVIEW_FILTERS); setFilters({ ...EMPTY_REVIEW_FILTERS }); setPage(1); };
  const changePage = (next) => { reload(); setPage(next); };

  const replaceRow = (updated) =>
    setData((current) => ({ ...current, rows: current.rows.map((row) => (row.id === updated.id ? updated : row)) }));

  const show = async (review) => {
    try {
      replaceRow(await reviewAdminApi.setVisibility(review.id, false));
      notification.success("Đã hiện lại đánh giá trên trang sản phẩm.");
    } catch (failure) {
      notification.error(failure.message);
    }
  };

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100">
      <AdminCatalogPageHeader icon={Star} eyebrow="Sản phẩm" title="Đánh giá sản phẩm"
        description="Đánh giá của khách đã mua hàng. Đánh giá hiển thị ngay; ẩn nội dung không phù hợp và trả lời khách tại đây."
        status={`${data.totalElements.toLocaleString("vi-VN")} đánh giá`} />

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
        <form className="space-y-3 border-b border-zinc-800 bg-zinc-950 p-4 sm:p-6" onSubmit={apply}>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <label className="space-y-1.5 text-sm text-zinc-400 xl:col-span-2">
              <span>Tìm kiếm</span>
              <input className={control} value={draft.keyword} onChange={update("keyword")}
                placeholder="Tên / mã sản phẩm, mã đơn, tên khách, nội dung" />
            </label>
            <label className="space-y-1.5 text-sm text-zinc-400">
              <span>Số sao</span>
              <select className={control} value={draft.rating} onChange={update("rating")}>
                <option value="">Tất cả</option>
                {[5, 4, 3, 2, 1].map((star) => <option key={star} value={star}>{star} sao</option>)}
              </select>
            </label>
            <label className="space-y-1.5 text-sm text-zinc-400">
              <span>Trạng thái</span>
              <select className={control} value={draft.visibility} onChange={update("visibility")}>
                <option value="">Tất cả</option>
                <option value="visible">Đang hiển thị</option>
                <option value="hidden">Đã ẩn</option>
              </select>
            </label>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" variant="primary" className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold"><Search size={16} />Lọc</Button>
            <Button type="button" variant="secondary" className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm" onClick={reset}><RotateCcw size={16} />Đặt lại</Button>
          </div>
        </form>

        {error && <p role="alert" className="m-4 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-200">{error}</p>}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1000px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Sản phẩm", "Khách / đơn", "Đánh giá", "Trạng thái", "Thao tác"].map((head) =>
                <th key={head} className="px-4 py-3 text-left font-semibold">{head}</th>)}</tr>
            </thead>
            <tbody>
              {state === "loading" && <tr><td colSpan="5" className="px-4 py-12 text-center text-zinc-400">Đang tải đánh giá…</td></tr>}
              {state === "ready" && data.rows.length === 0 && (
                <tr><td colSpan="5" className="px-4 py-12 text-center text-zinc-400">Không có đánh giá phù hợp.</td></tr>
              )}
              {state === "ready" && data.rows.map((review) => (
                <tr key={review.id} className="border-t border-zinc-800 align-top transition-colors hover:bg-zinc-800/30">
                  <td className="max-w-[220px] px-4 py-3">
                    <p className="font-medium text-zinc-100">{review.productName}</p>
                    <p className="text-xs text-zinc-500">{review.productCode}{review.variantLabel ? ` · ${review.variantLabel}` : ""}</p>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <p className="text-zinc-200">{review.customerName || "Khách hàng"}</p>
                    <p className="text-xs text-zinc-500">#{review.orderCode} · {formatTime(review.createdAt)}</p>
                  </td>
                  <td className="max-w-[380px] px-4 py-3">
                    <StarsCell value={review.rating} />
                    {review.content ? <p className="mt-1 whitespace-pre-line text-zinc-300">{review.content}</p>
                      : <p className="mt-1 text-xs italic text-zinc-500">Không có nhận xét</p>}
                    {review.images.length > 0 && (
                      <div className="mt-2 flex gap-2">
                        {review.images.map((url) => (
                          <a key={url} href={url} target="_blank" rel="noreferrer">
                            <img src={url} alt="" className="h-12 w-12 rounded-lg border border-zinc-700 object-cover" />
                          </a>
                        ))}
                      </div>
                    )}
                    {review.reply && (
                      <p className="mt-2 border-l-2 border-amber-500/60 pl-2 text-xs text-amber-100/80">
                        <MessageSquareReply size={12} className="mr-1 inline" />{review.reply}
                      </p>
                    )}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    {review.hidden ? (
                      <span className="inline-flex flex-col gap-1">
                        <span className="inline-flex w-fit rounded-full border border-rose-400/30 bg-rose-500/10 px-2.5 py-1 text-xs font-semibold text-rose-200">Đã ẩn</span>
                        <small className="max-w-[180px] whitespace-normal text-xs text-zinc-500">{review.hiddenReason}</small>
                      </span>
                    ) : (
                      <span className="inline-flex rounded-full border border-emerald-400/30 bg-emerald-500/10 px-2.5 py-1 text-xs font-semibold text-emerald-200">Đang hiển thị</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-col items-start gap-2">
                      <Button type="button" permission="REVIEW_MANAGE" variant="ghost"
                        className="inline-flex items-center gap-1" onClick={() => setDialog({ type: "reply", review })}>
                        <MessageSquareReply size={14} />{review.reply ? "Sửa phản hồi" : "Trả lời"}
                      </Button>
                      {review.hidden ? (
                        <Button type="button" permission="REVIEW_MANAGE" variant="ghost"
                          className="inline-flex items-center gap-1" onClick={() => show(review)}>
                          <Eye size={14} />Hiện lại
                        </Button>
                      ) : (
                        <Button type="button" permission="REVIEW_MANAGE" variant="danger"
                          className="inline-flex items-center gap-1" onClick={() => setDialog({ type: "hide", review })}>
                          <EyeOff size={14} />Ẩn
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={data.totalPages} onPageChange={changePage} />
      </section>

      {dialog && (
        <ReviewActionDialog dialog={dialog} onClose={() => setDialog(null)}
          onSaved={(updated, message) => {
            setDialog(null);
            replaceRow(updated);
            notification.success(message);
            if (filters.visibility) { reload(); setVersion((value) => value + 1); }
          }} />
      )}
    </div>
  );
}

function ReviewActionDialog({ dialog, onClose, onSaved }) {
  const { type, review } = dialog;
  const hiding = type === "hide";
  const [text, setText] = useState(hiding ? "" : review.reply || "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const save = async (event) => {
    event.preventDefault();
    if (hiding && !text.trim()) { setError("Vui lòng nhập lý do ẩn."); return; }
    setSaving(true);
    setError("");
    try {
      const updated = hiding
        ? await reviewAdminApi.setVisibility(review.id, true, text.trim())
        : await reviewAdminApi.reply(review.id, text.trim());
      onSaved(updated, hiding ? "Đã ẩn đánh giá khỏi trang sản phẩm."
        : text.trim() ? "Đã lưu phản hồi cho khách." : "Đã gỡ phản hồi.");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdminDialog open onClose={onClose} size="md">
      <form onSubmit={save}>
        <AdminDialogHeader title={hiding ? "Ẩn đánh giá" : "Trả lời đánh giá"}
          description={`${review.productName} · ${review.customerName || "Khách hàng"} · ${review.rating} sao`} onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          {review.content && <blockquote className="rounded-xl border border-zinc-800 bg-zinc-950 p-3 text-sm text-zinc-300">{review.content}</blockquote>}
          <label className="block space-y-1.5 text-sm text-zinc-400">
            <span>{hiding ? "Lý do ẩn (khách sẽ thấy đánh giá đang bị ẩn)" : "Phản hồi hiển thị công khai dưới đánh giá (để trống để gỡ)"}</span>
            <textarea rows={4} maxLength={hiding ? 500 : 2000} value={text} onChange={(event) => setText(event.target.value)}
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm text-zinc-100 outline-none focus:border-amber-400" />
          </label>
          {error && <p role="alert" className="text-sm text-rose-300">{error}</p>}
        </AdminDialogBody>
        <AdminDialogFooter>
          <Button type="button" variant="secondary" className="rounded-xl px-4 py-2.5 text-sm" onClick={onClose} disabled={saving}>Hủy</Button>
          <Button type="submit" variant={hiding ? "danger-solid" : "primary"} className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={saving}>
            {hiding ? "Ẩn đánh giá" : "Lưu phản hồi"}
          </Button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
