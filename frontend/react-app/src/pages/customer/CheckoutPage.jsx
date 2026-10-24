import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { AlertTriangle, ArrowLeft, ArrowRight, LoaderCircle, LockKeyhole, ShoppingBag, Sparkles } from "lucide-react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import Button from "../../components/common/Button";
import { useSystemNotification } from "../../components/common/SystemNotification";
import OrderItems from "../../components/customer/Checkout/OrderItems";
import SavedAddresses from "../../components/customer/Checkout/SavedAddresses";
import AllocationPreview from "../../components/customer/Checkout/AllocationPreview";
import Promotions from "../../components/customer/Checkout/Promotions";
import PriceSummary from "../../components/customer/Checkout/PriceSummary";
import PaymentMethodSelector from "../../components/customer/Checkout/PaymentMethodSelector";
import OrderNote from "../../components/customer/Checkout/OrderNote";
import { addressApi, customerOrderApi } from "../../api/customer/accountApi";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { useCustomerCart } from "../../contexts/CustomerCartContext";

const newRequestId = () => (globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`);

/**
 * Thanh toán đơn online: sản phẩm đã chọn trong giỏ → địa chỉ (có tọa độ) → cửa hàng phụ trách (backend phân) →
 * voucher → phương thức thanh toán → đặt hàng. Mọi số tiền do backend tính lại ở bước báo giá và khi đặt.
 */
export default function CheckoutPage() {
  const navigate = useNavigate();
  const notification = useSystemNotification();
  const { cart, loading: cartLoading, loggedIn, refresh } = useCustomerCart();
  const [addresses, setAddresses] = useState(null);
  const [addressId, setAddressId] = useState(null);
  const [codes, setCodes] = useState([]);
  const [quote, setQuote] = useState(null);
  const [quoting, setQuoting] = useState(false);
  const [quoteError, setQuoteError] = useState("");
  const [vouchers, setVouchers] = useState([]);
  const [methods, setMethods] = useState([{ code: "COD", label: "Thanh toán khi nhận hàng", available: true }]);
  const [paymentMethod, setPaymentMethod] = useState("COD");
  const [note, setNote] = useState("");
  const [placing, setPlacing] = useState(false);
  const requestId = useRef(newRequestId());

  const lines = useMemo(() => cart.items.filter((item) => item.selected && !item.problem)
    .map((item) => ({ variantId: item.variantId, quantity: item.quantity })), [cart.items]);
  const linesKey = JSON.stringify(lines);
  const selectedAddress = addresses?.find((a) => a.id === addressId) || null;
  const located = selectedAddress && selectedAddress.latitude != null && selectedAddress.longitude != null;

  const loadAddresses = useCallback(async (preferId) => {
    try {
      const page = await addressApi.list();
      const list = page.content || [];
      setAddresses(list);
      setAddressId((current) => preferId || current || (list.find((a) => a.isDefault) || list[0])?.id || null);
    } catch (failure) {
      setAddresses([]);
      notification.error(failure.message);
    }
  }, [notification]);

  useEffect(() => {
    if (!loggedIn) return;
    Promise.resolve().then(loadAddresses);
    storefrontApi.paymentMethods().then(setMethods).catch(() => {});
  }, [loggedIn, loadAddresses]);

  // Báo giá + voucher lại mỗi khi đổi địa chỉ, giỏ hoặc mã.
  useEffect(() => {
    if (!located || !lines.length) return undefined;
    const controller = new AbortController();
    const body = { addressId, lines, promotionCodes: codes };
    Promise.resolve().then(() => {
      if (controller.signal.aborted) return;
      setQuoting(true);
      setQuoteError("");
    });
    Promise.all([
      customerOrderApi.quote(body, { signal: controller.signal }),
      customerOrderApi.vouchers(body, { signal: controller.signal }).catch(() => []),
    ]).then(([nextQuote, nextVouchers]) => {
      setQuote(nextQuote);
      setVouchers(nextVouchers || []);
      // Mã không còn hợp lệ bị backend bỏ: đồng bộ lại danh sách đang chọn.
      const applied = (nextQuote.promotions || []).map((p) => p.code);
      if (codes.some((code) => !applied.includes(code))) setCodes(applied);
    }).catch((failure) => {
      if (!controller.signal.aborted) setQuoteError(failure.message);
    }).finally(() => {
      if (!controller.signal.aborted) setQuoting(false);
    });
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [addressId, located, linesKey, codes.join(",")]);

  if (!loggedIn) return <Navigate to={`/customerlogin?redirect=${encodeURIComponent("/checkout")}`} replace />;

  const saveAddress = async (form) => {
    const body = {
      receiverName: form.receiverName.trim(), receiverPhone: form.receiverPhone.trim(), province: form.province || null,
      district: form.district || null, ward: form.ward || null, addressLine: form.addressLine.trim(),
      latitude: form.latitude, longitude: form.longitude, isDefault: Boolean(form.isDefault), addressType: form.addressType || "HOME",
    };
    const saved = form.id ? await addressApi.update(form.id, body) : await addressApi.create(body);
    notification.success("Đã lưu địa chỉ giao hàng.");
    await loadAddresses(saved.id);
  };

  const toggleCode = (code) => setCodes((current) => (current.includes(code) ? current.filter((c) => c !== code) : [...current, code]));
  const applyCode = (code) => setCodes((current) => (current.includes(code) ? current : [...current, code]));

  const place = async () => {
    setPlacing(true);
    try {
      const placed = await customerOrderApi.place({
        addressId, lines, promotionCodes: codes, paymentMethod, note: note.trim() || null, clientRequestId: requestId.current,
      });
      requestId.current = newRequestId();
      await refresh();
      if (placed.payment?.payUrl) {
        notification.info(placed.message);
        window.location.assign(placed.payment.payUrl);
        return;
      }
      if (placed.paymentError) notification.warning(`${placed.message}. ${placed.paymentError}`);
      else notification.success(placed.message);
      navigate(`/orderdetail?id=${placed.id}`, { replace: true });
    } catch (failure) {
      notification.error(failure.message);
      // Giỏ / tồn kho có thể đã đổi: tải lại để khách thấy lý do.
      refresh();
    } finally {
      setPlacing(false);
    }
  };

  const items = cart.items.filter((item) => item.selected && !item.problem).map((item) => ({
    key: item.variantId, name: item.productName, image: item.imageUrl, color: item.color, size: item.size,
    price: item.unitPrice, quantity: item.quantity, total: item.lineTotal,
  }));
  const quoteReady = quote && located && !quoting;
  const warnings = quoteReady ? quote.warnings || [] : [];
  const canPlace = quoteReady && quote.canPlace && !placing && lines.length > 0;
  const discount = quote ? Number(quote.productDiscount || 0) + Number(quote.orderDiscount || 0) : 0;

  if (!cartLoading && !lines.length) {
    return (
      <div className="customer-page checkout-page min-h-screen w-full text-gray-200">
        <div className="customer-page__wide checkout-page__inner">
          <div className="cart-empty-state">
            <ShoppingBag size={30} />
            <h2>Chưa có sản phẩm để thanh toán</h2>
            <p>Hãy chọn sản phẩm còn hàng trong giỏ hàng trước khi thanh toán.</p>
            <Link to="/carts">Quay lại giỏ hàng</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="customer-page checkout-page min-h-screen w-full text-gray-200">
      <div className="customer-page__wide checkout-page__inner">
        <header className="checkout-page__header">
          <div>
            <p><Sparkles size={13} /> Secure checkout</p>
            <h1>Hoàn tất đơn hàng</h1>
            <span><LockKeyhole size={13} /> Giá, ưu đãi và tồn kho được kiểm tra lại khi bạn đặt hàng.</span>
          </div>
          <Link to="/carts"><ArrowLeft size={14} /> Quay lại giỏ hàng</Link>
        </header>

        <div className="checkout-page__layout">
          <div className="checkout-page__content">
            <OrderItems items={items} />
            <SavedAddresses addresses={addresses || []} loading={addresses === null} selectedId={addressId}
              onSelect={(address) => setAddressId(address.id)} onSave={saveAddress} />
            <AllocationPreview parts={quoteReady ? quote.parts : []} split={quoteReady && quote.split} loading={quoting}
              eyebrow="Bước 03" />
            <OrderNote value={note} onChange={(value) => setNote(value.slice(0, 1000))} />
            <PaymentMethodSelector methods={methods} value={paymentMethod} onChange={setPaymentMethod} />
          </div>

          <aside className="checkout-summary">
            <div className="checkout-summary__heading">
              <p>Order review</p>
              <h2>Đơn hàng của bạn</h2>
              <span>{items.length} sản phẩm</span>
            </div>

            <Promotions vouchers={vouchers} selectedCodes={codes} onToggle={toggleCode} onApplyCode={applyCode}
              applied={quoteReady ? quote.promotions : []} />

            {(quote?.notices || []).length > 0 && quoteReady && (
              <ul className="checkout-notices">{quote.notices.map((n) => <li key={n}>{n}</li>)}</ul>
            )}
            {warnings.length > 0 && (
              <ul className="checkout-warnings">{warnings.map((w) => <li key={w}><AlertTriangle size={13} /> {w}</li>)}</ul>
            )}
            {selectedAddress && !located && (
              <p className="checkout-error">Địa chỉ này chưa có vị trí trên bản đồ. Bấm sửa địa chỉ để chọn vị trí.</p>
            )}
            {quoteError && <p className="checkout-error">{quoteError}</p>}

            <PriceSummary subtotal={quote?.subtotal ?? cart.selectedSubtotal} discount={quoteReady ? discount : 0}
              shippingFee={quote?.shippingFee ?? 0} total={quoteReady ? quote.total : cart.selectedSubtotal}>
              <Button type="button" variant="unstyled" className="checkout-place-order" disabled={!canPlace} loading={placing} onClick={place}>
                {quoting ? <><LoaderCircle className="animate-spin" size={16} /> Đang tính tiền...</>
                  : <>{paymentMethod === "COD" ? "Đặt hàng" : `Đặt hàng & thanh toán ${paymentMethod === "VNPAY" ? "VNPay" : "MoMo"}`} <ArrowRight size={16} /></>}
              </Button>
            </PriceSummary>
          </aside>
        </div>
      </div>
    </div>
  );
}
