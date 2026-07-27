import { ImagePlus, ImageIcon, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";

export default function ProductVariantImageSection({
  mode,
  imageUrl,
}) {
  const isView = mode === "view";

  const [preview, setPreview] = useState(imageUrl || null);
  const [fileName, setFileName] = useState("");

  useEffect(() => {
    setPreview(imageUrl || null);
  }, [imageUrl]);

  const handleChangeImage = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setPreview(URL.createObjectURL(file));
    setFileName(file.name);

    // TODO:
    // upload file
  };

  const handleRemoveImage = () => {
    setPreview(null);
    setFileName("");
  };

  return (
    <section className="rounded-xl border border-gray-800 bg-[#171717] p-6 shadow-lg">

      <h3 className="text-lg font-semibold text-orange-400 mb-6">
        Hình ảnh biến thể
      </h3>

      <div className="flex flex-col lg:flex-row gap-8">

        <div className="relative group w-72 h-72 rounded-xl overflow-hidden border-2 border-dashed border-gray-700 bg-[#1d1d1d]">

          {preview ? (
            <>
              <img
                src={preview}
                alt="Variant"
                className="w-full h-full object-cover"
              />

              {!isView && (
                <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition flex items-center justify-center">
                  <span>Đổi hình ảnh</span>
                </div>
              )}
            </>
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center text-gray-500">
              <ImageIcon size={70} />
              <p className="mt-3">
                Chưa có hình ảnh
              </p>
            </div>
          )}

        </div>

        {!isView && (
          <div className="flex flex-col justify-center gap-4">

            <label className="cursor-pointer inline-flex items-center justify-center gap-2 rounded-lg bg-orange-500 hover:bg-orange-600 px-5 py-3 font-medium text-black">

              <ImagePlus size={20} />

              Chọn ảnh

              <input
                hidden
                type="file"
                accept="image/*"
                onChange={handleChangeImage}
              />

            </label>

            {preview && (
              <button
                onClick={handleRemoveImage}
                className="inline-flex items-center justify-center gap-2 rounded-lg bg-red-600 hover:bg-red-700 px-5 py-3"
              >
                <Trash2 size={18} />
                Xóa ảnh
              </button>
            )}

            <div className="text-sm text-gray-400">
              {fileName
                ? `Đã chọn: ${fileName}`
                : "PNG, JPG, JPEG (800×800)"}
            </div>

          </div>
        )}

      </div>

    </section>
  );
}