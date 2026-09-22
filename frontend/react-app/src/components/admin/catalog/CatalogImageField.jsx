import { useEffect, useMemo } from "react";
import { ImagePlus, X } from "lucide-react";
import { validateCatalogImage } from "./catalogFieldLogic";

export default function CatalogImageField({ file, currentUrl, disabled, onChange }) {
  const preview = useMemo(() => file ? URL.createObjectURL(file) : null, [file]);

  useEffect(() => () => {
    if (preview) URL.revokeObjectURL(preview);
  }, [preview]);

  const selectFile = (event) => {
    const nextFile = event.target.files?.[0] || null;
    const error = validateCatalogImage(nextFile);
    onChange(error ? null : nextFile, error);
    event.target.value = "";
  };

  return <div className="space-y-3">
    {(preview || currentUrl) ? <img src={preview || currentUrl} alt="Xem trước ảnh catalog" className="h-32 w-32 rounded-xl border border-zinc-700 object-cover" /> : <div className="flex h-32 w-32 items-center justify-center rounded-xl border border-dashed border-zinc-600 bg-zinc-800 text-zinc-500"><ImagePlus size={28} /></div>}
    {!disabled && <div className="flex flex-wrap items-center gap-2">
      <span className="cursor-pointer rounded-xl border border-zinc-600 bg-zinc-800 px-3 py-2 text-sm text-zinc-200 hover:bg-zinc-700">
        Chọn ảnh
        <input className="sr-only" type="file" accept="image/jpeg,image/png,image/gif,image/webp" onChange={selectFile} />
      </span>
      {file && <button type="button" onClick={() => onChange(null, "")} className="inline-flex items-center gap-1 rounded-xl px-2 py-2 text-sm text-zinc-300 hover:bg-zinc-800"><X size={16} /> Xóa ảnh đã chọn</button>}
    </div>}
    <p className="text-xs text-zinc-500">JPEG, PNG, GIF hoặc WebP · tối đa 25 MB</p>
  </div>;
}
