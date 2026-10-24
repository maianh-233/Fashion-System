import { forwardRef, useEffect, useState } from "react";
import { ImageOff, Plus, ScanLine, Search } from "lucide-react";
import Button from "../../common/Button";
import { orderApi } from "../../../api/admin/orderApi";
import { money } from "../../../pages/admin/offlinePosLogic";

/**
 * Chọn sản phẩm theo 2 cách: nhập/quét SKU (Enter để thêm) hoặc tìm theo tên rồi chọn biến thể.
 * Máy quét mã vạch dạng bàn phím gõ mã + Enter nên dùng được ngay với ô SKU.
 */
const PosProductPicker = forwardRef(function PosProductPicker({ storeId, disabled, onAdd }, scanRef) {
  const [code, setCode] = useState("");
  const [scanError, setScanError] = useState("");
  const [scanning, setScanning] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [products, setProducts] = useState(null);
  const [searchError, setSearchError] = useState("");
  const [active, setActive] = useState(null);
  const [variants, setVariants] = useState([]);
  const [quantities, setQuantities] = useState({});
  const [variantsLoading, setVariantsLoading] = useState(false);

  const submitCode = async event => {
    event.preventDefault();
    const value = code.trim();
    if (!value || scanning) return;
    setScanning(true);
    setScanError("");
    try {
      const variant = await orderApi.variantByCode(storeId, value);
      onAdd(variant, 1);
      setCode("");
    } catch (error) {
      setScanError(error.message);
    } finally {
      setScanning(false);
      scanRef?.current?.focus();
    }
  };

  useEffect(() => {
    const value = keyword.trim();
    if (!storeId || value.length < 2) {
      const reset = setTimeout(() => { setProducts(null); setSearchError(""); }, 0);
      return () => clearTimeout(reset);
    }
    const controller = new AbortController();
    const timer = setTimeout(() => {
      orderApi.products({ storeId, keyword: value, size: 12 }, { signal: controller.signal })
        .then(page => { setProducts(page.content || []); setSearchError(""); })
        .catch(error => { if (!controller.signal.aborted) setSearchError(error.message); });
    }, 300);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [keyword, storeId]);

  const openProduct = async product => {
    if (active?.productId === product.productId) { setActive(null); return; }
    setActive(product);
    setVariants([]);
    setVariantsLoading(true);
    try {
      setVariants(await orderApi.productVariants(product.productId, storeId));
    } catch (error) {
      setSearchError(error.message);
    } finally {
      setVariantsLoading(false);
    }
  };

  const addVariant = (variant, row) => {
    const quantity = Math.max(1, Math.min(Number(quantities[variant.variantId] || 1), variant.stock));
    onAdd({ ...variant, productName: row.name }, quantity);
    setQuantities(current => ({ ...current, [variant.variantId]: 1 }));
    scanRef?.current?.focus();
  };

  return <section className="pos-card">
    <div className="pos-card__title"><span><ScanLine size={18} /> Thêm sản phẩm</span><small>Quét mã vạch hoặc gõ SKU rồi nhấn Enter</small></div>
    <form className="pos-scan" onSubmit={submitCode}>
      <input ref={scanRef} className="pos-input" value={code} disabled={disabled} autoFocus autoComplete="off"
        placeholder="Mã SKU / mã vạch" aria-label="Mã SKU hoặc mã vạch"
        onChange={event => { setCode(event.target.value); setScanError(""); }} />
      <Button type="submit" variant="primary" loading={scanning} disabled={disabled || !code.trim()}>Thêm</Button>
    </form>
    {scanError && <p className="pos-error mt-2">{scanError}</p>}

    <div className="relative mt-4">
      <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 opacity-60" />
      <input className="pos-input" style={{ paddingLeft: "2.2rem" }} value={keyword} disabled={disabled}
        placeholder="Tìm theo tên hoặc mã sản phẩm (từ 2 ký tự)" aria-label="Tìm sản phẩm theo tên"
        onChange={event => { setKeyword(event.target.value); setActive(null); }} />
    </div>
    {searchError && <p className="pos-error mt-2">{searchError}</p>}
    {products && !products.length && <p className="pos-muted mt-3">Không có sản phẩm còn hàng phù hợp tại cửa hàng.</p>}
    {products?.length > 0 && <div className="pos-products">
      {products.map(product => <button type="button" key={product.productId} onClick={() => openProduct(product)}
        className={`pos-product ${active?.productId === product.productId ? "is-active" : ""}`}>
        <div className="flex items-center gap-2">
          {product.imageUrl ? <img src={product.imageUrl} alt="" className="pos-thumb" /> : <span className="pos-thumb grid place-items-center"><ImageOff size={16} /></span>}
          <strong>{product.name}</strong>
        </div>
        <span className="pos-muted">{product.minPrice === product.maxPrice ? money(product.minPrice) : `${money(product.minPrice)} – ${money(product.maxPrice)}`}</span>
        <span className="pos-muted">{product.variantCount} biến thể · tồn {product.stock}</span>
      </button>)}
    </div>}

    {active && <div className="pos-variants">
      <p className="pos-muted">Biến thể còn hàng của <strong>{active.name}</strong></p>
      {variantsLoading && <p className="pos-muted">Đang tải biến thể…</p>}
      {!variantsLoading && !variants.length && <p className="pos-muted">Sản phẩm đã hết hàng tại cửa hàng.</p>}
      {variants.map(variant => <div key={variant.variantId} className="pos-variant">
        <div className="min-w-0">
          <strong className="text-sm">{[variant.color, variant.size].filter(Boolean).join(" / ") || variant.sku}</strong>
          <p className="pos-muted">{variant.sku} · tồn {variant.stock}</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold">{money(variant.salePrice ?? variant.price)}</span>
          <input type="number" min={1} max={variant.stock} className="pos-input" style={{ width: "4.5rem" }}
            aria-label={`Số lượng ${variant.sku}`} value={quantities[variant.variantId] ?? 1}
            onChange={event => setQuantities(current => ({ ...current, [variant.variantId]: event.target.value }))} />
          <Button type="button" variant="secondary" size="sm" onClick={() => addVariant(variant, active)}><Plus size={14} /> Thêm</Button>
        </div>
      </div>)}
    </div>}
  </section>;
});

export default PosProductPicker;
