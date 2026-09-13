import { useState } from "react";
import { Check, Copy, KeyRound } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import Button from "../../common/Button";

/** Hiển thị thông tin hệ thống vừa tạo đúng một lần để người quản trị bàn giao. */
export default function CreatedEmployeeDialog({ result, onClose }) {
  const [copied, setCopied] = useState(false);
  const employee = result.employee;
  const content = `Mã nhân viên: ${employee.employeeCode}\nTên đăng nhập: ${employee.username}\nMật khẩu tạm: ${result.temporaryPassword}`;
  const copy = async () => {
    await navigator.clipboard.writeText(content);
    setCopied(true);
  };
  return <AdminDialog open onClose={onClose} size="md" closeOnBackdrop={false}>
    <AdminDialogHeader title="Tạo nhân viên thành công" description="Hãy bàn giao thông tin đăng nhập này cho nhân viên." onClose={onClose} />
    <AdminDialogBody className="space-y-5">
      <div className="flex items-center gap-3 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-emerald-200"><KeyRound size={22}/><p className="text-sm">Mật khẩu tạm chỉ được hiển thị trong hộp thoại này.</p></div>
      <div className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-5">
        <Credential label="Họ và tên" value={employee.fullName} />
        <Credential label="Mã nhân viên" value={employee.employeeCode} />
        <Credential label="Tên đăng nhập" value={employee.username} />
        {employee.departmentName && <Credential label="Phòng ban" value={employee.departmentName} />}
        {employee.positionName && <Credential label="Vị trí" value={employee.positionName} />}
        <Credential label="Mật khẩu tạm" value={result.temporaryPassword} highlight />
      </div>
    </AdminDialogBody>
    <AdminDialogFooter className="justify-end gap-3">
      <Button onClick={copy} className="flex items-center gap-2 rounded-2xl border border-zinc-700 px-5 py-3 text-zinc-200 hover:bg-zinc-800">{copied ? <Check size={17}/> : <Copy size={17}/>} {copied ? "Đã sao chép" : "Sao chép thông tin"}</Button>
      <Button onClick={onClose} className="rounded-2xl bg-amber-500 px-6 py-3 font-semibold text-zinc-950">Đã lưu thông tin</Button>
    </AdminDialogFooter>
  </AdminDialog>;
}

function Credential({ label, value, highlight = false }) {
  return <div className="grid grid-cols-[8rem_1fr] items-center gap-3 text-sm"><span className="text-zinc-500">{label}</span><strong className={highlight ? "break-all text-amber-300" : "break-all text-white"}>{value}</strong></div>;
}
