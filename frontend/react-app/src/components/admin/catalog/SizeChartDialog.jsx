import { useEffect, useState } from "react";
import { ArrowDown, ArrowUp, Plus, Trash2 } from "lucide-react";
import Button from "../../common/Button";
import { useSystemNotification } from "../../common/SystemNotification";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import { categoryApi } from "../../../api/admin/catalogApi";
import { SIZE_CHART_PRESET, chartPayload, emptyChartRow, rowsFromChart, validateChartRows } from "./sizeChartLogic";

const NUMBER_FIELDS = [
  ["minHeightCm", "Cao từ"], ["maxHeightCm", "Cao đến"], ["minWeightKg", "Nặng từ"], ["maxWeightKg", "Nặng đến"],
];

/**
 * Bảng size của một danh mục (từ nhỏ đến lớn). Danh mục con không có bảng riêng dùng bảng của danh mục cha gần nhất;
 * lưu bảng rỗng để quay lại dùng bảng cha.
 */
export default function SizeChartDialog({ category, onClose }) {
  const notification = useSystemNotification();
  const [rows, setRows] = useState(null);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    categoryApi.sizeChart(category.id, { signal: controller.signal })
      .then((chart) => setRows(rowsFromChart(chart)))
      .catch((failure) => {
        if (!controller.signal.aborted) {
          setRows([]);
          setError(failure.message);
        }
      });
    return () => controller.abort();
  }, [category.id]);

  const update = (index, field, value) => setRows((current) => current.map((row, i) => (i === index ? { ...row, [field]: value } : row)));
  const move = (index, delta) => setRows((current) => {
    const next = [...current];
    const target = index + delta;
    if (target < 0 || target >= next.length) return current;
    [next[index], next[target]] = [next[target], next[index]];
    return next;
  });

  const save = async () => {
    const problem = validateChartRows(rows);
    if (problem) return setError(problem);
    setSaving(true);
    setError("");
    try {
      const saved = await categoryApi.saveSizeChart(category.id, chartPayload(rows));
      setRows(rowsFromChart(saved));
      notification.success(saved.entries.length ? `Đã lưu bảng size cho ${category.name}.` : `Đã xóa bảng size riêng của ${category.name}.`);
      onClose();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdminDialog open onClose={onClose} size="lg">
      <AdminDialogHeader title={`Bảng size · ${category.name}`}
        description="Khoảng chiều cao (cm) và cân nặng (kg) cho từng size, xếp từ nhỏ đến lớn. Ô trống là không xét. Danh mục con không có bảng riêng sẽ dùng bảng này."
        onClose={onClose} />
      <AdminDialogBody className="space-y-3 text-sm">
        {rows === null ? <p className="text-zinc-400">Đang tải...</p> : (
          <>
            {rows.length === 0 && (
              <div className="flex flex-wrap items-center gap-3 rounded-xl border border-dashed border-zinc-700 p-4 text-zinc-400">
                <span>Chưa có bảng size riêng.</span>
                <Button type="button" variant="secondary" className="rounded-lg px-3 py-1.5 text-xs" onClick={() => setRows(SIZE_CHART_PRESET.map((row) => ({ ...row })))}>
                  Dùng bảng mẫu áo (S–XL)
                </Button>
              </div>
            )}
            {rows.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[640px] text-left">
                  <thead className="text-xs text-zinc-400">
                    <tr><th className="py-2 pr-2">Size</th>{NUMBER_FIELDS.map(([, label]) => <th key={label} className="py-2 pr-2">{label}</th>)}<th className="py-2" /></tr>
                  </thead>
                  <tbody>
                    {rows.map((row, index) => (
                      <tr key={row.key} className="border-t border-zinc-800">
                        <td className="py-2 pr-2">
                          <input aria-label={`Tên size dòng ${index + 1}`} className="w-20 rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-1.5" maxLength={50}
                            value={row.size} onChange={(e) => update(index, "size", e.target.value)} />
                        </td>
                        {NUMBER_FIELDS.map(([field, label]) => (
                          <td key={field} className="py-2 pr-2">
                            <input aria-label={`${label} dòng ${index + 1}`} type="number" min="0" step={field.includes("Weight") ? "0.1" : "1"}
                              className="w-24 rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-1.5"
                              value={row[field]} onChange={(e) => update(index, field, e.target.value)} />
                          </td>
                        ))}
                        <td className="py-2">
                          <div className="flex items-center gap-2">
                            <Button type="button" title="Lên" onClick={() => move(index, -1)} disabled={index === 0} className="text-zinc-400"><ArrowUp size={15} /></Button>
                            <Button type="button" title="Xuống" onClick={() => move(index, 1)} disabled={index === rows.length - 1} className="text-zinc-400"><ArrowDown size={15} /></Button>
                            <Button type="button" title="Xóa dòng" onClick={() => setRows((current) => current.filter((_, i) => i !== index))} className="text-red-400"><Trash2 size={15} /></Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <Button type="button" variant="secondary" className="inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs"
              onClick={() => setRows((current) => [...current, emptyChartRow()])} disabled={rows.length >= 20}>
              <Plus size={14} /> Thêm size
            </Button>
          </>
        )}
        {error && <p className="text-red-400">{error}</p>}
      </AdminDialogBody>
      <AdminDialogFooter>
        <Button type="button" variant="secondary" className="rounded-xl px-4 py-2.5 text-sm" onClick={onClose} disabled={saving}>Hủy</Button>
        <Button type="button" variant="primary" permission="CATEGORY_UPDATE" className="rounded-xl px-4 py-2.5 text-sm"
          onClick={save} loading={saving} disabled={rows === null}>Lưu bảng size</Button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
