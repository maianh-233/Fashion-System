import { useSyncExternalStore } from "react";
import { CUSTOMER_SESSION_EVENT, getCustomerProfile, getCustomerToken } from "../api/customerSession";

let cachedKey = null;
let cachedValue = { profile: null, loggedIn: false };

function snapshot() {
  const token = getCustomerToken();
  const profile = getCustomerProfile();
  const key = `${token ? 1 : 0}|${profile?.id || ""}|${profile?.fullName || ""}`;
  if (key !== cachedKey) {
    cachedKey = key;
    cachedValue = { profile: token ? profile : null, loggedIn: Boolean(token && profile) };
  }
  return cachedValue;
}

function subscribe(callback) {
  window.addEventListener(CUSTOMER_SESSION_EVENT, callback);
  window.addEventListener("storage", callback);
  return () => {
    window.removeEventListener(CUSTOMER_SESSION_EVENT, callback);
    window.removeEventListener("storage", callback);
  };
}

/** Phiên khách hiện tại (đổi ngay khi đăng nhập / đăng xuất). */
export function useCustomerSession() {
  return useSyncExternalStore(subscribe, snapshot, () => cachedValue);
}
