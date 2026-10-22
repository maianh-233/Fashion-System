import { LoaderCircle } from "lucide-react";
import { Link } from "react-router-dom";

/** Trạng thái đang tải / lỗi của trang chi tiết phía khách. */
export default function DetailState({ loading, error, icon: Icon, backTo, backLabel }) {
  if (loading) {
    return (
      <div className="flex min-h-96 items-center justify-center gap-2 text-sm text-zinc-400" aria-live="polite">
        <LoaderCircle className="animate-spin" size={18} /> Đang tải...
      </div>
    );
  }
  if (!error) return null;
  return (
    <div className="flex min-h-96 flex-col items-center justify-center gap-3 px-4 text-center">
      {Icon && <Icon className="text-zinc-500" size={36} />}
      <p className="text-sm text-zinc-400">{error}</p>
      {backTo && <Link to={backTo} className="text-sm text-amber-400 underline">{backLabel}</Link>}
    </div>
  );
}
