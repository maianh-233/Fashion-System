import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, Boxes, QrCode } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { productApi, variantApi } from "../../../api/catalogApi";
import ProductImageSection from "../../../components/admin/Product/ProductImageSection";
import VariantQrDialog from "../../../components/admin/catalog/VariantQrDialog";
import Button from "../../../components/common/Button";

const columns = [
  { key: "imageUrl", label: "Ảnh", render: (row) => row.imageUrl ? <img src={row.imageUrl} alt={row.sku} className="h-10 w-10 rounded-lg object-cover" /> : "—" },
  { key: "sku", label: "SKU" },
  { key: "color", label: "Màu" }, { key: "size", label: "Kích thước" },
  { key: "price", label: "Giá", render: (row) => Number(row.price).toLocaleString("vi-VN") },
  { key: "salePrice", label: "Giá khuyến mãi", render: (row) => row.salePrice == null ? "—" : Number(row.salePrice).toLocaleString("vi-VN") },
  { key: "weight", label: "Khối lượng", render: (row) => row.weight == null ? "—" : `${row.weight} g` },
  { key: "barcode", label: "Barcode" },
  { key: "active", label: "Trạng thái", render: (row) => row.active ? "Hoạt động" : "Ngừng hoạt động" },
];
const fields = [
  { key: "imageUrl", label: "Ảnh biến thể", type: "image", createOnly: true, permission: "PRODUCT_VARIANT_UPDATE" },
  { key: "color", label: "Màu" }, { key: "size", label: "Kích thước" },
  { key: "price", label: "Giá", required: true, type: "number" },
  { key: "salePrice", label: "Giá khuyến mãi", type: "number" },
  { key: "weight", label: "Khối lượng (g)", type: "number" }, { key: "barcode", label: "Barcode" },
  { key: "sku", label: "SKU", generated: true },
  { key: "active", label: "Trạng thái", type: "select", options: [{ value: "true", label: "Hoạt động" }, { value: "false", label: "Ngừng hoạt động" }] },
];
const permissions = { create: "PRODUCT_VARIANT_CREATE", update: "PRODUCT_VARIANT_UPDATE", delete: "PRODUCT_VARIANT_DELETE" };

function validateVariant(value, rows, mode) {
  const price = Number(value.price);
  const salePrice = value.salePrice == null || value.salePrice === "" ? null : Number(value.salePrice);
  if (!Number.isFinite(price) || price < 0 || (salePrice != null && (!Number.isFinite(salePrice) || salePrice < 0 || salePrice > price))) return "Giá bán và giá khuyến mãi không hợp lệ.";
  if (value.weight != null && value.weight !== "" && Number(value.weight) < 0) return "Khối lượng không được âm.";
  const color = String(value.color || "").trim().toLowerCase();
  const size = String(value.size || "").trim().toLowerCase();
  if (rows.some((row) => row.productId === value.productId && row.id !== (mode === "edit" ? value.id : null)
    && String(row.color || "").trim().toLowerCase() === color
    && String(row.size || "").trim().toLowerCase() === size)) return "Biến thể màu sắc và kích thước này đã tồn tại.";
  return "";
}

export default function VariantManagement() {
  const [params] = useSearchParams();
  const productId = params.get("productId") || "";
  const [productState, setProductState] = useState({ productId: "", product: null, error: "" });
  const [qrVariant, setQrVariant] = useState(null);
  const selectedProduct = productState.productId === productId ? productState.product : null;

  useEffect(() => {
    if (!productId) return undefined;
    const controller = new AbortController();
    productApi.detail(productId, { signal: controller.signal })
      .then((product) => {
        if (!controller.signal.aborted) setProductState({ productId, product, error: "" });
      })
      .catch((requestError) => {
        if (requestError.name !== "AbortError") {
          setProductState({ productId, product: null, error: requestError.message || "Không thể tải sản phẩm." });
        }
      });
    return () => controller.abort();
  }, [productId]);

  const api = useMemo(() => ({
    list: async (query, options) => {
      const page = await variantApi.list(productId, query, options);
      console.info("[Biến thể] Kết quả API", { endpoint: `/api/products/${productId}/variants`, query, returned: page.content?.length ?? 0, totalElements: page.totalElements, page: page.page, selectedProduct: selectedProduct?.name });
      return { ...page, content: page.content.map((variant) => ({
        ...variant,
        productName: selectedProduct?.name || variant.productName,
        productCode: selectedProduct?.code || variant.productCode,
      })) };
    },
    create: (body) => variantApi.create(productId, body),
    update: (id, body, row) => variantApi.update(row.productId, id, body),
    remove: (id, row) => variantApi.remove(row.productId, id),
    restore: (id, row) => variantApi.restore(row.productId, id),
    uploadImage: (id, file) => variantApi.uploadImage(productId, id, file),
  }), [productId, selectedProduct?.name, selectedProduct?.code]);

  const backToProducts = <Link to="/admin/products" className="inline-flex items-center gap-2 rounded-xl border border-[#d7bea0] bg-[#fffaf2] px-4 py-2 text-sm font-medium text-[#754b27] hover:bg-[#f8ecda]"><ArrowLeft size={16} /> Quay lại danh sách sản phẩm</Link>;

  if (!productId) return <div className="space-y-5">
    <p role="alert" className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-red-200">Thiếu productId trên URL. Hãy chọn một sản phẩm từ danh sách sản phẩm.</p>
    {backToProducts}
  </div>;

  if (productState.productId !== productId) return <div className="space-y-5">{backToProducts}<p className="text-sm text-zinc-400">Đang tải sản phẩm…</p></div>;

  if (productState.error || !selectedProduct) return <div className="space-y-5">
    <p role="alert" className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-red-200">{productState.error || "Không tìm thấy sản phẩm."}</p>
    {backToProducts}
  </div>;

  return <div className="space-y-5">
    {backToProducts}
    <section className="flex flex-wrap items-center gap-4 rounded-3xl border border-[#e7d7c2] bg-[#fffaf2] p-5 text-[#3d3024] shadow-sm">
      {selectedProduct.imageUrl && <img src={selectedProduct.imageUrl} alt={selectedProduct.name} className="h-16 w-16 rounded-xl object-cover" />}
      <div className="min-w-0">
        <p className="text-xs font-semibold uppercase tracking-wide text-[#a5632f]">Biến thể của sản phẩm</p>
        <h2 className="mt-1 text-xl font-semibold">{selectedProduct.name}</h2>
        <p className="mt-1 text-sm text-[#765f4d]">Mã: {selectedProduct.code || "—"} · Trạng thái: {selectedProduct.status || "—"}</p>
      </div>
    </section>
    <ApiCatalogPage key={productId} validate={(value, rows, mode) => validateVariant({ ...value, productId: value.productId || productId }, rows, mode)} statusParam="active" statusOptions={[{ value: "false", label: "Ngừng hoạt động" }]} title="Biến thể sản phẩm" description={`SKU của ${selectedProduct.name} (${selectedProduct.code || ""}).`} icon={Boxes} api={api} permissions={permissions} columns={columns} fields={fields} dialogMaxWidth="max-w-4xl" detailsPosition="before" continueEditingAfterCreate sort="sku,asc" initialValues={{ active: "true" }} normalize={(value) => ({ ...value, sku: value.sku || "AUTO", active: value.active === true || value.active === "true", salePrice: value.salePrice === "" ? null : value.salePrice, weight: value.weight === "" ? null : value.weight })} rowClassName={(row, index, rows) => rows.slice(0, index + 1).reduce((group, item, i) => i && item.productId !== rows[i - 1].productId ? group + 1 : group, 0) % 2 ? "bg-zinc-800/30" : ""} renderActions={(row) => <Button permission="PRODUCT_VARIANT_VIEW" type="button" onClick={() => setQrVariant(row)} title="Xem QR"><QrCode size={17} /></Button>} renderDetails={(variant, mode, reload) => <ProductImageSection key={variant.id} mode={mode} imageUrl={variant.imageUrl} productId={variant.productId} variantId={variant.id} onImageChange={reload} />} />
    {qrVariant && <VariantQrDialog variant={qrVariant} onClose={() => setQrVariant(null)} />}
  </div>;
}
