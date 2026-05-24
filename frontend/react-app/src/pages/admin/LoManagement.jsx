import { useMemo, useState } from "react";
import {
  Activity,
  CircleAlert,
  CreditCard,
  MessageSquareMore,
  ShieldCheck,
  Users,
  UserCheck,
} from "lucide-react";
import Pagination from "../../components/common/Pagination";

const PAGE_SIZE = 5;

const loggedInEmployeesSeed = [
  { id: "EMP-001", name: "Nguyen Minh Quan", role: "Admin", device: "Chrome / Windows", ip: "10.20.1.15", lastActivity: "2026-05-22 13:42" },
  { id: "EMP-002", name: "Tran Bao Chau", role: "CSKH", device: "Edge / Windows", ip: "10.20.1.21", lastActivity: "2026-05-22 13:40" },
  { id: "EMP-003", name: "Pham Duc Anh", role: "Kho", device: "Chrome / Android", ip: "10.20.1.30", lastActivity: "2026-05-22 13:39" },
  { id: "EMP-004", name: "Le Hoang Yen", role: "Ketoan", device: "Safari / iPad", ip: "10.20.1.09", lastActivity: "2026-05-22 13:38" },
  { id: "EMP-005", name: "Vo Quoc Nam", role: "Van hanh", device: "Chrome / macOS", ip: "10.20.1.44", lastActivity: "2026-05-22 13:35" },
  { id: "EMP-006", name: "Dang Kieu My", role: "Moderator", device: "Firefox / Windows", ip: "10.20.1.48", lastActivity: "2026-05-22 13:33" },
  { id: "EMP-007", name: "Bui Tuan Kiet", role: "CSKH", device: "Chrome / Windows", ip: "10.20.1.55", lastActivity: "2026-05-22 13:30" },
  { id: "EMP-008", name: "Hoang Gia Linh", role: "Kho", device: "Edge / Windows", ip: "10.20.1.60", lastActivity: "2026-05-22 13:25" },
  { id: "EMP-009", name: "Nguyen Thai Son", role: "Admin", device: "Chrome / Linux", ip: "10.20.1.67", lastActivity: "2026-05-22 13:21" },
  { id: "EMP-010", name: "Tran Nhat Ha", role: "Nhan su", device: "Safari / iPhone", ip: "10.20.1.72", lastActivity: "2026-05-22 13:18" },
  { id: "EMP-011", name: "Mai Hong Nhung", role: "CSKH", device: "Chrome / Windows", ip: "10.20.1.77", lastActivity: "2026-05-22 13:11" },
  { id: "EMP-012", name: "Pham Duy Khanh", role: "Ketoan", device: "Edge / Windows", ip: "10.20.1.80", lastActivity: "2026-05-22 13:06" },
];

const logsByDomain = {
  authAudit: [
    { id: "AUTH-001", level: "INFO", action: "LOGIN_SUCCESS", detail: "Nhan vien EMP-002 dang nhap thanh cong", at: "2026-05-22 13:40" },
    { id: "AUTH-002", level: "WARN", action: "LOGIN_FAILED", detail: "Tai khoan EMP-015 sai mat khau 2 lan", at: "2026-05-22 13:37" },
    { id: "AUTH-003", level: "ALERT", action: "ACCOUNT_LOCKED", detail: "Tai khoan EMP-015 bi khoa do brute-force", at: "2026-05-22 13:36" },
    { id: "AUTH-004", level: "INFO", action: "REFRESH_TOKEN", detail: "EMP-001 da refresh session", at: "2026-05-22 13:32" },
  ],
  orderAndPayment: [
    { id: "PAY-011", level: "INFO", action: "PAYMENT_SUCCESS", detail: "Order ORD-20260522-288 da thanh toan VNPAY", at: "2026-05-22 13:29" },
    { id: "PAY-012", level: "WARN", action: "WEBHOOK_RETRY", detail: "Webhook MOMO timeout, chuyen retry lan 2", at: "2026-05-22 13:25" },
    { id: "PAY-013", level: "INFO", action: "REFUND_REQUEST", detail: "Yeu cau refund cho payment PAY-8891, amount 150000", at: "2026-05-22 13:19" },
  ],
  inventoryAndProduct: [
    { id: "INV-091", level: "WARN", action: "LOW_STOCK", detail: "SKU HD-OVR-BLK-M con 4 san pham", at: "2026-05-22 13:27" },
    { id: "INV-092", level: "ALERT", action: "OUT_OF_STOCK", detail: "SKU DR-FLR-PNK-S da het hang", at: "2026-05-22 13:23" },
    { id: "PRD-114", level: "INFO", action: "PRICE_UPDATE", detail: "Cap nhat sale_price cho 12 variant collection SUMMER2026", at: "2026-05-22 13:15" },
  ],
  chatAndCustomer: [
    { id: "CHAT-401", level: "INFO", action: "ROOM_OPEN", detail: "Mo room ho tro don ORD-20260522-288", at: "2026-05-22 13:34" },
    { id: "CHAT-402", level: "WARN", action: "PENDING_RESPONSE", detail: "Room issue LATE_DELIVERY cho phan hoi > 30 phut", at: "2026-05-22 13:20" },
    { id: "CUS-221", level: "INFO", action: "LOYALTY_TIER_UP", detail: "Customer CUS-1932 duoc nang hang GOLD", at: "2026-05-22 13:08" },
  ],
};

const levelStyle = {
  INFO: "bg-cyan-500/15 text-cyan-200 border-cyan-300/30",
  WARN: "bg-amber-500/15 text-amber-200 border-amber-300/30",
  ALERT: "bg-rose-500/15 text-rose-200 border-rose-300/30",
};

const statCards = [
  {
    key: "onlineEmployees",
    label: "Nhan vien dang su dung he thong",
    icon: UserCheck,
    accent: "text-emerald-300",
  },
  {
    key: "activeCustomers",
    label: "Khach hang dang su dung he thong",
    icon: Users,
    accent: "text-sky-300",
  },
];

const logSections = [
  {
    key: "authAudit",
    title: "Auth Audit Logs",
    description: "Du lieu tu auth_audit_logs, user_tokens, users",
    icon: ShieldCheck,
  },
  {
    key: "orderAndPayment",
    title: "Order & Payment Logs",
    description: "Tong hop payment_transactions, refunds, payment_webhook_logs",
    icon: CreditCard,
  },
  {
    key: "inventoryAndProduct",
    title: "Inventory & Product Logs",
    description: "Canh bao tu inventory + product_variants",
    icon: CircleAlert,
  },
  {
    key: "chatAndCustomer",
    title: "Chat & Customer Activity",
    description: "Su kien tu order_chat_rooms, order_chat_messages, customer profile",
    icon: MessageSquareMore,
  },
];

export default function LoManagement() {
  const [currentPage, setCurrentPage] = useState(1);
  const [logPage, setLogPage] = useState(1);

  const totalPages = Math.max(1, Math.ceil(loggedInEmployeesSeed.length / PAGE_SIZE));

  const currentEmployees = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return loggedInEmployeesSeed.slice(start, start + PAGE_SIZE);
  }, [currentPage]);

  const stats = useMemo(
    () => ({
      onlineEmployees: loggedInEmployeesSeed.length,
      activeCustomers: 268,
    }),
    []
  );

  const allLogs = useMemo(
    () =>
      logSections.flatMap((section) =>
        logsByDomain[section.key].map((log) => ({
          ...log,
          source: section.title,
        }))
      ),
    []
  );

  const logTotalPages = Math.max(1, Math.ceil(allLogs.length / PAGE_SIZE));

  const currentLogs = useMemo(() => {
    const start = (logPage - 1) * PAGE_SIZE;
    return allLogs.slice(start, start + PAGE_SIZE);
  }, [allLogs, logPage]);

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 p-4 md:p-6 lg:p-8">
      <div className="mx-auto max-w-[1600px] space-y-6">
        <section className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
          <div>
            <h1 className="text-2xl md:text-3xl font-bold tracking-tight">LoManagement</h1>
            <p className="text-zinc-400 mt-1">Tong quan session dang hoat dong va log lien service trong he thong.</p>
          </div>
          <div className="inline-flex items-center gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm text-zinc-300">
            <Activity size={16} className="text-emerald-300" />
            Last sync: 2026-05-22 13:45
          </div>
        </section>

        <section className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {statCards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.key} className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5 shadow-[0_16px_40px_rgba(0,0,0,0.3)]">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm text-zinc-400">{card.label}</p>
                    <p className="mt-2 text-3xl font-semibold text-white">{stats[card.key].toLocaleString("vi-VN")}</p>
                  </div>
                  <div className="h-11 w-11 rounded-2xl bg-zinc-800 flex items-center justify-center">
                    <Icon size={20} className={card.accent} />
                  </div>
                </div>
              </div>
            );
          })}
        </section>

        <section className="rounded-3xl border border-zinc-800 bg-zinc-900 overflow-hidden">
          <div className="px-5 py-4 border-b border-zinc-800">
            <h2 className="text-lg font-semibold">Nhan vien dang dang nhap</h2>
            <p className="text-zinc-400 text-sm mt-1">Hien thi 5 nhan vien moi trang, du lieu session tu auth_service.</p>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-[920px] w-full text-sm">
              <thead className="bg-zinc-800/70 text-zinc-300">
                <tr>
                  {["Ma NV", "Ho ten", "Vai tro", "Thiet bi", "IP", "Lan hoat dong cuoi"].map((head) => (
                    <th key={head} className="px-4 py-3 text-left font-semibold whitespace-nowrap">{head}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {currentEmployees.map((employee) => (
                  <tr key={employee.id} className="border-t border-zinc-800 hover:bg-zinc-800/40 transition-colors">
                    <td className="px-4 py-3 text-zinc-100 font-medium">{employee.id}</td>
                    <td className="px-4 py-3 text-zinc-200">{employee.name}</td>
                    <td className="px-4 py-3 text-zinc-300">{employee.role}</td>
                    <td className="px-4 py-3 text-zinc-300">{employee.device}</td>
                    <td className="px-4 py-3 text-zinc-300">{employee.ip}</td>
                    <td className="px-4 py-3 text-zinc-300">{employee.lastActivity}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />
        </section>

        <section className="rounded-3xl border border-zinc-800 bg-zinc-900 overflow-hidden">
          <div className="px-5 py-4 border-b border-zinc-800">
            <h2 className="text-lg font-semibold">System Logs</h2>
            <p className="text-zinc-400 text-sm mt-1">Bang log tong hop tu auth, order-payment, inventory-product, chat-customer.</p>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-[1100px] w-full text-sm">
              <thead className="bg-zinc-800/70 text-zinc-300">
                <tr>
                  {["Log ID", "Muc do", "Action", "Noi dung", "Nguon", "Thoi gian"].map((head) => (
                    <th key={head} className="px-4 py-3 text-left font-semibold whitespace-nowrap">{head}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {currentLogs.map((log) => (
                  <tr key={log.id} className="border-t border-zinc-800 hover:bg-zinc-800/40 transition-colors">
                    <td className="px-4 py-3 text-zinc-100 font-medium whitespace-nowrap">{log.id}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${levelStyle[log.level]}`}>
                        {log.level}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-zinc-200 whitespace-nowrap">{log.action}</td>
                    <td className="px-4 py-3 text-zinc-300">{log.detail}</td>
                    <td className="px-4 py-3 text-zinc-300 whitespace-nowrap">{log.source}</td>
                    <td className="px-4 py-3 text-zinc-300 whitespace-nowrap">{log.at}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination currentPage={logPage} totalPages={logTotalPages} onPageChange={setLogPage} />
        </section>
      </div>
    </div>
  );
}
