import { useEffect, useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { Gift, LoaderCircle, Star, TicketPercent } from "lucide-react";
import Button from "../../components/common/Button";
import PromotionCard from "../../components/customer/Promotion/PromotionCard";
import ServerPagedList from "../../components/customer/ServerPagedList";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { profileApi, voucherWalletApi } from "../../api/customerApi";
import { updateCustomerProfile } from "../../api/customerSession";
import { useCustomerSession } from "../../hooks/useCustomerSession";
import { useServerPages } from "../../hooks/useServerPages";
import { formatDate } from "./storefrontLogic";
import { canRedeem, formatPoints, missingPoints, toCardPromotion, voucherState } from "./voucherWalletLogic";

const PAGE_SIZE = 9;
const TABS = [
  { key: "redeem", label: "Đổi điểm", icon: Gift },
  { key: "mine", label: "Voucher của tôi", icon: TicketPercent },
];

/** Ví voucher: đổi điểm tích lũy lấy voucher và xem voucher đã đổi (chưa dùng, đã dùng, hết hiệu lực). */
export default function VoucherWalletPage() {
  const { loggedIn, profile } = useCustomerSession();
  const notification = useSystemNotification();
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = searchParams.get("tab") === "mine" ? "mine" : "redeem";
  const [points, setPoints] = useState(null);
  const [version, setVersion] = useState(0);
  const [redeeming, setRedeeming] = useState(null);

  // Số dư điểm lấy lại từ máy chủ; trong lúc chờ dùng số đã lưu trong phiên.
  useEffect(() => {
    if (!loggedIn) return undefined;
    const controller = new AbortController();
    profileApi.get({ signal: controller.signal })
      .then((data) => {
        setPoints(data.points ?? 0);
        updateCustomerProfile({ points: data.points ?? 0 });
      })
      .catch(() => {});
    return () => controller.abort();
  }, [loggedIn]);

  const list = useServerPages(`${tab}#${version}`, (page, options) => (tab === "mine"
    ? voucherWalletApi.mine({ page: page - 1, size: PAGE_SIZE }, options)
    : voucherWalletApi.redeemable({ page: page - 1, size: PAGE_SIZE }, options)));

  if (!loggedIn) return <Navigate to={`/customerlogin?redirect=${encodeURIComponent("/vouchers")}`} replace />;

  const balance = points ?? profile?.points ?? 0;

  const redeem = async (promotion) => {
    setRedeeming(promotion.id);
    try {
      const result = await voucherWalletApi.redeem(promotion.id);
      setPoints(result.pointsBalance);
      updateCustomerProfile({ points: result.pointsBalance });
      notification.success(`Đã đổi ${formatPoints(result.pointsSpent)} lấy voucher ${promotion.code}. Chọn voucher khi thanh toán để dùng.`);
      setVersion((value) => value + 1);
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setRedeeming(null);
    }
  };

  const renderRedeemable = (promotion) => {
    const enough = canRedeem(promotion, balance);
    const busy = redeeming === promotion.id;
    return (
      <PromotionCard key={promotion.id} promotion={toCardPromotion(promotion)}
        note="Voucher vào ví ngay sau khi đổi, chọn voucher ở bước thanh toán."
        action={(
          <Button type="button" variant="unstyled" className="voucher-wallet__redeem" disabled={!enough || Boolean(redeeming)}
            onClick={() => redeem(promotion)}>
            {busy ? <LoaderCircle className="animate-spin" size={14} /> : <Gift size={14} />}
            {enough ? `Đổi ${formatPoints(promotion.pointsRequired)}` : `Còn thiếu ${formatPoints(missingPoints(promotion, balance))}`}
          </Button>
        )} />
    );
  };

  const renderOwned = (item) => {
    const state = voucherState(item);
    const note = state.key === "used"
      ? `Đã dùng ngày ${formatDate(item.usedAt)}.`
      : `Đổi ngày ${formatDate(item.redeemedAt)} với ${formatPoints(item.pointsSpent)}.`;
    return (
      <PromotionCard key={item.id} promotion={toCardPromotion(item.promotion)} status={state} note={note}
        action={state.key === "used" ? (
          <Link className="promotion-card__link" to={`/orderdetail?id=${item.usedOrderId}`}>Xem đơn đã dùng</Link>
        ) : null} />
    );
  };

  return (
    <div className="customer-page min-h-screen w-full text-zinc-200">
      <div className="customer-page__wide px-4 py-5 sm:px-6 sm:py-8">
        <div className="mb-6 flex flex-col gap-4 sm:mb-8 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="customer-page-title">Ví voucher</h1>
            <p className="mt-1 text-sm text-zinc-400">Dùng điểm tích lũy để đổi voucher, sau đó chọn voucher khi thanh toán.</p>
          </div>
          <div className="voucher-wallet__balance">
            <Star size={17} />
            <span><small>Điểm hiện có</small><strong>{formatPoints(balance)}</strong></span>
          </div>
        </div>

        <div className="voucher-wallet__tabs" role="tablist" aria-label="Ví voucher">
          {TABS.map(({ key, label, icon: Icon }) => (
            <button key={key} type="button" role="tab" aria-selected={tab === key}
              className={`voucher-wallet__tab ${tab === key ? "is-active" : ""}`}
              onClick={() => setSearchParams(key === "mine" ? { tab: "mine" } : {}, { replace: true })}>
              <Icon size={15} /> {label}
            </button>
          ))}
        </div>

        <ServerPagedList
          list={list}
          itemLabel="voucher"
          emptyIcon={tab === "mine" ? TicketPercent : Gift}
          emptyText={tab === "mine" ? "Bạn chưa đổi voucher nào." : "Hiện chưa có voucher đổi điểm dành cho hạng của bạn."}
          desktopClassName="hidden gap-6 sm:grid sm:grid-cols-2 lg:grid-cols-3"
          mobileClassName="grid grid-cols-1 gap-6 sm:hidden"
          skeleton={<div className="customer-card customer-card--skeleton" />}
          renderItem={tab === "mine" ? renderOwned : renderRedeemable}
          onPageChange={() => window.scrollTo({ top: 0, behavior: "smooth" })}
        />
      </div>
    </div>
  );
}
