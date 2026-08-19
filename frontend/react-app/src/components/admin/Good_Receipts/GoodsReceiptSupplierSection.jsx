import Button from "../../common/Button";
import {
  Building2,
  Phone,
  Mail,
  MapPin,
  Plus,
} from "lucide-react";

export default function GoodsReceiptSupplierSection({
  mode = "view",
  supplier,
  onChange,
  onSelectSupplier,
}) {
  const isView = mode === "view";

  const handleChange = (field, value) => {
    onChange?.({
      ...supplier,
      [field]: value,
    });
  };

  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b]">
      {/* HEADER */}

      <div className="flex items-center justify-between border-b border-zinc-700 px-6 py-4">
        <div>
          <h3 className="text-lg font-semibold text-orange-400">
            Nhà cung cấp
          </h3>

          <p className="mt-1 text-sm text-zinc-400">
            Thông tin nhà cung cấp của phiếu nhập
          </p>
        </div>

        {!isView && (
          <Button
            onClick={onSelectSupplier}
            className="flex items-center gap-2 rounded-lg bg-orange-500 px-4 py-2 text-white hover:bg-orange-600"
          >
            <Plus size={18} />
            Chọn NCC
          </Button>
        )}
      </div>

      {/* BODY */}

      <div className="grid grid-cols-1 gap-6 p-6 md:grid-cols-2">
        {/* NAME */}

        <div>
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <Building2 size={16} />
            Tên nhà cung cấp
          </label>

          <input
            value={supplier?.name || ""}
            disabled
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-white"
          />
        </div>

        {/* PHONE */}

        <div>
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <Phone size={16} />
            Số điện thoại
          </label>

          <input
            value={supplier?.phone || ""}
            disabled
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-white"
          />
        </div>

        {/* EMAIL */}

        <div>
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <Mail size={16} />
            Email
          </label>

          <input
            value={supplier?.email || ""}
            disabled
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-white"
          />
        </div>

        {/* CONTACT */}

        <div>
          <label className="mb-2 text-sm text-zinc-400">
            Người liên hệ
          </label>

          <input
            value={supplier?.contactPerson || ""}
            disabled
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-white"
          />
        </div>

        {/* ADDRESS */}

        <div className="md:col-span-2">
          <label className="mb-2 flex items-center gap-2 text-sm text-zinc-400">
            <MapPin size={16} />
            Địa chỉ
          </label>

          <textarea
            rows={3}
            value={supplier?.address || ""}
            disabled
            className="w-full resize-none rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-3 text-white"
          />
        </div>
      </div>
    </div>
  );
}