import { Calendar, FileText, Package } from "lucide-react";

export default function GoodsIssueBasicInfo({
  mode = "view",
  issue,
  onChange,
}) {
  const isView = mode === "view";

  const issueTypes = [
    {
      value: "ORDER",
      label: "Xuất cho đơn hàng",
    },
    {
      value: "TRANSFER",
      label: "Chuyển kho",
    },
    {
      value: "DAMAGED",
      label: "Hàng hỏng",
    },
    {
      value: "RETURN_SUPPLIER",
      label: "Trả nhà cung cấp",
    },
    {
      value: "INTERNAL_USE",
      label: "Sử dụng nội bộ",
    },
  ];

  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b] p-6">
      <div className="mb-6 flex items-center gap-3">
        <Package
          size={22}
          className="text-orange-400"
        />

        <div>
          <h3 className="text-lg font-semibold text-white">
            Thông tin phiếu xuất
          </h3>

          <p className="text-sm text-zinc-500">
            Thông tin cơ bản của phiếu xuất kho
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        {/* Mã phiếu */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Mã phiếu xuất
          </label>

          <div className="relative">
            <FileText
              size={18}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
            />

            <input
              value={issue.issueCode}
              disabled
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 py-2 pl-10 pr-4 text-white"
            />
          </div>
        </div>

        {/* Ngày xuất */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Ngày xuất
          </label>

          <div className="relative">
            <Calendar
              size={18}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
            />

            <input
              type="date"
              value={issue.issueDate}
              disabled={isView}
              onChange={(e) =>
                onChange(
                  "issueDate",
                  e.target.value
                )
              }
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 py-2 pl-10 pr-4 text-white focus:border-orange-500 focus:outline-none"
            />
          </div>
        </div>

        {/* Loại xuất */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Loại xuất kho
          </label>

          <select
            value={issue.issueType}
            disabled={isView}
            onChange={(e) =>
              onChange(
                "issueType",
                e.target.value
              )
            }
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-4 py-2 text-white focus:border-orange-500 focus:outline-none"
          >
            {issueTypes.map((type) => (
              <option
                key={type.value}
                value={type.value}
              >
                {type.label}
              </option>
            ))}
          </select>
        </div>

        {/* Trạng thái */}

        <div>
          <label className="mb-2 block text-sm text-zinc-400">
            Trạng thái
          </label>

          <input
            value={issue.status}
            disabled
            className="w-full rounded-lg border border-zinc-700 bg-zinc-800 px-4 py-2 text-white"
          />
        </div>

        {/* Ghi chú */}

        <div className="md:col-span-2">
          <label className="mb-2 block text-sm text-zinc-400">
            Ghi chú
          </label>

          <textarea
            rows={4}
            value={issue.note}
            disabled={isView}
            onChange={(e) =>
              onChange(
                "note",
                e.target.value
              )
            }
            placeholder="Nhập ghi chú..."
            className="w-full rounded-lg border border-zinc-700 bg-zinc-900 p-3 text-white resize-none focus:border-orange-500 focus:outline-none"
          />
        </div>
      </div>
    </div>
  );
}