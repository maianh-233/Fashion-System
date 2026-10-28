import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { BellRing, X } from "lucide-react";
import { useSystemNotification } from "../common/SystemNotification";
import { stockAlertApi } from "../../api/customer/accountApi";
import { formatDate } from "../../pages/customer/storefrontLogic";
import { stockAlertStatus, variantText } from "../../pages/customer/stockAlertLogic";

/** Danh sách "báo khi có hàng" của khách (trang yêu thích): đang chờ trước, đã báo sau. */
export default function StockAlertList() {
  const notification = useSystemNotification();
  const [alerts, setAlerts] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    stockAlertApi.mine({ signal: controller.signal }).then(setAlerts).catch(() => setAlerts([]));
    return () => controller.abort();
  }, []);

  if (!alerts || alerts.length === 0) return null;

  const cancel = async (alert) => {
    try {
      await stockAlertApi.unsubscribe(alert.variantId);
      setAlerts((current) => current.filter((item) => item.id !== alert.id));
    } catch (failure) {
      notification.error(failure.message);
    }
  };

  return (
    <section className="stock-alert-list" aria-labelledby="stock-alert-title">
      <h2 id="stock-alert-title"><BellRing size={18} /> Chờ có hàng</h2>
      <ul>
        {alerts.map((alert) => {
          const status = stockAlertStatus(alert);
          return (
            <li key={alert.id}>
              {alert.imageUrl ? <img src={alert.imageUrl} alt="" /> : <span className="stock-alert-list__placeholder" />}
              <div>
                <Link to={`/productdetail?id=${alert.productId}`}><strong>{alert.productName}</strong></Link>
                <small>{variantText(alert)} · Đăng ký {formatDate(alert.createdAt)}</small>
              </div>
              <span className={`stock-alert-list__status is-${status.key}`}>{status.label}</span>
              {!alert.notifiedAt ? (
                <button type="button" onClick={() => cancel(alert)} aria-label={`Hủy chờ ${alert.productName}`}><X size={15} /></button>
              ) : <span />}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
