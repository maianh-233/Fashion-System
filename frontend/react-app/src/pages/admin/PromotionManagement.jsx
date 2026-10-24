import Button from "../../components/common/Button";
import { useEffect, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  Trash2,
  RotateCcw,
  TicketPercent,
  CircleCheck,
  Clock3,
  CalendarX2,
} from "lucide-react";
import Pagination from "../../components/common/Pagination";
import PromotionDialog from "../../components/admin/Promotion/PromotionDialog";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import { promotionApi } from "../../api/admin/promotionApi";
import {
  PROMOTION_STATUSES,
  PROMOTION_TYPES,
  formatDateTime,
  formatDiscount,
  labelOf,
} from "../../components/admin/Promotion/promotionOptions";

const PAGE_SIZE = 10;
const EMPTY_FILTERS = { promotionType: "", status: "", dateFrom: "", dateTo: "" };
const STAT_CARDS = [
  { key: "total", label: "Tổng khuyến mãi", icon: TicketPercent, className: "text-blue-400" },
  { key: "ACTIVE", label: "Đang hoạt động", icon: CircleCheck, className: "text-emerald-400" },
  { key: "SCHEDULED", label: "Sắp diễn ra", icon: Clock3, className: "text-purple-400" },
  { key: "EXPIRED", label: "Đã hết hạn", icon: CalendarX2, className: "text-zinc-300" },
];

function StatusBadge({ status }) {
  const option = PROMOTION_STATUSES.find((item) => item.value === status);
  return (
    <span className={`px-4 py-1 rounded-full text-xs whitespace-nowrap ${option?.className || "bg-zinc-700"}`}>
      {option?.label || status}
    </span>
  );
}

export default function PromotionManagement() {
  const notification = useSystemNotification();
  const { isGlobal } = useAdminPermissions();

  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [currentPage, setCurrentPage] = useState(1);
  const [revision, setRevision] = useState(0);

  const [page, setPage] = useState({ content: [], totalElements: 0, totalPages: 1 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [stats, setStats] = useState({});
  const [dialog, setDialog] = useState(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      setKeyword(keywordInput.trim());
      setCurrentPage(1);
    }, 300);
    return () => clearTimeout(timer);
  }, [keywordInput]);

  useEffect(() => {
    const controller = new AbortController();
    const timer = setTimeout(() => {
      setLoading(true);
      setError("");
      promotionApi.list({
        keyword,
        ...filters,
        page: currentPage - 1,
        size: PAGE_SIZE,
        sort: "createdAt,desc",
      }, { signal: controller.signal })
        .then((data) => setPage({
          content: data?.content || [],
          totalElements: data?.totalElements || 0,
          totalPages: Math.max(1, data?.totalPages || 1),
        }))
        .catch((requestError) => {
          if (!controller.signal.aborted) setError(requestError.message || "Không tải được danh sách khuyến mãi");
        })
        .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    }, 0);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [keyword, filters, currentPage, revision]);

  useEffect(() => {
    const controller = new AbortController();
    const count = (status) => promotionApi.list({ status, size: 1 }, { signal: controller.signal })
      .then((data) => data?.totalElements ?? 0);
    Promise.all([count(""), count("ACTIVE"), count("SCHEDULED"), count("EXPIRED")])
      .then(([total, active, scheduled, expired]) => setStats({ total, ACTIVE: active, SCHEDULED: scheduled, EXPIRED: expired }))
      .catch(() => setStats({}));
    return () => controller.abort();
  }, [revision]);

  const setFilter = (field, value) => {
    setFilters((current) => ({ ...current, [field]: value }));
    setCurrentPage(1);
  };

  const resetFilters = () => {
    setKeywordInput("");
    setKeyword("");
    setFilters(EMPTY_FILTERS);
    setCurrentPage(1);
  };

  const remove = async (promotion) => {
    const accepted = await notification.confirm({
      title: "Xóa khuyến mãi",
      message: `Chuyển ${promotion.code} - ${promotion.name} sang trạng thái ngừng hoạt động?`,
      destructive: true,
    });
    if (!accepted) return;
    try {
      await promotionApi.remove(promotion.id);
      notification.success(`Đã xóa mềm khuyến mãi ${promotion.code}.`);
      setRevision((value) => value + 1);
    } catch (requestError) {
      notification.error(requestError);
    }
  };

  const restore = async (promotion) => {
    const accepted = await notification.confirm({
      title: "Khôi phục khuyến mãi",
      message: `Khôi phục ${promotion.code} - ${promotion.name}? Trạng thái sẽ được tính lại theo thời gian áp dụng.`,
    });
    if (!accepted) return;
    try {
      const restored = await promotionApi.restore(promotion.id);
      notification.success(`Đã khôi phục ${restored.code} (${labelOf(PROMOTION_STATUSES, restored.status)}).`);
      setRevision((value) => value + 1);
    } catch (requestError) {
      notification.error(requestError);
    }
  };

  const statusOptions = PROMOTION_STATUSES.filter((option) => isGlobal || option.value !== "INACTIVE");

  return (
    <div className="admin-catalog-page admin-catalog-page--promotion">
      <AdminCatalogPageHeader
        icon={TicketPercent}
        eyebrow="Chiến dịch bán hàng"
        title="Quản lý khuyến mãi"
        description="Lập kế hoạch ưu đãi, theo dõi thời gian áp dụng và hiệu lực chương trình."
      />

      {/* FILTER */}
      <div className="admin-catalog-toolbar bg-zinc-900 border border-zinc-800 rounded-3xl p-6 mb-8">
        <div className="flex flex-wrap items-end gap-4">
          <div className="relative">
            <input
              type="text"
              placeholder="Tìm theo mã, tên khuyến mãi..."
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              className="bg-zinc-800 border border-zinc-700 focus:border-amber-400 rounded-2xl py-3 pl-11 pr-4 w-80 max-w-full focus:outline-none text-sm"
            />
            <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" />
          </div>

          <select
            value={filters.promotionType}
            onChange={(event) => setFilter("promotionType", event.target.value)}
            className="bg-zinc-800 border border-zinc-700 rounded-2xl py-3 px-4 text-sm"
            aria-label="Lọc theo loại khuyến mãi"
          >
            <option value="">Tất cả loại</option>
            {PROMOTION_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
          </select>

          <select
            value={filters.status}
            onChange={(event) => setFilter("status", event.target.value)}
            className="bg-zinc-800 border border-zinc-700 rounded-2xl py-3 px-4 text-sm"
            aria-label="Lọc theo trạng thái"
          >
            <option value="">Tất cả trạng thái</option>
            {statusOptions.map((status) => <option key={status.value} value={status.value}>{status.label}</option>)}
          </select>

          <label className="flex flex-col gap-1 text-xs text-zinc-400">
            Từ ngày
            <input
              type="date"
              value={filters.dateFrom}
              max={filters.dateTo || undefined}
              onChange={(event) => setFilter("dateFrom", event.target.value)}
              className="bg-zinc-800 border border-zinc-700 rounded-2xl py-2.5 px-4 text-sm text-white"
            />
          </label>

          <label className="flex flex-col gap-1 text-xs text-zinc-400">
            Đến ngày
            <input
              type="date"
              value={filters.dateTo}
              min={filters.dateFrom || undefined}
              onChange={(event) => setFilter("dateTo", event.target.value)}
              className="bg-zinc-800 border border-zinc-700 rounded-2xl py-2.5 px-4 text-sm text-white"
            />
          </label>

          <Button
            onClick={resetFilters}
            className="bg-blue-500 hover:bg-blue-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <RotateCcw size={18} />
            <span>Reset</span>
          </Button>

          <Button
            permission="PROMOTION_CREATE"
            globalOnly
            onClick={() => setDialog({ mode: "create" })}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm khuyến mãi</span>
          </Button>
        </div>
      </div>

      {/* STATS */}
      <div className="admin-catalog-stats grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {STAT_CARDS.map(({ key, label, icon: Icon, className }) => (
          <div key={key} className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
            <div className="flex justify-between items-start">
              <div>
                <p className="text-zinc-400">{label}</p>
                <p className={`text-4xl font-bold mt-2 ${key === "total" ? "" : className}`}>{stats[key] ?? "–"}</p>
              </div>
              <Icon size={40} className={className} />
            </div>
          </div>
        ))}
      </div>

      {/* TABLE */}
      <div className="admin-catalog-table bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">Danh sách khuyến mãi</h3>
          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">{page.totalElements}</span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-6 font-normal">Mã</th>
                <th className="text-left py-5 px-6 font-normal">Tên</th>
                <th className="text-left py-5 px-6 font-normal">Loại</th>
                <th className="text-left py-5 px-6 font-normal">Mức giảm</th>
                <th className="text-center py-5 px-6 font-normal">Bắt đầu</th>
                <th className="text-center py-5 px-6 font-normal">Kết thúc</th>
                <th className="text-center py-5 px-6 font-normal">Trạng thái</th>
                <th className="text-center py-5 px-6 font-normal w-40">Thao tác</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {loading && (
                <tr><td colSpan={8} className="px-6 py-10 text-center text-zinc-400">Đang tải khuyến mãi...</td></tr>
              )}
              {!loading && error && (
                <tr>
                  <td colSpan={8} className="px-6 py-10 text-center text-red-400">
                    {error}
                    <Button
                      onClick={() => setRevision((value) => value + 1)}
                      className="ml-3 text-amber-400 underline"
                      variant="unstyled"
                    >
                      Thử lại
                    </Button>
                  </td>
                </tr>
              )}
              {!loading && !error && page.content.length === 0 && (
                <tr><td colSpan={8} className="px-6 py-10 text-center text-zinc-400">Không có khuyến mãi phù hợp.</td></tr>
              )}
              {!loading && !error && page.content.map((promotion) => (
                <tr key={promotion.id} className="hover:bg-zinc-800 transition-colors">
                  <td className="px-6 py-5 font-medium text-amber-400 whitespace-nowrap">{promotion.code}</td>
                  <td className="px-6 py-5 font-medium">{promotion.name}</td>
                  <td className="px-6 py-5 whitespace-nowrap">
                    {labelOf(PROMOTION_TYPES, promotion.promotionType)}
                    {promotion.promotionType === "POINT_REDEMPTION" && (
                      <span className="block text-xs text-zinc-400">{promotion.pointsRequired} điểm</span>
                    )}
                  </td>
                  <td className="px-6 py-5 whitespace-nowrap">{formatDiscount(promotion)}</td>
                  <td className="px-6 py-5 text-center whitespace-nowrap">{formatDateTime(promotion.startDate)}</td>
                  <td className="px-6 py-5 text-center whitespace-nowrap">{formatDateTime(promotion.endDate)}</td>
                  <td className="px-6 py-5 text-center"><StatusBadge status={promotion.status} /></td>
                  <td className="px-6 py-5">
                    <div className="flex items-center justify-center gap-3">
                      <Button
                        onClick={() => setDialog({ mode: "view", id: promotion.id })}
                        className="text-blue-400 hover:text-blue-300 transition-colors"
                        title="Xem"
                      >
                        <Eye size={18} />
                      </Button>

                      <Button
                        permission="PROMOTION_UPDATE"
                        globalOnly
                        onClick={() => setDialog({ mode: "edit", id: promotion.id })}
                        className="text-amber-400 hover:text-amber-300 transition-colors"
                        title="Sửa"
                      >
                        <PenSquare size={18} />
                      </Button>

                      {promotion.status === "INACTIVE" ? (
                        <Button
                          permission="PROMOTION_UPDATE"
                          globalOnly
                          onClick={() => restore(promotion)}
                          className="text-emerald-400 hover:text-emerald-300 transition-colors"
                          title="Khôi phục"
                        >
                          <RotateCcw size={18} />
                        </Button>
                      ) : (
                        <Button
                          permission="PROMOTION_DELETE"
                          globalOnly
                          onClick={() => remove(promotion)}
                          className="text-red-400 hover:text-red-300 transition-colors"
                          title="Xóa mềm"
                        >
                          <Trash2 size={18} />
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <Pagination
          currentPage={currentPage}
          totalPages={page.totalPages}
          onPageChange={setCurrentPage}
        />
      </div>

      {dialog && (
        <PromotionDialog
          key={`${dialog.mode}-${dialog.id || "new"}`}
          mode={dialog.mode}
          promotionId={dialog.id}
          onClose={() => setDialog(null)}
          onSaved={() => { setDialog(null); setRevision((value) => value + 1); }}
        />
      )}
    </div>
  );
}
