import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  Users,
  UserCheck,
} from "lucide-react";
import Pagination from "../../components/common/Pagination";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import { fetchBusinessAuditLogs, fetchAuthAuditLogs, normalizeAdminLogsPage } from "../../api/adminLogsApi";

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

const levelStyle = {
  INFO: "bg-cyan-500/15 text-cyan-200 border-cyan-300/30",
  WARN: "bg-amber-500/15 text-amber-200 border-amber-300/30",
  ALERT: "bg-rose-500/15 text-rose-200 border-rose-300/30",
};

const statCards = [
  {
    key: "onlineEmployees",
    label: "Nhân viên đang sử dụng hệ thống",
    icon: UserCheck,
    accent: "text-emerald-300",
  },
  {
    key: "activeCustomers",
    label: "Khách hàng đang hoạt động",
    icon: Users,
    accent: "text-sky-300",
  },
];

export default function LoManagement() {
  const [currentPage, setCurrentPage] = useState(1);
  const [logPage, setLogPage] = useState(1);
  const [logView, setLogView] = useState("business");
  const [logData, setLogData] = useState({ rows: [], totalPages: 1, totalElements: 0 });
  const [logState, setLogState] = useState("loading");
  const [logError, setLogError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setLogState("loading");
    setLogError("");
    const fetchLogs = logView === "business" ? fetchBusinessAuditLogs : fetchAuthAuditLogs;
    fetchLogs({ page: logPage - 1, size: PAGE_SIZE })
      .then((response) => { if (!cancelled) { setLogData(normalizeAdminLogsPage(response)); setLogState("ready"); } })
      .catch((error) => { if (!cancelled) { setLogError(error.message || "Không thể tải nhật ký."); setLogState("error"); } });
    return () => { cancelled = true; };
  }, [logPage, logView]);

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

  const currentLogs = logData.rows;
  const logTotalPages = logData.totalPages;

  return (
    <div className="admin-catalog-page admin-catalog-page--logs text-zinc-100">
      <AdminCatalogPageHeader
        icon={Activity}
        eyebrow="Giám sát hệ thống"
        title="Nhật ký & phiên hoạt động"
        description="Theo dõi phiên đăng nhập, cảnh báo bảo mật và sự kiện liên dịch vụ trong hệ thống."
        status={(
          <>
            <Activity size={16} className="text-emerald-300" />
            Đồng bộ lúc 13:45 · 22/05/2026
          </>
        )}
      />

        <section className="admin-catalog-stats admin-catalog-stats--logs grid grid-cols-1 sm:grid-cols-2 gap-4">
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

        <section className="admin-catalog-table admin-logs-table rounded-3xl border border-zinc-800 bg-zinc-900 overflow-hidden">
          <div className="p-6 border-b border-zinc-800 bg-zinc-950">
            <h2 className="text-lg font-semibold">Nhân viên đang đăng nhập</h2>
            <p className="text-zinc-400 text-sm mt-1">Hiển thị 5 nhân viên mỗi trang, dữ liệu phiên từ dịch vụ xác thực.</p>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-[920px] w-full text-sm">
              <thead className="bg-zinc-800/70 text-zinc-300">
                <tr>
                  {["Mã NV", "Họ tên", "Vai trò", "Thiết bị", "IP", "Hoạt động gần nhất"].map((head) => (
                    <th key={head} className="px-4 py-3 text-left font-semibold whitespace-nowrap">{head}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {currentEmployees.map((employee) => (
                  <tr key={employee.id} className="border-t border-zinc-800 hover:bg-zinc-800/40 transition-colors">
                    <td className="px-4 py-3 text-zinc-100 font-medium">{employee.id}</td>
                    <td className="px-4 py-3 text-zinc-200">
                      <div className="admin-employee-identity">
                        <span>{employee.name.split(" ").slice(-2).map((part) => part[0]).join("")}</span>
                        <strong>{employee.name}</strong>
                      </div>
                    </td>
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

        <section className="admin-catalog-table admin-logs-table rounded-3xl border border-zinc-800 bg-zinc-900 overflow-hidden">
          <div className="p-6 border-b border-zinc-800 bg-zinc-950">
            <h2 className="text-lg font-semibold">Nhật ký hệ thống</h2>
            <div className="flex gap-3 mt-3" role="group" aria-label="Loại nhật ký">
              {[["business", "Thao tác dữ liệu"], ["auth", "Đăng nhập / đăng xuất"]].map(([view, label]) => (
                <button key={view} type="button" aria-pressed={logView === view}
                  className={`px-4 py-2 rounded-xl border ${logView === view ? "border-cyan-300 text-cyan-200" : "border-zinc-700 text-zinc-400"}`}
                  onClick={() => { setLogView(view); setLogPage(1); setLogState("loading"); setLogData({ rows: [], totalPages: 1, totalElements: 0 }); }}>
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-[1100px] w-full text-sm">
              <thead className="bg-zinc-800/70 text-zinc-300">
                <tr>
                  {["Log ID", logView === "business" ? "Số dòng" : "Mức độ", "Hành động", "Nội dung", "Người thực hiện", "Thời gian"].map((head) => (
                    <th key={head} className="px-4 py-3 text-left font-semibold whitespace-nowrap">{head}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {logState === "loading" && <tr><td className="px-4 py-8 text-center text-zinc-400" colSpan="6">Đang tải nhật ký...</td></tr>}
                {logState === "error" && <tr><td className="px-4 py-8 text-center text-rose-300" colSpan="6">{logError}</td></tr>}
                {logState === "ready" && currentLogs.length === 0 && <tr><td className="px-4 py-8 text-center text-zinc-400" colSpan="6">Chưa có dữ liệu nhật ký.</td></tr>}
                {logState === "ready" && currentLogs.map((log) => (
                  <tr key={log.id} className="border-t border-zinc-800 hover:bg-zinc-800/40 transition-colors">
                    <td className="px-4 py-3 text-zinc-100 font-medium whitespace-nowrap">{log.id}</td>
                    <td className="px-4 py-3">
                      {logView === "business" ? (log.rowCount ?? "Không rõ") : (
                        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${levelStyle[log.action === "LOGIN_FAILED" ? "WARN" : "INFO"]}`}>
                          {log.action === "LOGIN_FAILED" ? "WARN" : "INFO"}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-zinc-200 whitespace-nowrap">{log.action}</td>
                    <td className="px-4 py-3 text-zinc-300">
                      {logView === "business" ? (
                        <details>
                          <summary className="cursor-pointer">{log.detail || log.path || log.jobName || log.requestId || "Chi tiết thay đổi"}</summary>
                          {log.migratedFromAuthAudit && <p>Chuyển từ lịch sử cũ; không có dữ liệu thay đổi từng dòng.</p>}
                          <p>{[log.method, log.path, log.jobName, log.requestId].filter(Boolean).join(" · ")}</p>
                          <pre className="whitespace-pre-wrap break-all max-w-xl text-xs mt-2">{JSON.stringify(log.changes || [], null, 2)}</pre>
                        </details>
                      ) : (log.description || "—")}
                    </td>
                    <td className="px-4 py-3 text-zinc-300 whitespace-nowrap">{log.username || log.actorUserId || log.userId || "SYSTEM"}</td>
                    <td className="px-4 py-3 text-zinc-300 whitespace-nowrap">{log.createdAt ? new Date(log.createdAt).toLocaleString("vi-VN") : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination currentPage={logPage} totalPages={logTotalPages} onPageChange={setLogPage} />
        </section>
    </div>
  );
}
