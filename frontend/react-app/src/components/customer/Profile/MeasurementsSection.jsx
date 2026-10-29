import { useEffect, useState } from "react";
import { LoaderCircle, Save } from "lucide-react";
import Button from "../../common/Button";
import { useSystemNotification } from "../../common/SystemNotification";
import { sizeProfileApi } from "../../../api/customer/accountApi";
import { parseMeasurements } from "../../../pages/customer/sizeGuideLogic";

/** Chiều cao / cân nặng của khách, dùng để gợi ý size trên trang sản phẩm. */
export default function MeasurementsSection() {
  const notification = useSystemNotification();
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    sizeProfileApi.measurements({ signal: controller.signal })
      .then((data) => setForm({ height: data.heightCm ?? "", weight: data.weightKg ?? "" }))
      .catch((failure) => {
        if (!controller.signal.aborted) {
          setForm({ height: "", weight: "" });
          setError(failure.message);
        }
      });
    return () => controller.abort();
  }, []);

  if (!form) return <p className="customer-profile-measurements__loading"><LoaderCircle className="animate-spin" size={15} /> Đang tải số đo...</p>;

  const submit = async (event) => {
    event.preventDefault();
    const parsed = parseMeasurements(form.height, form.weight);
    if (parsed.error) return setError(parsed.error);
    setSaving(true);
    setError("");
    try {
      const saved = await sizeProfileApi.saveMeasurements(parsed);
      setForm({ height: saved.heightCm ?? "", weight: saved.weightKg ?? "" });
      notification.success("Đã lưu số đo. Trang sản phẩm sẽ gợi ý size theo số đo này.");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="customer-profile-measurements" onSubmit={submit} noValidate>
      <label><span>Chiều cao (cm)</span>
        <input type="number" inputMode="numeric" min="80" max="250" value={form.height}
          onChange={(e) => setForm((current) => ({ ...current, height: e.target.value }))} placeholder="Chưa cập nhật" />
      </label>
      <label><span>Cân nặng (kg)</span>
        <input type="number" inputMode="decimal" min="20" max="250" step="0.1" value={form.weight}
          onChange={(e) => setForm((current) => ({ ...current, weight: e.target.value }))} placeholder="Chưa cập nhật" />
      </label>
      <Button type="submit" variant="unstyled" className="customer-profile-measurements__save" loading={saving}>
        <Save size={14} /> Lưu số đo
      </Button>
      {error && <p className="checkout-error">{error}</p>}
    </form>
  );
}
