import { useEffect, useState } from "react";
import { LoaderCircle, Ruler, X } from "lucide-react";
import Button from "../../common/Button";
import { isChartMatch, parseMeasurements, rangeLabel } from "../../../pages/customer/sizeGuideLogic";

/**
 * Bảng size của sản phẩm và ô nhập chiều cao / cân nặng để được gợi ý. Khách đăng nhập có thể lưu số đo vào hồ sơ
 * ({@code onSaveMeasurements}); {@code onPick} chọn size được gợi ý trên trang sản phẩm.
 */
export default function SizeGuideDialog({ productName, guide, loading, error, loggedIn, onCheck, onSaveMeasurements, onPick, onClose }) {
  const [height, setHeight] = useState(guide?.heightCm ?? "");
  const [weight, setWeight] = useState(guide?.weightKg ?? "");
  const [remember, setRemember] = useState(loggedIn);
  const [inputError, setInputError] = useState("");

  useEffect(() => {
    const onKey = (event) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const submit = async (event) => {
    event.preventDefault();
    const parsed = parseMeasurements(height, weight);
    if (parsed.error) return setInputError(parsed.error);
    if (parsed.heightCm == null && parsed.weightKg == null) return setInputError("Nhập chiều cao hoặc cân nặng.");
    setInputError("");
    if (loggedIn && remember) await onSaveMeasurements(parsed);
    onCheck(parsed);
  };

  const entries = guide?.entries || [];
  const recommended = guide?.recommendedSize;

  return (
    <div className="customer-dialog" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="customer-dialog__panel size-guide" role="dialog" aria-modal="true" aria-labelledby="size-guide-title">
        <header>
          <div>
            <small>{productName}</small>
            <h2 id="size-guide-title">Hướng dẫn chọn size</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng"><X size={17} /></button>
        </header>

        <form className="size-guide__form" onSubmit={submit} noValidate>
          <label><span>Chiều cao (cm)</span>
            <input type="number" inputMode="numeric" min="80" max="250" value={height} onChange={(e) => setHeight(e.target.value)} placeholder="165" />
          </label>
          <label><span>Cân nặng (kg)</span>
            <input type="number" inputMode="decimal" min="20" max="250" step="0.1" value={weight} onChange={(e) => setWeight(e.target.value)} placeholder="55" />
          </label>
          <Button type="submit" variant="unstyled" className="size-guide__check" disabled={loading}>
            {loading ? <LoaderCircle className="animate-spin" size={14} /> : <Ruler size={14} />} Gợi ý size
          </Button>
          {loggedIn && (
            <label className="size-guide__remember">
              <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> Lưu số đo vào hồ sơ
            </label>
          )}
        </form>
        {(inputError || error) && <p className="checkout-error">{inputError || error}</p>}

        {guide && (recommended || guide.note) && (
          <div className={`size-guide__result ${recommended ? "is-found" : ""}`} aria-live="polite">
            {recommended ? (
              <>
                <p>Size phù hợp: <strong>{recommended}</strong></p>
                {guide.note && <small>{guide.note}</small>}
                <Button type="button" variant="unstyled" className="size-guide__pick" onClick={() => onPick(recommended)}>
                  Chọn size {recommended}
                </Button>
              </>
            ) : <small>{guide.note}</small>}
          </div>
        )}

        {entries.length > 0 ? (
          <div className="size-guide__table-wrap">
            <table className="size-guide__table">
              <caption>Bảng size {guide.chartCategoryName ? `· ${guide.chartCategoryName}` : ""}</caption>
              <thead><tr><th scope="col">Size</th><th scope="col">Chiều cao</th><th scope="col">Cân nặng</th></tr></thead>
              <tbody>
                {entries.map((entry) => (
                  <tr key={entry.size} className={isChartMatch(entry, guide) ? "is-match" : ""}>
                    <th scope="row">{entry.size}</th>
                    <td>{rangeLabel(entry.minHeightCm, entry.maxHeightCm, "cm")}</td>
                    <td>{rangeLabel(entry.minWeightKg, entry.maxWeightKg, "kg")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : !loading && (
          <p className="customer-dialog__hint">Sản phẩm này chưa có bảng size, bạn có thể nhắn cửa hàng để được tư vấn.</p>
        )}
        <p className="customer-dialog__hint">Số đo chỉ mang tính tham khảo; sản phẩm được đổi size trong 14 ngày.</p>
      </section>
    </div>
  );
}
