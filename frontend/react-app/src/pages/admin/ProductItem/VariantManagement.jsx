import { useEffect, useMemo, useState } from "react";
import { Boxes, QrCode, Search } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { productApi, variantApi } from "../../../api/catalogApi";
import { toCatalogOptions } from "../../../utils/catalogQuery";
import ProductImageSection from "../../../components/admin/Product/ProductImageSection";
import VariantQrDialog from "../../../components/admin/catalog/VariantQrDialog";
import Button from "../../../components/common/Button";

const columns = [
  { key: "productName", label: "Sản phẩm" }, { key: "productCode", label: "Mã sản phẩm" },
  { key: "sku", label: "SKU" }, { key: "imageUrl", label: "Ảnh", render: (row) => row.imageUrl ? <img src={row.imageUrl} alt={row.sku} className="h-10 w-10 rounded-lg object-cover" /> : "—" }, { key: "barcode", label: "Barcode" },
  { key: "color", label: "Màu" }, { key: "size", label: "Kích thước" },
  { key: "price", label: "Giá", render: (row) => Number(row.price).toLocaleString("vi-VN") },
  { key: "salePrice", label: "Giá khuyến mãi", render: (row) => row.salePrice == null ? "—" : Number(row.salePrice).toLocaleString("vi-VN") },
  { key: "active", label: "Trạng thái", render: (row) => row.active ? "Hoạt động" : "Ngừng hoạt động" },
];
const fields = [
  { key: "color", label: "Màu" }, { key: "size", label: "Kích thước" },
  { key: "sku", label: "SKU", generated: true }, { key: "barcode", label: "Barcode" },
  { key: "price", label: "Giá", required: true, type: "number" },
  { key: "salePrice", label: "Giá khuyến mãi", type: "number" },
  { key: "weight", label: "Khối lượng", type: "number" },
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
  const [params, setParams] = useSearchParams();
  const productId = params.get("productId") || "";
  const [productKeyword, setProductKeyword] = useState("");
  const [products, setProducts] = useState([]);
  const [productLoading, setProductLoading] = useState(true);
  const [productError, setProductError] = useState("");
  const [qrVariant, setQrVariant] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      setProductLoading(true);
      setProductError("");
      try {
        const [page, selected] = await Promise.all([
          productApi.list({
            keyword: productKeyword.trim(),
            page: 0,
            size: 50,
            sort: "name,asc",
          }, { signal: controller.signal }),
          productId
            ? productApi.detail(productId, { signal: controller.signal })
            : Promise.resolve(null),
        ]);
        const items = page?.content || [];
        setProducts(selected && !items.some((item) => item.id === selected.id)
          ? [selected, ...items]
          : items);
      } catch (requestError) {
        if (requestError.name !== "AbortError") {
          setProducts([]);
          setProductError(requestError.message || "Không thể tải danh sách Product.");
        }
      } finally {
        if (!controller.signal.aborted) setProductLoading(false);
      }
    }, 250);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [productId, productKeyword]);

  const productOptions = useMemo(() => toCatalogOptions(products), [products]);
  const selectedProduct = products.find((product) => product.id === productId);
  const api = useMemo(() => ({
    list: async (query, options) => {
      if (!productId) return variantApi.listAll(query, options);
      const page = await variantApi.list(productId, query, options);
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
  }), [productId, selectedProduct?.name, selectedProduct?.code]);

  const productPicker = <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-4 text-zinc-100">
    <div className="mb-3 flex items-center gap-2 text-sm font-medium text-zinc-300"><Search size={17} /> Chọn Product để quản lý biến thể</div>
    <div className="grid gap-3 md:grid-cols-2">
      <input
        value={productKeyword}
        onChange={(event) => setProductKeyword(event.target.value)}
        placeholder="Tìm Product theo tên hoặc slug…"
        className="h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-4"
      />
      <select
        value={productId}
        disabled={productLoading && products.length === 0}
        onChange={(event) => setParams(event.target.value ? { productId: event.target.value } : {})}
        className="h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-4"
      >
        <option value="">-- Chọn Product --</option>
        {productOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
    </div>
    {productLoading && <p className="mt-3 text-sm text-zinc-500">Đang tải Product…</p>}
    {productError && <p className="mt-3 text-sm text-red-300">{productError}</p>}
  </section>;

  return <div className="space-y-5">
    {productPicker}
    <ApiCatalogPage key={productId || "all-products"} validate={(value, rows, mode) => validateVariant({ ...value, productId: value.productId || productId }, rows, mode)} statusParam="active" statusOptions={[{ value: "false", label: "Ngừng hoạt động" }]} title="Biến thể sản phẩm" description={selectedProduct ? `SKU của ${selectedProduct.name} (${selectedProduct.code || ""}).` : "Toàn bộ biến thể trong catalog."} icon={Boxes} api={api} permissions={permissions} columns={columns} fields={fields} canCreate={Boolean(productId)} sort={productId ? "sku,asc" : "productId,asc"} initialValues={{ active: "true" }} normalize={(value) => ({ ...value, active: value.active === true || value.active === "true", salePrice: value.salePrice === "" ? null : value.salePrice, weight: value.weight === "" ? null : value.weight })} rowClassName={(row, index, rows) => rows.slice(0, index + 1).reduce((group, item, i) => i && item.productId !== rows[i - 1].productId ? group + 1 : group, 0) % 2 ? "bg-zinc-800/30" : ""} renderActions={(row) => <Button permission="PRODUCT_VARIANT_VIEW" type="button" onClick={() => setQrVariant(row)} title="Xem QR"><QrCode size={17} /></Button>} renderDetails={(variant, mode) => <ProductImageSection mode={mode} productId={variant.productId} variantId={variant.id} />} />
    {qrVariant && <VariantQrDialog variant={qrVariant} onClose={() => setQrVariant(null)} />}
  </div>;
}
