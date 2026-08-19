import Button from "../../common/Button";
import { useMemo, useState } from "react";
import {
  Search,
  Building2,
  Phone,
  Mail,
  MapPin,
  Check,
} from "lucide-react";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

export default function SupplierPickerDialog({
  open,
  suppliers = [],
  onClose,
  onAdd,
}) {
  const [keyword, setKeyword] = useState("");
  const [selectedSupplier, setSelectedSupplier] =
    useState(null);

  /* =====================================================
      FILTER
  ===================================================== */

  const filteredSuppliers = useMemo(() => {
    return suppliers.filter((supplier) => {
      const key = keyword.toLowerCase();

      return (
        supplier.name
          ?.toLowerCase()
          .includes(key) ||
        supplier.code
          ?.toLowerCase()
          .includes(key) ||
        supplier.phone
          ?.toLowerCase()
          .includes(key) ||
        supplier.contactName
          ?.toLowerCase()
          .includes(key)
      );
    });
  }, [keyword, suppliers]);

  /* =====================================================
      RETURN
  ===================================================== */

  if (!open) return null;

  /* =====================================================
      SELECT
  ===================================================== */

  const handleSelectSupplier = (supplier) => {
    setSelectedSupplier(supplier);
  };

  /* =====================================================
      ADD
  ===================================================== */

  const handleAdd = () => {
    if (!selectedSupplier) return;

    onAdd?.({
      id: selectedSupplier.id,
      code: selectedSupplier.code,
      name: selectedSupplier.name,
      contactPerson:
        selectedSupplier.contactName,
      phone: selectedSupplier.phone,
      email: selectedSupplier.email,
      address: selectedSupplier.address,
      status: selectedSupplier.status,
    });

    setKeyword("");
    setSelectedSupplier(null);

    onClose?.();
  };

    return (
    <AdminDialog open={open} onClose={onClose} size="lg">
        <AdminDialogHeader
          title="Chọn nhà cung cấp"
          description="Chọn nhà cung cấp cho phiếu nhập kho"
          onClose={onClose}
        />
        <AdminDialogBody className="admin-dialog__body--flush">

        {/* ================= BODY ================= */}

        <div className="grid h-[650px] grid-cols-1 md:grid-cols-2">

          {/* ================= LEFT ================= */}

          <div className="flex flex-col border-r border-zinc-700">

            {/* SEARCH */}

            <div className="p-5">
              <div className="relative">
                <Search
                  size={18}
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
                />

                <input
                  value={keyword}
                  onChange={(e) =>
                    setKeyword(e.target.value)
                  }
                  placeholder="Tìm mã, tên, SĐT..."
                  className="w-full rounded-lg border border-zinc-700 bg-zinc-900 py-2 pl-10 pr-4 text-white outline-none focus:ring-2 focus:ring-orange-500"
                />
              </div>
            </div>

            {/* SUPPLIER LIST */}

            <div className="flex-1 overflow-y-auto">

              {filteredSuppliers.length === 0 && (
                <div className="flex h-full items-center justify-center text-zinc-500">
                  Không tìm thấy nhà cung cấp
                </div>
              )}

              {filteredSuppliers.map((supplier) => (
                <Button
                  key={supplier.id}
                  onClick={() =>
                    handleSelectSupplier(supplier)
                  }
                  className={`flex w-full gap-4 border-b border-zinc-800 p-4 text-left transition ${
                    selectedSupplier?.id === supplier.id
                      ? "bg-orange-500/10"
                      : "hover:bg-zinc-800"
                  }`}
                >
                  {/* Avatar */}

                  <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-zinc-800">
                    <Building2
                      size={28}
                      className="text-orange-400"
                    />
                  </div>

                  {/* Info */}

                  <div className="flex flex-1 flex-col justify-center">

                    <div className="flex items-center justify-between">

                      <h4 className="font-medium text-white">
                        {supplier.name}
                      </h4>

                      {selectedSupplier?.id ===
                        supplier.id && (
                        <Check
                          size={18}
                          className="text-orange-400"
                        />
                      )}

                    </div>

                    <div className="mt-1 text-sm text-zinc-500">
                      {supplier.code}
                    </div>

                    <div className="mt-2 flex items-center justify-between">

                      <span className="text-xs text-zinc-400">
                        {supplier.phone}
                      </span>

                      <span
                        className={`rounded-full px-2 py-1 text-xs font-medium ${
                          supplier.status === "ACTIVE"
                            ? "bg-green-500/10 text-green-400"
                            : "bg-red-500/10 text-red-400"
                        }`}
                      >
                        {supplier.status === "ACTIVE"
                          ? "Đang hoạt động"
                          : "Ngừng hoạt động"}
                      </span>

                    </div>

                  </div>
                </Button>
              ))}

            </div>

          </div>

          {/* ===== RIGHT (Phần 2 bắt đầu từ đây) ===== */}
                    {/* ================= RIGHT ================= */}

          <div className="overflow-y-auto p-6">

            {!selectedSupplier ? (
              <div className="flex h-full items-center justify-center">
                <div className="text-center">
                  <Building2
                    size={70}
                    className="mx-auto text-zinc-600"
                  />

                  <p className="mt-5 text-zinc-500">
                    Chọn nhà cung cấp bên trái
                  </p>
                </div>
              </div>
            ) : (
              <div>

                {/* ICON */}

                <div className="mx-auto flex h-28 w-28 items-center justify-center rounded-2xl bg-zinc-800">
                  <Building2
                    size={54}
                    className="text-orange-400"
                  />
                </div>

                {/* NAME */}

                <h3 className="mt-5 text-center text-2xl font-bold text-white">
                  {selectedSupplier.name}
                </h3>

                <p className="mt-2 text-center text-zinc-500">
                  {selectedSupplier.code}
                </p>

                {/* STATUS */}

                <div className="mt-4 flex justify-center">
                  <span
                    className={`rounded-full px-4 py-2 text-sm font-medium ${
                      selectedSupplier.status === "ACTIVE"
                        ? "bg-green-500/10 text-green-400"
                        : "bg-red-500/10 text-red-400"
                    }`}
                  >
                    {selectedSupplier.status === "ACTIVE"
                      ? "Đang hoạt động"
                      : "Ngừng hoạt động"}
                  </span>
                </div>

                {/* INFORMATION */}

                <div className="mt-8 space-y-4">

                  {/* CONTACT */}

                  <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-4">
                    <div className="flex items-center gap-3">
                      <Building2
                        size={18}
                        className="text-orange-400"
                      />

                      <span className="text-sm text-zinc-400">
                        Người liên hệ
                      </span>
                    </div>

                    <div className="mt-2 font-medium text-white">
                      {selectedSupplier.contactName || "-"}
                    </div>
                  </div>

                  {/* PHONE */}

                  <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-4">
                    <div className="flex items-center gap-3">
                      <Phone
                        size={18}
                        className="text-orange-400"
                      />

                      <span className="text-sm text-zinc-400">
                        Số điện thoại
                      </span>
                    </div>

                    <div className="mt-2 font-medium text-white">
                      {selectedSupplier.phone || "-"}
                    </div>
                  </div>

                  {/* EMAIL */}

                  <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-4">
                    <div className="flex items-center gap-3">
                      <Mail
                        size={18}
                        className="text-orange-400"
                      />

                      <span className="text-sm text-zinc-400">
                        Email
                      </span>
                    </div>

                    <div className="mt-2 break-all font-medium text-white">
                      {selectedSupplier.email || "-"}
                    </div>
                  </div>

                  {/* ADDRESS */}

                  <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-4">
                    <div className="flex items-center gap-3">
                      <MapPin
                        size={18}
                        className="text-orange-400"
                      />

                      <span className="text-sm text-zinc-400">
                        Địa chỉ
                      </span>
                    </div>

                    <div className="mt-2 text-white">
                      {selectedSupplier.address || "-"}
                    </div>
                  </div>

                </div>

              </div>
            )}

          </div>

        </div>
        </AdminDialogBody>

        {/* ================= FOOTER ================= */}

        <AdminDialogFooter>

          <Button
            onClick={onClose}
            className="rounded-lg border border-zinc-700 px-5 py-2 text-white hover:bg-zinc-800"
          >
            Hủy
          </Button>

          <Button
            disabled={!selectedSupplier}
            onClick={handleAdd}
            className="rounded-lg bg-orange-500 px-6 py-2 text-white hover:bg-orange-600 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Chọn nhà cung cấp
          </Button>

        </AdminDialogFooter>
    </AdminDialog>
  );
}
