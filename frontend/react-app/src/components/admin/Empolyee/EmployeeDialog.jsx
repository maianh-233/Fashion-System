import { useCallback, useEffect, useMemo, useState } from "react";
import { Plus, Trash2, UserRound, Users } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter } from "../common/AdminDialog";
import Button from "../../common/Button";
import DialogHeader from "./DialogHeader";
import { employeeApi } from "../../../hooks/adminManagementApi";
import { useSystemNotification } from "../../common/SystemNotification";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import { changeSubordinate, formatSalaryRange, loadSubordinateLists } from "../../../pages/admin/positionManagementLogic";

const ALL_ROLES = [
  ["MANAGER", "Quản lý"], ["STAFF", "Nhân viên bán hàng"],
  ["WAREHOUSE", "Nhân viên kho"], ["ACCOUNTANT", "Kế toán"],
  ["ADMIN", "Quản trị viên"], ["SUPER_ADMIN", "Quản trị viên tối cao"],
];
const ADMIN_ROLES = new Set(["ADMIN", "SUPER_ADMIN"]);
const PHONE_PATTERN = /^(?:\+84|0)(?:3|5|7|8|9)\d{8}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const EMPTY = { username: "", employeeCode: "", newPassword: "", confirmPassword: "", fullName: "", email: "", phone: "", employmentType: "", hireDate: "", employmentStatus: "ACTIVE", active: true, locked: false, roleCode: "STAFF", storeId: "", departmentId: "", positionId: "" };

export default function EmployeeDialog({ mode = "view", employee, stores = [], departments = [], positions = [], privileged = false,
  superAdmin = false, scopeContext = null, busy = false, subordinateData, onClose, onSave }) {
  const isView = mode === "view";
  const storeScoped = scopeContext?.scope === "STORE";
  const [activeTab, setActiveTab] = useState("profile");
  const [form, setForm] = useState(() => initialForm(employee, scopeContext, departments, positions));
  const [errors, setErrors] = useState({});
  const [subordinateBusy, setSubordinateBusy] = useState(false);
  const close = useCallback(() => { if (!busy && !subordinateBusy) onClose(); }, [busy, subordinateBusy, onClose]);
  const roles = useMemo(() => ALL_ROLES.filter(([code]) => code === "SUPER_ADMIN" ? superAdmin : code === "ADMIN" ? privileged : true), [privileged, superAdmin]);
  const needsStoreAssignment = !ADMIN_ROLES.has(form.roleCode);
  const availablePositions = positions.filter((position) => position.departmentId === form.departmentId);
  const set = (field) => (event) => {
    const value = event.target.type === "checkbox" ? event.target.checked : event.target.value;
    setForm((current) => field === "departmentId"
      ? { ...current, departmentId: value, positionId: positions.find((position) => position.departmentId === value)?.id || "" }
      : { ...current, [field]: value });
    setErrors((current) => ({ ...current, [field]: "" }));
  };
  const submit = () => {
    const nextErrors = validate(form, mode, needsStoreAssignment, storeScoped);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;
    onSave({
      ...(mode === "create" ? { username: null } : { username: form.username.trim(), newPassword: form.newPassword || null }),
      fullName: form.fullName.trim(), email: form.email.trim(), phone: form.phone.trim(),
      jobTitle: null,
      employmentType: form.employmentType,
      departmentId: needsStoreAssignment ? form.departmentId : null,
      positionId: needsStoreAssignment ? form.positionId : null,
      ...(mode === "edit" ? { hireDate: form.hireDate || null, employmentStatus: form.employmentStatus, active: form.active, locked: form.locked } : {}),
      roleCodes: [form.roleCode],
      storeId: needsStoreAssignment ? (storeScoped ? scopeContext.storeId : form.storeId || null) : null,
    });
  };
  const cls = "w-full rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm text-white outline-none focus:border-amber-400 disabled:cursor-not-allowed disabled:opacity-70";

  return <AdminDialog open onClose={close} size="lg">
    <DialogHeader mode={mode} onClose={close} />
    <AdminDialogBody className="space-y-6">
      {isView && <div className="flex gap-2 border-b border-zinc-800 pb-3">
        <Tab active={activeTab === "profile"} onClick={() => { if (!subordinateBusy) setActiveTab("profile"); }} icon={UserRound}>Thông tin cá nhân</Tab>
        <Tab active={activeTab === "subordinates"} onClick={() => { if (!subordinateBusy) setActiveTab("subordinates"); }} icon={Users}>Nhân viên dưới quyền</Tab>
      </div>}
      {isView && activeTab === "subordinates"
        ? <SubordinateTab employee={employee} initialData={subordinateData} onBusyChange={setSubordinateBusy} />
        : <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          {mode === "create" ? <>
            <Field label="Mã nhân viên" hint="Hệ thống tự sinh khi lưu và không thể chỉnh sửa."><input value="Tự động tạo khi lưu" readOnly aria-readonly="true" className={cls} /></Field>
            <Field label="Tên đăng nhập" hint="Hệ thống tự động dùng mã nhân viên làm tên đăng nhập."><input value="Tự động tạo theo mã nhân viên" readOnly aria-readonly="true" className={cls} /></Field>
            <Field label="Mật khẩu tạm" hint="Mật khẩu tạm chính là họ tên nhân viên."><input value={form.fullName || "Nhập họ tên để tạo mật khẩu tạm"} disabled className={cls} /></Field>
          </> : <>
            <Field label="Tên đăng nhập *" error={errors.username}><input value={form.username} onChange={set("username")} disabled={isView} className={cls} /></Field>
            <Field label="Mã nhân viên" hint="Mã do hệ thống tạo và không thể chỉnh sửa."><input value={form.employeeCode} disabled className={cls} /></Field>
          </>}
          {mode === "edit" && <>
            <Field label="Mật khẩu mới" error={errors.newPassword} hint="Để trống nếu không muốn đổi mật khẩu."><input type="password" autoComplete="new-password" value={form.newPassword} onChange={set("newPassword")} className={cls} /></Field>
            <Field label="Xác nhận mật khẩu mới" error={errors.confirmPassword}><input type="password" autoComplete="new-password" value={form.confirmPassword} onChange={set("confirmPassword")} className={cls} /></Field>
          </>}
          <Field label="Họ và tên *" error={errors.fullName}><input required value={form.fullName} onChange={set("fullName")} disabled={isView} className={cls} /></Field>
          <Field label="Email *" error={errors.email}><input required type="email" value={form.email} onChange={set("email")} disabled={isView} className={cls} /></Field>
          <Field label="Số điện thoại *" error={errors.phone} hint="Ví dụ: 0912345678 hoặc +84912345678"><input required inputMode="tel" value={form.phone} onChange={set("phone")} disabled={isView} className={cls} /></Field>
          {needsStoreAssignment && <>
            <Field label="Phòng ban *" error={errors.departmentId}><select required value={form.departmentId} onChange={set("departmentId")} disabled={isView} className={cls}><option value="">-- Chọn phòng ban --</option>{departments.map((department) => <option key={department.id} value={department.id}>{department.code} · {department.name}</option>)}</select></Field>
            <Field label="Vị trí *" error={errors.positionId}><select required value={form.positionId} onChange={set("positionId")} disabled={isView || !form.departmentId} className={cls}><option value="">-- Chọn vị trí --</option>{availablePositions.map((position) => <option key={position.id} value={position.id}>{position.code} · {position.name} · Cấp {position.hierarchyLevel} · {formatSalaryRange(position.minSalary, position.maxSalary)}</option>)}</select></Field>
            {storeScoped
              ? <Field label="Cửa hàng" hint="Cửa hàng được backend xác định theo phạm vi tài khoản."><input value={scopeContext.storeName || "Cửa hàng hiện tại"} disabled readOnly className={cls} /></Field>
              : <Field label="Cửa hàng" error={errors.storeId}><select value={form.storeId} onChange={set("storeId")} disabled={isView} className={cls}><option value="">Toàn chuỗi / Không thuộc cửa hàng</option>{stores.map((store) => <option key={store.id} value={store.id}>{store.code ? `${store.code} · ` : ""}{store.name}</option>)}</select></Field>}
          </>}
          <Field label="Vai trò"><select value={form.roleCode} onChange={set("roleCode")} disabled={isView} className={cls}>{roles.map(([code, label]) => <option key={code} value={code}>{label}</option>)}</select></Field>
          <Field label="Loại hợp đồng *" error={errors.employmentType}><select required value={form.employmentType} onChange={set("employmentType")} disabled={isView} className={cls}><option value="">-- Chọn loại hợp đồng --</option><option value="FULL_TIME">Toàn thời gian</option><option value="PART_TIME">Bán thời gian</option><option value="CONTRACT">Hợp đồng</option><option value="INTERN">Thực tập</option><option value="TEMPORARY">Thời vụ</option></select></Field>
          <Field label="Ngày vào làm" hint={mode === "create" ? "Tự động lấy ngày hiện tại của hệ thống." : ""}><input type="date" value={form.hireDate} onChange={set("hireDate")} disabled={isView || mode === "create"} className={cls} /></Field>
          {isView && <Field label="Quản lý trực tiếp"><input value={employee.managerName || "Chưa có quản lý trực tiếp"} disabled className={cls} /></Field>}
          {mode === "edit" && <>
            <Field label="Trạng thái làm việc"><select value={form.employmentStatus} onChange={set("employmentStatus")} className={cls}><option value="ACTIVE">Đang làm việc</option><option value="PROBATION">Thử việc</option><option value="ON_LEAVE">Tạm nghỉ</option><option value="SUSPENDED">Đình chỉ</option><option value="TERMINATED">Đã nghỉ việc</option></select></Field>
            <div className="flex items-end gap-6 pb-3"><Check label="Kích hoạt tài khoản" checked={form.active} onChange={set("active")} /><Check label="Khóa đăng nhập" checked={form.locked} onChange={set("locked")} /></div>
          </>}
        </div>}
    </AdminDialogBody>
    <AdminDialogFooter className="justify-end gap-3">
      <Button onClick={close} disabled={busy || subordinateBusy} className="rounded-2xl px-6 py-3 text-zinc-300 hover:bg-zinc-800">{isView ? "Đóng" : "Hủy"}</Button>
      {!isView && <Button onClick={submit} disabled={busy} className="rounded-2xl bg-amber-500 px-6 py-3 font-semibold text-zinc-950 hover:bg-amber-400 disabled:opacity-50">{busy ? "Đang lưu..." : mode === "create" ? "Tạo nhân viên" : "Lưu thay đổi"}</Button>}
    </AdminDialogFooter>
  </AdminDialog>;
}

function SubordinateTab({ employee, initialData, onBusyChange }) {
  const notification = useSystemNotification();
  const { hasPermission } = useAdminPermissions();
  const [items, setItems] = useState(initialData?.items || []);
  const [candidates, setCandidates] = useState(initialData?.candidates || []);
  const [candidateId, setCandidateId] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const eligible = employee.employmentType === "FULL_TIME" && Boolean(employee.positionId);
  const canAssign = eligible && hasPermission("USER_UPDATE");
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await loadSubordinateLists({ id: employee.id, api: employeeApi, canAssign });
      setItems(data.items); setCandidates(data.candidates);
      setCandidateId((current) => data.candidates.some((item) => item.id === current) ? current : "");
    }
    catch (error) { setCandidates([]); setCandidateId(""); throw error; }
    finally { setLoading(false); }
  }, [employee.id, canAssign]);
  useEffect(() => { Promise.resolve().then(() => load()).catch((error) => notification.error(error.message)); }, [load, notification]);
  const change = async (operation, candidate) => {
    if (busy || loading) return;
    setBusy(true); onBusyChange(true);
    try {
      const changed = await changeSubordinate({ operation, employeeId: employee.id, candidate,
        api: employeeApi, confirm: notification.confirm, reload: load });
      if (changed) {
        setCandidateId("");
        notification.success(operation === "add" ? "Đã thêm nhân viên dưới quyền." : "Đã gỡ nhân viên dưới quyền.");
      }
    } catch (error) { notification.error(error.message); }
    finally { setBusy(false); onBusyChange(false); }
  };
  const add = async () => {
    const candidate = candidates.find((item) => item.id === candidateId);
    if (!candidate) { notification.error("Vui lòng chọn nhân viên đủ điều kiện."); return; }
    await change("add", candidate);
  };
  return <div className="space-y-5">
    {canAssign && <div className="rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4">
      <p className="mb-3 text-sm font-medium text-white">Thêm nhân viên dưới quyền</p>
      <div className="flex flex-col gap-3 sm:flex-row"><select aria-label="Nhân viên đủ điều kiện" value={candidateId} onChange={(event) => setCandidateId(event.target.value)} disabled={busy || loading} className="min-w-0 flex-1 rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm outline-none focus:border-amber-400"><option value="">{loading ? "Đang tải nhân viên..." : candidates.length ? "-- Chọn nhân viên --" : "Không có nhân viên đủ điều kiện"}</option>{candidates.map((candidate) => <option key={candidate.id} value={candidate.id}>{candidate.employeeCode} · {candidate.fullName} · {candidate.positionName} · Cấp {candidate.hierarchyLevel}</option>)}</select><Button permission="USER_UPDATE" onClick={add} disabled={busy || loading || !candidateId} className="flex items-center justify-center gap-2 rounded-2xl bg-amber-500 px-5 py-3 font-semibold text-zinc-950 disabled:opacity-50"><Plus size={17}/>Thêm nhân viên</Button></div>
    </div>}
    {!eligible && <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm text-amber-200">Nhân viên cần có hợp đồng toàn thời gian và vị trí để quản lý nhân viên dưới quyền.</div>}
    <div className="overflow-hidden rounded-2xl border border-zinc-800">
      {loading ? <p className="p-8 text-center text-sm text-zinc-500">Đang tải danh sách...</p> : items.length === 0 ? <p className="p-8 text-center text-sm text-zinc-500">Chưa có nhân viên dưới quyền.</p> : items.map((item) => <div key={item.id} className="flex items-center justify-between gap-4 border-b border-zinc-800 p-4 last:border-0"><div className="min-w-0"><p className="font-medium text-white">{item.fullName}</p><p className="mt-1 truncate text-xs text-zinc-500">{item.employeeCode} · {item.email} · {item.positionName || item.jobTitle || "Chưa có vị trí"}</p></div><Button permission="USER_UPDATE" title="Gỡ nhân viên dưới quyền" onClick={() => change("remove", item)} disabled={busy || loading} className="shrink-0 text-red-400 hover:text-red-300"><Trash2 size={18}/></Button></div>)}
    </div>
  </div>;
}

function validate(form, mode, needsStoreAssignment, storeScoped) {
  const errors = {};
  if (!form.fullName.trim()) errors.fullName = "Vui lòng nhập họ tên.";
  if (mode === "edit" && form.username.trim().length < 3) errors.username = "Tên đăng nhập phải có ít nhất 3 ký tự.";
  if (form.newPassword && form.newPassword.length < 8) errors.newPassword = "Mật khẩu mới phải có ít nhất 8 ký tự.";
  if (form.newPassword !== form.confirmPassword) errors.confirmPassword = "Mật khẩu xác nhận không khớp.";
  if (!EMAIL_PATTERN.test(form.email.trim())) errors.email = "Email không đúng định dạng.";
  if (!PHONE_PATTERN.test(form.phone.trim())) errors.phone = "Số điện thoại không đúng định dạng Việt Nam.";
  if (!form.employmentType) errors.employmentType = "Vui lòng chọn loại hợp đồng.";
  if (needsStoreAssignment && !form.departmentId) errors.departmentId = "Vui lòng chọn phòng ban.";
  if (needsStoreAssignment && !form.positionId) errors.positionId = "Vui lòng chọn vị trí.";
  if (needsStoreAssignment && storeScoped && !form.storeId) errors.storeId = "Tài khoản chưa có cửa hàng hợp lệ.";
  return errors;
}
function today() { const value = new Date(); return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`; }
function initialForm(employee, scopeContext, departments, positions) {
  const departmentId = employee?.departmentId || departments[0]?.id || "";
  const scopedStoreId = scopeContext?.scope === "STORE" ? scopeContext.storeId || "" : "";
  return employee ? { ...EMPTY, username: employee.username || "", employeeCode: employee.employeeCode || "", fullName: employee.fullName || "", email: employee.email || "", phone: employee.phone || "", employmentType: employee.employmentType || "", hireDate: employee.hireDate || "", employmentStatus: employee.employmentStatus || "ACTIVE", active: employee.active !== false, locked: Boolean(employee.locked), roleCode: employee.roleCodes?.[0] || "STAFF", storeId: employee.storeId || scopedStoreId, departmentId, positionId: employee.positionId || positions.find((position) => position.departmentId === departmentId)?.id || "" }
    : { ...EMPTY, storeId: scopedStoreId, departmentId, positionId: positions.find((position) => position.departmentId === departmentId)?.id || "", hireDate: today() };
}
function Tab({ active, onClick, icon: Icon, children }) { return <button type="button" onClick={onClick} className={`flex items-center gap-2 rounded-xl px-4 py-2 text-sm transition ${active ? "bg-amber-500 text-zinc-950" : "text-zinc-400 hover:bg-zinc-800 hover:text-white"}`}><Icon size={16}/>{children}</button>; }
function Field({ label, hint, error, children }) { return <label className="grid gap-2 text-sm text-zinc-400"><span>{label}</span>{children}{error ? <span className="text-xs text-red-400">{error}</span> : hint ? <span className="text-xs text-zinc-500">{hint}</span> : null}</label>; }
function Check({ label, checked, onChange }) { return <label className="flex items-center gap-2 text-sm text-zinc-300"><input type="checkbox" checked={checked} onChange={onChange} className="h-4 w-4 accent-amber-500" />{label}</label>; }
