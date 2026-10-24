import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Store } from "lucide-react";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import StoreSelector from "../../components/common/StoreSelector";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import useStoreOptions from "../../hooks/useStoreOptions";
import { orderApi } from "../../api/admin/orderApi";
import PosProductPicker from "../../components/admin/OrderPos/PosProductPicker";
import PosCart from "../../components/admin/OrderPos/PosCart";
import PosCustomerPanel from "../../components/admin/OrderPos/PosCustomerPanel";
import PosVoucherPanel from "../../components/admin/OrderPos/PosVoucherPanel";
import PosCheckout from "../../components/admin/OrderPos/PosCheckout";
import { addToCart, cartRequest, removeLine, setQuantity, toggleVoucher } from "./offlinePosLogic";
import "../../components/admin/OrderPos/OrderPos.css";

const QUOTE_DEBOUNCE_MS = 300;

/**
 * Bán tại quầy. Giỏ hàng chỉ giữ biến thể + số lượng; mỗi khi giỏ, khách, voucher hoặc tiền khách đưa
 * thay đổi, trang gọi lại API quote/voucher (debounce 300ms) và chỉ hiển thị số tiền backend trả về.
 */
export default function OfflineOrderPos() {
  const notification = useSystemNotification();
  const { isGlobal, currentStoreId, currentStoreName, hasPermission, loading: permissionsLoading } = useAdminPermissions();
  const { stores, loading: storesLoading, error: storesError } = useStoreOptions({ endpoint: "/api/admin/orders/stores", enabled: !permissionsLoading });
  const [storeId, setStoreId] = useState("");
  const [cart, setCart] = useState([]);
  const [customer, setCustomer] = useState(null);
  const [selectedCodes, setSelectedCodes] = useState([]);
  const [cash, setCash] = useState("");
  const [note, setNote] = useState("");
  const [quote, setQuote] = useState(null);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [quoteError, setQuoteError] = useState("");
  const [vouchers, setVouchers] = useState([]);
  const [vouchersLoading, setVouchersLoading] = useState(false);
  const [vouchersError, setVouchersError] = useState("");
  const [creating, setCreating] = useState(false);
  const [lastCreated, setLastCreated] = useState(null);
  const scanRef = useRef(null);

  const effectiveStoreId = isGlobal ? storeId : currentStoreId || stores[0]?.id || "";
  const storeName = isGlobal ? stores.find(s => s.id === storeId)?.name : currentStoreName || stores[0]?.name;

  // Tính lại tiền và voucher sau mỗi thay đổi; request cũ bị hủy khi có thay đổi mới.
  useEffect(() => {
    if (!effectiveStoreId) return undefined;
    const controller = new AbortController();
    const body = cartRequest({ storeId: effectiveStoreId, customer, cart, promotionCodes: selectedCodes, cashReceived: cash });
    const timer = setTimeout(() => {
      setVouchersLoading(true);
      orderApi.vouchers(body, { signal: controller.signal })
        .then(result => { setVouchers(result); setVouchersError(""); })
        .catch(error => { if (!controller.signal.aborted) setVouchersError(error.message); })
        .finally(() => { if (!controller.signal.aborted) setVouchersLoading(false); });
      if (!cart.length) { setQuote(null); setQuoteError(""); return; }
      setQuoteLoading(true);
      orderApi.quote(body, { signal: controller.signal })
        .then(result => { setQuote(result); setQuoteError(""); })
        .catch(error => { if (!controller.signal.aborted) setQuoteError(error.message); })
        .finally(() => { if (!controller.signal.aborted) setQuoteLoading(false); });
    }, QUOTE_DEBOUNCE_MS);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [effectiveStoreId, customer, cart, selectedCodes, cash]);

  const resetSale = () => {
    setCart([]); setCustomer(null); setSelectedCodes([]); setCash(""); setNote(""); setQuote(null);
    scanRef.current?.focus();
  };

  const changeStore = async value => {
    if (value === storeId) return;
    if (cart.length && !await notification.confirm("Đổi cửa hàng sẽ xóa giỏ hàng hiện tại vì tồn kho khác nhau.", { confirmText: "Đổi cửa hàng" })) return;
    resetSale();
    setLastCreated(null);
    setStoreId(value);
  };

  const add = (variant, quantity) => {
    setLastCreated(null);
    const result = addToCart(cart, variant, quantity);
    if (result.capped) notification.warning(`Sản phẩm ${variant.sku} chỉ còn ${variant.stock} tại cửa hàng.`);
    setCart(result.cart);
  };

  const create = async () => {
    setCreating(true);
    try {
      const created = await orderApi.createOffline(cartRequest({ storeId: effectiveStoreId, customer, cart, promotionCodes: selectedCodes, cashReceived: cash, note }));
      setLastCreated(created);
      notification.success(`Đã tạo đơn ${created.orderCode}. Tiền thối lại ${Math.round(created.changeAmount).toLocaleString("vi-VN")}đ.`);
      resetSale();
    } catch (error) {
      notification.error(error.message);
    } finally {
      setCreating(false);
      // Ô quét bị khóa trong lúc tạo đơn; trả focus sau khi mở lại để quét tiếp đơn mới.
      requestAnimationFrame(() => scanRef.current?.focus());
    }
  };

  const storeControl = isGlobal
    ? <StoreSelector stores={stores} value={storeId} onChange={changeStore} required loading={storesLoading} label="Cửa hàng bán" placeholder="Chọn cửa hàng" />
    : <span className="pos-tag"><Store size={14} /> {storeName || "Cửa hàng của bạn"}</span>;

  return <div className="admin-catalog-page order-pos">
    <AdminCatalogPageHeader icon={Store} eyebrow="Đơn hàng" title="Bán tại quầy"
      description="Quét mã hoặc tìm sản phẩm, áp voucher và thu tiền mặt. Đơn hoàn thành ngay khi tạo."
      action={<Link to="/admin/orders" className="ui-button ui-button--ghost"><ArrowLeft size={16} /> Danh sách đơn</Link>} />

    <div className="pos-card pos-toolbar">
      {storeControl}
      {storesError && <span className="pos-error">{storesError}</span>}
    </div>

    {!effectiveStoreId ? <div className="pos-card"><div className="pos-empty"><Store size={28} /><span>{isGlobal ? "Chọn cửa hàng để bắt đầu bán hàng." : "Đang tải cửa hàng…"}</span></div></div>
      : <div className="order-pos__layout">
        <div className="order-pos__main">
          <PosProductPicker ref={scanRef} storeId={effectiveStoreId} disabled={creating} onAdd={add} />
          <PosCart cart={cart} quote={quote}
            onQuantity={(variantId, value) => setCart(current => setQuantity(current, variantId, value))}
            onRemove={variantId => setCart(current => removeLine(current, variantId))} />
        </div>
        <aside className="order-pos__side">
          <PosCustomerPanel storeId={effectiveStoreId} customer={customer} canCreate={hasPermission("CUSTOMER_CREATE")}
            onChange={value => { setCustomer(value); setLastCreated(null); }} />
          <PosVoucherPanel vouchers={vouchers} selected={selectedCodes} loading={vouchersLoading} error={vouchersError}
            onToggle={voucher => setSelectedCodes(current => toggleVoucher(current, voucher, vouchers))} />
          <PosCheckout quote={quote} quoteLoading={quoteLoading} quoteError={quoteError} hasItems={cart.length > 0}
            cash={cash} onCash={setCash} note={note} onNote={setNote} creating={creating} onCreate={create} lastCreated={lastCreated} />
        </aside>
      </div>}
  </div>;
}
