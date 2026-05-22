import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bell } from "lucide-react";

export default function AdminHeader() {
  const [showNotifications, setShowNotifications] = useState(false);
  const notificationRef = useRef(null);
  const navigate = useNavigate();

  const notifications = [
    {
      id: 1,
      title: "Đơn hàng #LN2405131 vừa thanh toán",
      time: "2 phút trước",
      unread: true,
    },
    {
      id: 2,
      title: "Sản phẩm Áo khoác Denim sắp hết hàng",
      time: "8 phút trước",
      unread: true,
    },
    {
      id: 3,
      title: "Khách hàng mới: Trần Minh Khoa",
      time: "15 phút trước",
      unread: true,
    },
    {
      id: 4,
      title: "Phiếu nhập #PN240522 đã được duyệt",
      time: "25 phút trước",
      unread: false,
    },
    {
      id: 5,
      title: "Báo cáo kho ngày 21/05 đã sẵn sàng",
      time: "40 phút trước",
      unread: false,
    },
  ];

  const unreadCount = notifications.filter((item) => item.unread).length;

  useEffect(() => {
    function handleClickOutside(event) {
      if (
        notificationRef.current &&
        !notificationRef.current.contains(event.target)
      ) {
        setShowNotifications(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  return (
    <header className="h-16 bg-zinc-900 border-b border-zinc-800 px-8 flex items-center justify-between">
      <h2 className="text-xl font-semibold text-white">
      </h2>

      <div className="flex items-center gap-6">
        <div
          className="relative"
          ref={notificationRef}
        >
          <button
            onClick={() => setShowNotifications((prev) => !prev)}
            className="relative hover:text-amber-400 transition-colors"
          >
            <Bell size={22} />

            <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-[10px] rounded-full flex items-center justify-center">
              {unreadCount}
            </span>
          </button>

          {showNotifications && (
            <div className="absolute right-0 mt-3 w-96 bg-zinc-900 border border-zinc-700 rounded-2xl shadow-2xl z-40 overflow-hidden">
              <div className="px-4 py-3 border-b border-zinc-800">
                <p className="font-semibold text-white">
                  Thông báo mới
                </p>
              </div>

              <div className="max-h-72 overflow-y-auto [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
                {notifications.map((item) => (
                  <button
                    key={item.id}
                    className="w-full text-left px-4 py-3 border-b border-zinc-800/70 hover:bg-zinc-800/70 transition-colors"
                  >
                    <div className="flex items-start gap-3">
                      <span
                        className={`mt-1 w-2 h-2 rounded-full ${
                          item.unread
                            ? "bg-emerald-400"
                            : "bg-zinc-600"
                        }`}
                      />

                      <div>
                        <p className="text-sm text-zinc-100">
                          {item.title}
                        </p>

                        <p className="text-xs text-zinc-400 mt-1">
                          {item.time}
                        </p>
                      </div>
                    </div>
                  </button>
                ))}
              </div>

              <div className="p-3">
                <button className="w-full rounded-xl py-2 bg-zinc-800 hover:bg-zinc-700 text-amber-400 font-medium transition-colors">
                  Xem thêm
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="flex items-center gap-3">
          <div
            className="text-right cursor-pointer hover:opacity-80 transition"
            onClick={() => navigate("/admin/profile")}
          >
            <p className="text-sm font-medium">
              Nguyễn Văn Admin
            </p>

            <p className="text-xs text-emerald-400">
              Online
            </p>
          </div>

          <img
            src="https://i.pravatar.cc/150?img=68"
            alt="admin"
            className="w-9 h-9 rounded-full border border-amber-400"
          />
        </div>
      </div>
    </header>
  );
}
