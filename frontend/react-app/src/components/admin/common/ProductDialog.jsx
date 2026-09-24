import { useEffect, useState } from "react";
import Button from "../../common/Button";
import CatalogImageField from "../catalog/CatalogImageField";
import CatalogNoteField from "../catalog/CatalogNoteField";
import { isCatalogFieldVisible } from "../catalog/catalogPageLogic";
import { productDialogFields } from "./productDialogConfig";

function ProductCatalogSelect({ field, value, onChange, disabled }) {
  const [keyword, setKeyword] = useState("");
  const [options, setOptions] = useState([]);

  useEffect(() => {
    if (disabled) return undefined;
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      field.api.list({ keyword, status: "ACTIVE", active: true, size: 30, page: 0, sort: "name,asc" },
        { signal: controller.signal }).then((page) => setOptions(page?.content || []))
        .catch(() => setOptions([]));
    }, 250);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [disabled, field.api, keyword]);

  useEffect(() => {
    if (!value || options.some((item) => item.id === value)) return undefined;
    const controller = new AbortController();
    field.api.detail(value, { signal: controller.signal }).then((item) =>
      setOptions((current) => current.some((option) => option.id === item.id) ? current : [item, ...current]))
      .catch(() => {});
    return () => controller.abort();
  }, [field.api, options, value]);

  return <div className="space-y-2">
    {!disabled && <input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder={`Tìm ${field.label.toLowerCase()}…`} className="h-10 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" />}
    <select disabled={disabled} value={value || ""} onChange={(event) => onChange(event.target.value || null)} className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3">
      <option value="">-- Chọn --</option>
      {options.map((item) => <option key={item.id} value={item.id}>{item.code ? `${item.code} - ` : ""}{item.name}</option>)}
    </select>
  </div>;
}

export default function ProductDialog({
  mode,
  product,
  onProductChange = () => {},
  onImageChange = () => {},
  onSubmit,
  onClose,
  uploading = false,
  imageError = "",
  permissions = {},
  hasPermission = () => true,
  renderDetails,
}) {
  const title = mode === "create" ? "Thêm mới" : mode === "edit" ? "Chỉnh sửa" : "Chi tiết";

  return <div className="fixed inset-0 z-[110] flex items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true" aria-label={`${title} Sản phẩm`}>
    <div className="max-h-[90vh] w-full max-w-5xl overflow-y-auto rounded-3xl border border-zinc-700 bg-zinc-900 p-6">
      <div className="mb-5 flex items-center justify-between gap-4">
        <h2 className="text-xl font-semibold">{title} Sản phẩm</h2>
        <Button type="button" onClick={onClose}>Đóng</Button>
      </div>
      <form onSubmit={onSubmit}>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {productDialogFields.filter((field) => isCatalogFieldVisible(field, mode, hasPermission)).map((field) => <label key={field.key} className={`min-w-0 space-y-1 text-sm ${field.fullWidth || ["textarea", "image"].includes(field.type) ? "md:col-span-2" : ""}`}>
            <span className="text-zinc-300">{field.label}</span>
            {field.type === "image" ? <CatalogImageField file={product.imageFile} currentUrl={product[field.key]} disabled={mode === "view"} onChange={onImageChange} />
              : field.type === "catalog" ? <ProductCatalogSelect field={field} value={product[field.key]} disabled={mode === "view"} onChange={(value) => onProductChange((current) => ({ ...current, [field.key]: value }))} />
                : field.type === "select" ? <select disabled={mode === "view" || field.generated} required={field.required} value={product[field.key] ?? ""} onChange={(event) => onProductChange((current) => ({ ...current, [field.key]: event.target.value || null }))} className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3"><option value="">-- Chọn --</option>{field.options?.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>
                  : field.type === "textarea" ? <CatalogNoteField disabled={mode === "view" || field.generated} required={field.required} value={product[field.key]} onChange={(note) => onProductChange((current) => ({ ...current, [field.key]: note }))} />
                    : <input disabled={mode === "view" || field.generated} required={field.required} type={field.type || "text"} value={product[field.key] ?? ""} onChange={(event) => onProductChange((current) => ({ ...current, [field.key]: field.type === "number" ? (event.target.value === "" ? "" : Number(event.target.value)) : event.target.value }))} className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" />}
          </label>)}
        </div>
        {imageError && <p className="mt-3 text-sm text-red-300">{imageError}</p>}
        {mode !== "view" && <div className="mt-6 flex justify-end"><Button permission={mode === "create" ? permissions.create : permissions.update} type="submit" loading={uploading} className="rounded-xl bg-amber-500 px-5 py-2.5 text-black">{uploading ? "Đang lưu và tải ảnh…" : "Lưu dữ liệu"}</Button></div>}
      </form>
      {product.id && renderDetails?.(product, mode)}
    </div>
  </div>;
}
