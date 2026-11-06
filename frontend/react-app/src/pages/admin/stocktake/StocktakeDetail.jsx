import { useEffect, useRef, useState } from "react";
import PageGuide from "../../../components/admin/common/PageGuide";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Camera, CameraOff, Check, ClipboardCheck, ScanBarcode, Send, Undo2, X } from "lucide-react";
import Pagination from "../../../components/common/Pagination";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import {
  Badge, Empty, ErrorText, Field, Loading, ReasonDialog, Section, Stat, btnDanger, btnGhost, btnPrimary, control, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { stocktakeApi } from "../../../api/admin/stocktakeApi";
import { COST_SOURCE, ITEM_FILTERS, STOCKTAKE_STATUS, formatDateTime, money, qty, signed, variantText, varianceTone } from "./stocktakeLogic";

export default function StocktakeDetail() {
  const { id } = useParams();
  const [version, setVersion] = useState(0);
  const refresh = () => setVersion((v) => v + 1);
  const { data: st, error, loading } = useLoader((signal) => stocktakeApi.detail(id, { signal }), [id, version]);
  const [dialog, setDialog] = useState(null);
  const [actionError, setActionError] = useState("");

  const act = async (action) => {
    setActionError("");
    try {
      await action();
      setDialog(null);
      refresh();
    } catch (e) {
      setActionError(e.message);
      throw e;
    }
  };

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <Link to="/admin/stocktakes" className="inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-zinc-200"><ArrowLeft size={15} />Danh sách kiểm kê</Link>
      <ErrorText>{error || actionError}</ErrorText>
      {loading && !st ? <Loading /> : st && (
        <>
          <AdminCatalogPageHeader icon={ClipboardCheck} eyebrow={st.storeName} title={`Phiếu kiểm kê ${st.code}`}
            description={`${st.categoryName ? `Danh mục ${st.categoryName} (gồm danh mục con)` : "Toàn cửa hàng"}${st.blindCount ? " · đếm mù" : ""} · tạo ${formatDateTime(st.createdAt)} bởi ${st.createdByName || "—"}${st.note ? ` · ${st.note}` : ""}`}
            status={<Badge map={STOCKTAKE_STATUS} value={st.status} />} />
          {st.rejectionReason && st.status === "IN_PROGRESS" && (
            <p className="rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">Quản lý trả về đếm lại: {st.rejectionReason}</p>
          )}
          <SummaryCards st={st} />
          <Actions st={st} onAction={setDialog} />
          {st.adjustment && <AdjustmentCard st={st} />}
          {st.canCount && <PageGuide id="stocktakeCount" />}
          {st.canCount && <ScanPanel id={st.id} onScanned={refresh} />}
          <ItemsTable st={st} version={version} onChanged={refresh} />
        </>
      )}
      {dialog === "submit" && <SubmitDialog st={st} onClose={() => setDialog(null)} onConfirm={(zero) => act(() => stocktakeApi.submit(st.id, zero))} />}
      {dialog === "approve" && <ApproveDialog st={st} onClose={() => setDialog(null)} onConfirm={(note) => act(() => stocktakeApi.approve(st.id, note))} />}
      {dialog === "reject" && (
        <ReasonDialog title="Trả về đếm lại" description="Phiếu quay lại bước đếm, số đã đếm được giữ nguyên." label="Lý do / vị trí cần đếm lại" confirmText="Trả về"
          onClose={() => setDialog(null)} onConfirm={(reason) => act(() => stocktakeApi.reject(st.id, reason))} />
      )}
      {dialog === "cancel" && (
        <ReasonDialog title={`Hủy phiếu ${st.code}?`} description="Tồn kho không thay đổi. Cửa hàng có thể tạo phiếu kiểm kê mới." label="Ghi chú" required={false} confirmText="Hủy phiếu" danger
          onClose={() => setDialog(null)} onConfirm={() => act(() => stocktakeApi.cancel(st.id))} />
      )}
    </div>
  );
}

function SummaryCards({ st }) {
  const s = st.summary;
  const pct = s.itemCount ? Math.round((s.countedCount / s.itemCount) * 100) : 0;
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      <Stat label="Đã đếm" value={`${qty(s.countedCount)} / ${qty(s.itemCount)}`} hint={`${pct}% mã hàng · ${qty(s.countedQuantity)} sản phẩm`} tone="sky" />
      {st.systemVisible ? (
        <>
          <Stat label="Tồn hệ thống (lúc tạo phiếu)" value={qty(s.systemQuantity)} hint="Tại quầy + online + đang giữ cho đơn" />
          <Stat label="Thiếu" value={`−${qty(s.shortageQuantity)}`} hint={money(s.shortageValue)} tone={s.shortageQuantity ? "rose" : "zinc"} />
          <Stat label="Thừa" value={`+${qty(s.surplusQuantity)}`} hint={money(s.surplusValue)} tone={s.surplusQuantity ? "emerald" : "zinc"} />
        </>
      ) : (
        <div className="col-span-1 rounded-2xl border border-zinc-800 bg-zinc-900 p-4 text-sm text-zinc-400 md:col-span-3">
          Phiếu đếm mù: số tồn hệ thống và chênh lệch được ẩn cho tới khi quản lý duyệt.
        </div>
      )}
    </div>
  );
}

function Actions({ st, onAction }) {
  const buttons = [];
  if (st.canCount) buttons.push(<button key="submit" type="button" className={btnPrimary} onClick={() => onAction("submit")}><Send size={15} />Gửi duyệt</button>);
  if (st.canApprove && st.status === "SUBMITTED") {
    buttons.push(<button key="reject" type="button" className={btnGhost} onClick={() => onAction("reject")}><Undo2 size={15} />Trả về đếm lại</button>);
    buttons.push(<button key="approve" type="button" className={btnPrimary} onClick={() => onAction("approve")}><Check size={15} />Duyệt & điều chỉnh tồn</button>);
  }
  const canCancel = (st.status === "IN_PROGRESS" && (st.canCount || st.canApprove)) || (st.status === "SUBMITTED" && st.canApprove);
  if (canCancel) buttons.unshift(<button key="cancel" type="button" className={btnDanger} onClick={() => onAction("cancel")}><X size={15} />Hủy phiếu</button>);
  if (!buttons.length) {
    if (st.status === "SUBMITTED") return <p className="text-sm text-zinc-400">Đã gửi duyệt lúc {formatDateTime(st.submittedAt)} bởi {st.submittedByName}. Đang chờ quản lý duyệt.</p>;
    return null;
  }
  return <div className="flex flex-wrap justify-end gap-2">{buttons}</div>;
}

function AdjustmentCard({ st }) {
  const a = st.adjustment;
  return (
    <Section title={`Phiếu điều chỉnh ${a.code}`}>
      <div className="grid gap-3 text-sm sm:grid-cols-4">
        <div><p className="text-xs text-zinc-500">Duyệt bởi</p><p className="text-zinc-200">{a.approvedByName} · {formatDateTime(a.createdAt)}</p></div>
        <div><p className="text-xs text-zinc-500">Số dòng điều chỉnh</p><p className="text-zinc-200">{qty(a.lineCount)}</p></div>
        <div><p className="text-xs text-zinc-500">Giảm tồn (hao hụt)</p><p className="text-rose-300">−{qty(a.decreaseQuantity)} · {money(a.decreaseValue)}</p></div>
        <div><p className="text-xs text-zinc-500">Tăng tồn</p><p className="text-emerald-300">+{qty(a.increaseQuantity)} · {money(a.increaseValue)}</p></div>
      </div>
      {st.approverNote && <p className="mt-3 text-sm text-zinc-400">Ghi chú duyệt: {st.approverNote}</p>}
    </Section>
  );
}

// ------------------------------------------------------------------ quét mã

function ScanPanel({ id, onScanned }) {
  const [code, setCode] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [log, setLog] = useState([]);
  const [busy, setBusy] = useState(false);
  const [camera, setCamera] = useState(false);
  const input = useRef(null);
  const cameraSupported = typeof window !== "undefined" && "BarcodeDetector" in window && navigator.mediaDevices?.getUserMedia;

  const scan = async (value) => {
    const text = String(value || "").trim();
    if (!text || busy) return;
    setBusy(true);
    try {
      const result = await stocktakeApi.scan(id, text, Number(quantity) > 1 ? Number(quantity) : undefined);
      setLog((l) => [{ ok: true, text: result.message, at: Date.now() }, ...l].slice(0, 6));
      beep(true);
      onScanned();
    } catch (e) {
      setLog((l) => [{ ok: false, text: `${text}: ${e.message}`, at: Date.now() }, ...l].slice(0, 6));
      beep(false);
    } finally {
      setBusy(false);
      setCode("");
      setQuantity(1);
      input.current?.focus();
    }
  };

  return (
    <Section title="Quét mã vạch" action={cameraSupported && (
      <button type="button" className={btnGhost} onClick={() => setCamera((c) => !c)}>{camera ? <CameraOff size={15} /> : <Camera size={15} />}{camera ? "Tắt camera" : "Quét bằng camera"}</button>
    )}>
      <form className="flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); scan(code); }}>
        <div className="relative min-w-[14rem] flex-1">
          <ScanBarcode size={18} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input ref={input} autoFocus className={`${control} h-12 pl-10 text-base`} value={code} onChange={(e) => setCode(e.target.value)}
            placeholder="Quét mã vạch hoặc gõ SKU rồi Enter" autoComplete="off" inputMode="text" aria-label="Mã vạch hoặc SKU" />
        </div>
        <Field label="" className="w-24">
          <input type="number" min="1" max="9999" className={`${control} h-12 text-center`} value={quantity} onChange={(e) => setQuantity(e.target.value)} aria-label="Số lượng mỗi lần quét" title="Số lượng mỗi lần quét" />
        </Field>
        <button type="submit" className={`${btnPrimary} h-12`} disabled={busy || !code.trim()}>Ghi nhận</button>
      </form>
      <p className="mt-2 text-xs text-zinc-500">Máy quét cầm tay hoạt động như bàn phím: mỗi lần quét cộng thêm số lượng bên cạnh (mặc định 1). Đếm theo thùng: nhập số lượng rồi quét một lần.</p>
      {camera && <CameraScanner onDetected={scan} />}
      {log.length > 0 && (
        <ul className="mt-3 space-y-1 text-sm" aria-live="polite">
          {log.map((l) => <li key={l.at} className={l.ok ? "text-emerald-300" : "text-rose-300"}>{l.ok ? "✓" : "✕"} {l.text}</li>)}
        </ul>
      )}
    </Section>
  );
}

/** Quét bằng camera điện thoại / máy tính bảng qua BarcodeDetector (Chrome / Edge / Android). */
function CameraScanner({ onDetected }) {
  const video = useRef(null);
  const [error, setError] = useState("");
  const callback = useRef(onDetected);
  useEffect(() => { callback.current = onDetected; }, [onDetected]);

  useEffect(() => {
    let stream;
    let timer;
    let last = { code: "", at: 0 };
    let cancelled = false;
    const detector = new window.BarcodeDetector({ formats: ["ean_13", "ean_8", "code_128", "code_39", "upc_a", "upc_e", "qr_code"] });
    navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } })
      .then((s) => {
        if (cancelled) { s.getTracks().forEach((t) => t.stop()); return; }
        stream = s;
        video.current.srcObject = s;
        video.current.play();
        timer = setInterval(async () => {
          if (!video.current || video.current.readyState < 2) return;
          try {
            const codes = await detector.detect(video.current);
            const value = codes[0]?.rawValue;
            const now = Date.now();
            if (value && (value !== last.code || now - last.at > 1500)) {
              last = { code: value, at: now };
              callback.current(value);
            }
          } catch {
            // khung hình lỗi: bỏ qua
          }
        }, 300);
      })
      .catch(() => setError("Không mở được camera — hãy cấp quyền camera cho trang này."));
    return () => {
      cancelled = true;
      clearInterval(timer);
      stream?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  return (
    <div className="mt-3">
      <ErrorText>{error}</ErrorText>
      <video ref={video} className="max-h-72 w-full rounded-2xl bg-black object-cover" muted playsInline />
    </div>
  );
}

function beep(ok) {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    osc.frequency.value = ok ? 1200 : 300;
    osc.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + (ok ? 0.08 : 0.25));
    osc.onended = () => ctx.close();
  } catch {
    // trình duyệt chặn âm thanh: bỏ qua
  }
}

// ------------------------------------------------------------------ danh sách hàng

function ItemsTable({ st, version, onChanged }) {
  const [filter, setFilter] = useState("ALL");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const { data, error, loading } = useLoader(
    (signal) => stocktakeApi.items(st.id, { filter, keyword, page: page - 1, size: 50 }, { signal }), [st.id, filter, keyword, page, version]);
  const rows = data?.content || [];
  const showSystem = st.systemVisible;

  return (
    <Section title="Mã hàng kiểm kê">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        {ITEM_FILTERS.filter((f) => !f.needsSystem || showSystem).map((f) => (
          <button key={f.value} type="button" onClick={() => { setFilter(f.value); setPage(1); }}
            className={`rounded-xl border px-3 py-1.5 text-sm ${filter === f.value ? "border-amber-400 bg-amber-400/10 text-amber-300" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>{f.label}</button>
        ))}
        <input className={`${control} ml-auto w-full sm:w-64`} placeholder="Tìm SKU, mã vạch, tên…" value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(1); }} />
      </div>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : !rows.length ? <Empty>Không có mã hàng phù hợp.</Empty> : (
        <div className="-mx-5 overflow-x-auto px-5">
          <table className="w-full min-w-[820px] text-sm">
            <thead className="text-left text-xs text-zinc-400">
              <tr className="border-b border-zinc-800">
                <th className="py-2 pr-3 font-medium">Sản phẩm</th>
                {showSystem && <th className="px-2 py-2 text-right font-medium">Hệ thống</th>}
                <th className="px-2 py-2 text-right font-medium">Đếm được</th>
                {showSystem && <th className="px-2 py-2 text-right font-medium">Chênh lệch</th>}
                {showSystem && <th className="px-2 py-2 text-right font-medium">Giá trị lệch</th>}
                {st.status === "APPROVED" && <th className="px-2 py-2 text-right font-medium">Đã điều chỉnh</th>}
                <th className="py-2 pl-2 font-medium">Người đếm</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => (
                <tr key={item.id} className="border-b border-zinc-800/70 align-top">
                  <td className="py-2 pr-3">
                    <p className="font-medium text-zinc-100">{item.productName} <span className="font-normal text-zinc-400">{variantText(item)}</span></p>
                    <p className="text-xs text-zinc-500">{item.sku}{item.barcode ? ` · ${item.barcode}` : ""}{item.addedDuringCount ? " · phát sinh khi đếm" : ""}</p>
                    {item.note && <p className="text-xs text-zinc-400">{item.note}</p>}
                  </td>
                  {showSystem && <td className="px-2 py-2 text-right tabular-nums text-zinc-300">{qty(item.systemQuantity)}</td>}
                  <td className="px-2 py-2 text-right">
                    {st.canCount ? <CountInput key={`${item.id}-${item.countedQuantity}`} st={st} item={item} onSaved={onChanged} /> : <span className="tabular-nums">{item.countedQuantity == null ? <span className="text-zinc-500">Chưa đếm</span> : qty(item.countedQuantity)}</span>}
                  </td>
                  {showSystem && <td className={`px-2 py-2 text-right font-semibold tabular-nums ${varianceTone(item.variance)}`}>{signed(item.variance)}</td>}
                  {showSystem && (
                    <td className="px-2 py-2 text-right tabular-nums text-zinc-300" title={COST_SOURCE[item.costSource]}>
                      {item.variance ? money(Math.abs(item.variance) * Number(item.unitCost || 0)) : "—"}
                    </td>
                  )}
                  {st.status === "APPROVED" && <td className={`px-2 py-2 text-right tabular-nums ${varianceTone(item.adjustedQuantity)}`}>{signed(item.adjustedQuantity)}</td>}
                  <td className="py-2 pl-2 text-xs text-zinc-400">{item.countedByName}{item.countedAt && <p>{formatDateTime(item.countedAt)}</p>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />
    </Section>
  );
}

function CountInput({ st, item, onSaved }) {
  const [value, setValue] = useState(item.countedQuantity ?? "");
  const [error, setError] = useState("");
  const save = async () => {
    if (value === "" || Number(value) === item.countedQuantity) return;
    try {
      await stocktakeApi.setCount(st.id, item.id, Number(value), item.note);
      setError("");
      onSaved();
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <div className="flex flex-col items-end">
      <input type="number" min="0" className={`${control} h-9 w-24 text-right`} value={value} placeholder="—"
        onChange={(e) => setValue(e.target.value)} onBlur={save} onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); e.currentTarget.blur(); } }}
        aria-label={`Số đếm ${item.sku}`} />
      {error && <span className="mt-1 text-xs text-rose-300">{error}</span>}
    </div>
  );
}

// ------------------------------------------------------------------ hộp thoại

function SubmitDialog({ st, onClose, onConfirm }) {
  const [zero, setZero] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const uncounted = st.summary.itemCount - st.summary.countedCount;
  const submit = async () => {
    setSaving(true);
    try {
      await onConfirm(zero);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <AdminDialogHeader title="Gửi duyệt kiểm kê" description={`Đã đếm ${st.summary.countedCount}/${st.summary.itemCount} mã hàng. Sau khi gửi, không sửa số đếm được nữa (trừ khi quản lý trả về).`} onClose={onClose} />
      <AdminDialogBody className="space-y-3">
        {uncounted > 0 ? (
          <label className="flex items-start gap-2 rounded-xl border border-amber-500/30 bg-amber-500/5 p-3 text-sm text-zinc-200">
            <input type="checkbox" className="mt-0.5" checked={zero} onChange={(e) => setZero(e.target.checked)} />
            <span>Tính {uncounted} mã hàng chưa đếm là <b>0</b> (không còn hàng)
              <span className="block text-xs text-zinc-400">Không chọn: các mã chưa đếm được giữ nguyên tồn hệ thống khi duyệt.</span></span>
          </label>
        ) : <p className="text-sm text-emerald-300">Đã đếm đủ tất cả mã hàng.</p>}
        <ErrorText>{error}</ErrorText>
      </AdminDialogBody>
      <AdminDialogFooter>
        <button type="button" className={btnGhost} onClick={onClose}>Đóng</button>
        <button type="button" className={btnPrimary} onClick={submit} disabled={saving}>{saving ? "Đang gửi…" : "Gửi duyệt"}</button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}

function ApproveDialog({ st, onClose, onConfirm }) {
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const s = st.summary;
  const submit = async () => {
    setSaving(true);
    try {
      await onConfirm(note.trim() || null);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <AdminDialogHeader title="Duyệt và điều chỉnh tồn" description="Hệ thống sinh phiếu điều chỉnh và cộng / trừ chênh lệch vào tồn hiện tại của cửa hàng. Không hoàn tác được." onClose={onClose} />
      <AdminDialogBody className="space-y-3">
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div className="rounded-xl border border-zinc-800 p-3"><p className="text-xs text-zinc-500">Giảm tồn</p><p className="text-lg font-semibold text-rose-300">−{qty(s.shortageQuantity)}</p><p className="text-xs text-zinc-400">{money(s.shortageValue)}</p></div>
          <div className="rounded-xl border border-zinc-800 p-3"><p className="text-xs text-zinc-500">Tăng tồn</p><p className="text-lg font-semibold text-emerald-300">+{qty(s.surplusQuantity)}</p><p className="text-xs text-zinc-400">{money(s.surplusValue)}</p></div>
        </div>
        <p className="text-xs text-zinc-400">{s.varianceCount} mã hàng lệch{st.zeroUncounted ? " · mã chưa đếm được tính là 0" : " · mã chưa đếm giữ nguyên"}. Thiếu trừ tồn tại quầy trước, rồi tới tồn online.</p>
        <Field label="Ghi chú duyệt"><textarea className={textarea} rows={2} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} /></Field>
        <ErrorText>{error}</ErrorText>
      </AdminDialogBody>
      <AdminDialogFooter>
        <button type="button" className={btnGhost} onClick={onClose}>Đóng</button>
        <button type="button" className={btnPrimary} onClick={submit} disabled={saving}>{saving ? "Đang duyệt…" : "Duyệt & điều chỉnh"}</button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
