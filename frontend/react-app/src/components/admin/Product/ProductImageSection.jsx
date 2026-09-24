import Button from "../../common/Button";
import { ImagePlus, ImageIcon, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import { requestAdmin } from "../../../api/auth/adminSession";

export default function ProductImageSection({ mode, imageUrl, productId, variantId, onImageChange }) {
  const isView = mode === "view";
  const { isGlobal, hasPermission } = useAdminPermissions();
  const canManageImages = isGlobal && hasPermission("PRODUCT_VARIANT_UPDATE");

  const [preview, setPreview] = useState(imageUrl || null);
  const [image, setImage] = useState(null);
  const [fileName, setFileName] = useState("");
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!productId || !variantId) return undefined;
    const controller = new AbortController();
    requestAdmin(`/api/products/${productId}/variants/${variantId}/images`, { signal: controller.signal })
      .then((items) => {
        const primary = items.find((item) => item.isPrimary) || items[0] || null;
        setImage(primary);
        setPreview(primary?.imageUrl || imageUrl || null);
      })
      .catch((requestError) => {
        if (requestError.name !== "AbortError") setError(requestError.message);
      });
    return () => controller.abort();
  }, [imageUrl, productId, variantId]);

  const handleChangeImage = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setFileName(file.name);
    setError("");
    if (!productId || !variantId) {
      setError("Hãy lưu sản phẩm và biến thể trước khi tải ảnh lên.");
      return;
    }
    setUploading(true);
    try {
      const form = new FormData();
      form.append("file", file);
      const imagePath = `/api/products/${productId}/variants/${variantId}/images`;
      const uploaded = image
        ? await requestAdmin(`${imagePath}/${image.id}/content`, { method: "PUT", body: form })
        : await requestAdmin(`${imagePath}?isPrimary=true`, { method: "POST", body: form });
      setImage(uploaded);
      setPreview(uploaded.imageUrl);
      await onImageChange?.();
    } catch (requestError) {
      setError(requestError.message || "Tải ảnh lên Cloudinary thất bại.");
    } finally {
      setUploading(false);
    }
  };

  const handleRemoveImage = async () => {
    if (!image || !productId || !variantId) return;
    setUploading(true);
    setError("");
    try {
      await requestAdmin(`/api/products/${productId}/variants/${variantId}/images/${image.id}`, { method: "DELETE" });
      setImage(null);
      setPreview(null);
      setFileName("");
      await onImageChange?.();
    } catch (requestError) {
      setError(requestError.message || "Xóa ảnh Cloudinary thất bại.");
    } finally {
      setUploading(false);
    }
  };

  return (
    <section className="mb-5 rounded-xl border border-gray-800 bg-[#171717] p-6 shadow-lg">
      <h3 className="text-lg font-semibold text-orange-400 mb-2">
        Ảnh biến thể
      </h3>
      <p className="mb-5 text-sm text-gray-400">{isView ? "Ảnh đại diện của biến thể này." : "Mỗi biến thể sử dụng một ảnh đại diện. Chọn ảnh mới để thay ảnh hiện tại."}</p>

      <div className="flex flex-col lg:flex-row gap-8">
        {/* IMAGE */}
        <div className="relative group h-56 w-56 max-w-full shrink-0 rounded-xl overflow-hidden border-2 border-dashed border-gray-700 bg-[#1d1d1d]">

          {preview ? (
            <>
              <img
                src={preview}
                alt="preview"
                className="w-full h-full object-cover transition duration-300 group-hover:scale-105"
              />

              {!isView && (
                <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition flex items-center justify-center">
                  <span className="text-white font-medium">
                    Đổi hình ảnh
                  </span>
                </div>
              )}
            </>
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center text-gray-500">
              <ImageIcon size={70} strokeWidth={1.3} />
              <p className="mt-3 text-sm">
                Chưa có hình ảnh
              </p>
            </div>
          )}
        </div>

        {/* RIGHT */}
        {!isView && canManageImages && (
          <div className="flex flex-col justify-center gap-4">

            <label className="cursor-pointer inline-flex items-center justify-center gap-2 rounded-lg bg-orange-500 hover:bg-orange-600 px-5 py-3 font-medium text-black transition active:scale-95">
              <ImagePlus size={20} />
              {image ? "Thay ảnh" : "Chọn ảnh"}

              <input
                hidden
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                onChange={handleChangeImage}
                disabled={uploading}
              />
            </label>

            {image && (
              <Button
                permission="PRODUCT_VARIANT_UPDATE"
                onClick={handleRemoveImage}
                disabled={uploading}
                className="inline-flex items-center justify-center gap-2 rounded-lg bg-red-600 hover:bg-red-700 px-5 py-3 text-white transition active:scale-95"
              >
                <Trash2 size={18} />
                Xóa ảnh
              </Button>
            )}

            <div className="text-sm text-gray-400">
              {fileName
                ? `${uploading ? "Đang tải" : "Đã tải"}: ${fileName}`
                : "PNG, JPG, JPEG (khuyến nghị 800×800)"}
            </div>
          </div>
        )}
      </div>
      {error && <p role="alert" className="mt-4 max-w-sm text-sm text-red-300">{error}</p>}
    </section>
  );
}
