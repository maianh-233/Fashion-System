import Button from "../common/Button";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertCircle, Eye, KeyRound, Layers3, LoaderCircle, PenSquare, Plus, RotateCcw, Search, ShieldCheck, Trash2 } from "lucide-react";
import Pagination from "../common/Pagination";
import RoleDialog from "./Role/RoleDialog";
import AdminCatalogPageHeader from "./common/AdminCatalogPageHeader";
import {
  createAuthorizationRole,
  deleteAuthorizationRole,
  getAuthorizationRole,
  getAuthorizationRoles,
  getRolePermissionCatalog,
  replaceAuthorizationRolePermissions,
  updateAuthorizationRole,
} from "../../hooks/auth/authorizationSettingsApi";

const PAGE_SIZE = 5;

export default function RoleManagement() {
  const [roles, setRoles] = useState([]);
  const [permissionGroups, setPermissionGroups] = useState([]);
  const [permissionTotal, setPermissionTotal] = useState(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState("");
  const [notice, setNotice] = useState("");
  const [dialog, setDialog] = useState({ open: false, mode: "view", role: null, busy: false, error: "" });

  const loadData = useCallback(async () => {
    setLoading(true);
    setPageError("");
    try {
      const [roleRows, catalog] = await Promise.all([getAuthorizationRoles(), getRolePermissionCatalog()]);
      const roleDetails = await Promise.all(roleRows.map((role) => getAuthorizationRole(role.id)));
      setRoles(roleDetails.map(normalizeRole));
      const groups = flattenCatalog(catalog);
      setPermissionGroups(groups);
      setPermissionTotal(groups.reduce((total, group) => total + group.permissions.length, 0));
    } catch (error) {
      setPageError(error.message || "Không thể tải dữ liệu phân quyền.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const filteredRoles = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
    if (!keyword) return roles;
    return roles.filter((role) => [role.code, role.name, role.description].some((value) => value?.toLowerCase().includes(keyword)));
  }, [roles, searchTerm]);
  const totalPages = Math.max(1, Math.ceil(filteredRoles.length / PAGE_SIZE));
  const pagedRoles = filteredRoles.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const grantedTotal = roles.reduce((total, role) => total + role.permissionCount, 0);

  useEffect(() => { setCurrentPage(1); }, [searchTerm]);
  useEffect(() => { if (currentPage > totalPages) setCurrentPage(totalPages); }, [currentPage, totalPages]);

  const openDialog = (mode, role = null) => setDialog({ open: true, mode, role, busy: false, error: "" });

  const closeDialog = () => setDialog((current) => current.busy ? current : { ...current, open: false, error: "" });

  const saveRole = async (payload) => {
    setDialog((current) => ({ ...current, busy: true, error: "" }));
    try {
      const body = { code: payload.code, name: payload.name, description: payload.description };
      const saved = dialog.mode === "create"
        ? await createAuthorizationRole(body)
        : await updateAuthorizationRole(payload.id, body);
      await replaceAuthorizationRolePermissions(saved.id, payload.permissions);
      setDialog({ open: false, mode: "view", role: null, busy: false, error: "" });
      setNotice(dialog.mode === "create" ? "Đã tạo vai trò và lưu danh sách quyền." : "Đã cập nhật vai trò và danh sách quyền.");
      await loadData();
    } catch (error) {
      setDialog((current) => ({ ...current, busy: false, error: error.message || "Không thể lưu vai trò." }));
    }
  };

  const removeRole = async (role) => {
    if (!window.confirm(`Xóa vai trò ${role.code}? Chỉ role chưa gán cho người dùng và chưa có permission mới xóa được.`)) return;
    setNotice("");
    try {
      await deleteAuthorizationRole(role.id);
      setNotice(`Đã xóa vai trò ${role.code}.`);
      await loadData();
    } catch (error) {
      setPageError(error.message || "Không thể xóa vai trò.");
    }
  };

  return (
    <div className="admin-catalog-page admin-catalog-page--roles">
      <AdminCatalogPageHeader icon={ShieldCheck} eyebrow="Quản trị truy cập" title="Vai trò & phân quyền" description="Dữ liệu role và permission được đồng bộ trực tiếp với backend." />

      {notice && <div className="mb-5 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">{notice}</div>}
      {pageError && <div className="mb-5 flex items-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300"><AlertCircle size={17} />{pageError}<Button onClick={loadData} className="ml-auto text-red-100 underline">Thử lại</Button></div>}

      <div className="admin-catalog-toolbar mb-8 rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
        <div className="flex flex-wrap items-center gap-4">
          <div className="relative min-w-[16rem] flex-1">
            <input type="text" placeholder="Tìm theo mã, tên hoặc mô tả..." value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} className="w-full rounded-2xl border border-zinc-700 bg-zinc-800 py-3 pl-11 pr-4 text-sm focus:border-amber-400 focus:outline-none" />
            <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" />
          </div>
          <Button onClick={() => setSearchTerm("")} className="flex items-center gap-2 rounded-2xl bg-blue-500 px-6 py-3 font-medium hover:bg-blue-600"><RotateCcw size={18} />Reset</Button>
          <Button permission="ROLE_CREATE" onClick={() => openDialog("create")} className="flex items-center gap-2 rounded-2xl bg-amber-500 px-6 py-3 font-medium text-zinc-950 hover:bg-amber-600"><Plus size={18} />Thêm vai trò</Button>
        </div>
      </div>

      <div className="admin-catalog-stats mb-8 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        <Stat label="Vai trò" value={roles.length} icon={ShieldCheck} color="text-blue-400" />
        <Stat label="Permission" value={permissionTotal} icon={KeyRound} color="text-emerald-400" />
        <Stat label="Quyền đã gán" value={grantedTotal} icon={ShieldCheck} color="text-amber-400" />
        <Stat label="Nhóm quyền" value={permissionGroups.length} icon={Layers3} color="text-purple-400" />
      </div>

      <div className="admin-catalog-table overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
        <div className="flex items-center justify-between border-b border-zinc-800 bg-zinc-950 p-6"><h3 className="text-lg font-semibold">Danh sách vai trò</h3><p className="text-sm text-zinc-400">Tìm thấy: <strong className="text-white">{filteredRoles.length}</strong></p></div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead><tr className="border-b border-zinc-800 text-sm text-zinc-400"><th className="px-6 py-5 text-left font-normal">Mã role</th><th className="px-6 py-5 text-left font-normal">Tên vai trò</th><th className="px-6 py-5 text-left font-normal">Mô tả</th><th className="px-6 py-5 text-center font-normal">Số quyền</th><th className="w-44 px-6 py-5 text-center font-normal">Thao tác</th></tr></thead>
            <tbody className="divide-y divide-zinc-800 text-sm">
              {loading ? <tr><td colSpan="5" className="px-6 py-14 text-center text-zinc-400"><LoaderCircle className="mx-auto mb-2 animate-spin" />Đang tải dữ liệu từ hệ thống...</td></tr>
                : pagedRoles.length === 0 ? <tr><td colSpan="5" className="px-6 py-14 text-center text-zinc-500">Không tìm thấy vai trò phù hợp.</td></tr>
                  : pagedRoles.map((role) => <tr key={role.id} className="transition-colors hover:bg-zinc-800"><td className="px-6 py-5 font-medium text-amber-400">{role.code}</td><td className="px-6 py-5 font-medium">{role.name}</td><td className="max-w-lg px-6 py-5 text-zinc-300">{role.description || "—"}</td><td className="px-6 py-5 text-center"><span className="rounded-full bg-amber-500/10 px-3 py-1 text-amber-300">{role.permissionCount}</span></td><td className="px-6 py-5"><div className="flex items-center justify-center gap-4"><Button onClick={() => openDialog("view", role)} className="text-blue-400 hover:text-blue-300" title="Xem"><Eye size={18} /></Button><Button permission="ROLE_UPDATE" onClick={() => openDialog("edit", role)} className="text-amber-400 hover:text-amber-300" title="Sửa"><PenSquare size={18} /></Button><Button permission="ROLE_DELETE" onClick={() => removeRole(role)} className="text-red-400 hover:text-red-300" title="Xóa"><Trash2 size={18} /></Button></div></td></tr>)}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />
      </div>

      {dialog.open && <RoleDialog open mode={dialog.mode} role={dialog.role} permissionGroups={permissionGroups} busy={dialog.busy} error={dialog.error} onClose={closeDialog} onSave={saveRole} />}
    </div>
  );
}

function Stat({ label, value, icon: Icon, color }) {
  return <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6"><div className="flex items-start justify-between"><div><p className="text-zinc-400">{label}</p><p className={`mt-2 text-4xl font-bold ${color}`}>{value}</p></div><Icon size={40} className={color} /></div></div>;
}

function normalizeRole(details) {
  const permissionScopes = Object.fromEntries((details.permissions || []).map((permission) => [permission.permissionId, permission.scope]));
  return { ...details.role, permissionScopes, permissionCount: Object.keys(permissionScopes).length };
}

function flattenCatalog(catalog) {
  return (catalog?.modules || []).filter((module) => module.active !== false).flatMap((module) =>
    (module.groups || []).map((group) => ({ ...group, moduleCode: module.code, moduleName: module.name, permissions: group.permissions || [] })),
  );
}
