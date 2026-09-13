import { useEffect, useMemo, useState } from "react";
import { Boxes, Search } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import ApiCatalogPage from "../../../components/admin/catalog/ApiCatalogPage";
import { productApi, variantApi } from "../../../hooks/catalogApi";
import { toCatalogOptions } from "../../../hooks/catalogQuery";
import ProductImageSection from "../../../components/admin/Product/ProductImageSection";

const columns = [
  { key: "sku", label: "SKU" }, { key: "barcode", label: "Barcode" },
  { key: "color", label: "Màu" }, { key: "size", label: "Kích thước" },
  { key: "price", label: "Giá", render: (row) => Number(row.price).toLocaleString("vi-VN") },
  { key: "active", label: "Trạng thái", render: (row) => row.active ? "Hoạt động" : "Ngừng hoạt động" },
];
const fields = [
  { key: "sku", label: "SKU", required: true }, { key: "barcode", label: "Barcode" },
  { key: "color", label: "Màu" }, { key: "size", label: "Kích thước" },
  { key: "price", label: "Giá", required: true, type: "number" },
  { key: "salePrice", label: "Giá khuyến mãi", type: "number" },
  { key: "weight", label: "Khối lượng", type: "number" },
  { key: "active", label: "Trạng thái", type: "select", options: [{ value: "true", label: "Hoạt động" }, { value: "false", label: "Ngừng hoạt động" }] },
];
const permissions = { create: "PRODUCT_VARIANT_CREATE", update: "PRODUCT_VARIANT_UPDATE", delete: "PRODUCT_VARIANT_DELETE" };

export default function VariantManagement() {
  const [params, setParams] = useSearchParams();
  const productId = params.get("productId") || "";
  const [productKeyword, setProductKeyword] = useState("");
  const [products, setProducts] = useState([]);
  const [productLoading, setProductLoading] = useState(true);
  const [productError, setProductError] = useState("");

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
    list: (query, options) => variantApi.list(productId, query, options),
    create: (body) => variantApi.create(productId, body),
    update: (id, body) => variantApi.update(productId, id, body),
    remove: (id) => variantApi.remove(productId, id),
  }), [productId]);

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

  if (!productId) return <div className="admin-catalog-page space-y-5 text-zinc-100">
    <div><h1 className="text-2xl font-semibold">Quản lý biến thể sản phẩm</h1><p className="mt-1 text-zinc-400">Tìm và chọn một Product để xem danh sách SKU, màu sắc, kích thước và giá.</p></div>
    {productPicker}
  </div>;

  return <div className="space-y-5">
    {productPicker}
    <ApiCatalogPage title="Biến thể sản phẩm" description={selectedProduct ? `SKU của ${selectedProduct.name}.` : `Variant của Product ${productId}.`} icon={Boxes} api={api} permissions={permissions} columns={columns} fields={fields} initialValues={{ active: "true" }} normalize={(value) => ({ ...value, active: value.active === true || value.active === "true", salePrice: value.salePrice === "" ? null : value.salePrice, weight: value.weight === "" ? null : value.weight })} renderDetails={(variant, mode) => <ProductImageSection mode={mode} productId={productId} variantId={variant.id} />} />
  </div>;
}
