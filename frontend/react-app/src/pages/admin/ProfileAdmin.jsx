import { useState } from "react";

export default function ProfileAdmin() {
  const [tab, setTab] = useState(0);

  const [phone, setPhone] = useState("0987 654 321");

  const [currentPass, setCurrentPass] = useState("");
  const [newPass, setNewPass] = useState("");
  const [confirmPass, setConfirmPass] = useState("");

  const [modal, setModal] = useState({
    open: false,
    title: "",
    message: "",
  });

  const switchTab = (t) => setTab(t);

  const showModal = (title, message) => {
    setModal({ open: true, title, message });
  };

  const closeModal = () => {
    setModal({ ...modal, open: false });
  };

  const updatePhone = () => {
    if (!phone.trim()) return alert("Vui lòng nhập số điện thoại");
    showModal("Thành công", `Số điện thoại đã được cập nhật: ${phone}`);
  };

  const changePassword = () => {
    if (!currentPass || !newPass || !confirmPass)
      return alert("Vui lòng điền đầy đủ thông tin!");

    if (newPass !== confirmPass)
      return alert("Mật khẩu xác nhận không khớp!");

    if (newPass.length < 6)
      return alert("Mật khẩu mới phải có ít nhất 6 ký tự!");

    showModal("Thành công", "Mật khẩu đã được đổi thành công.");

    setCurrentPass("");
    setNewPass("");
    setConfirmPass("");
  };

  return (
    <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 mb-8">
      <div className="max-w-7xl mx-auto">

        {/* HEADER */}
        <h1 className="text-3xl font-bold text-white mb-8">
          Thông Tin Cá Nhân
        </h1>

        {/* TABS */}
        <div className="flex border-b border-zinc-800 mb-8">
          <button
            onClick={() => switchTab(0)}
            className={`px-8 py-4 text-lg font-medium ${
              tab === 0
                ? "border-b-2 border-blue-500 text-white"
                : "text-gray-400"
            }`}
          >
            Thông Tin Cơ Bản
          </button>

          <button
            onClick={() => switchTab(1)}
            className={`px-8 py-4 text-lg font-medium ${
              tab === 1
                ? "border-b-2 border-blue-500 text-white"
                : "text-gray-400"
            }`}
          >
            Đổi Mật Khẩu
          </button>
        </div>

        {/* TAB 0 */}
        {tab === 0 && (
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-8">

            <div className="flex flex-col md:flex-row gap-8">

              {/* Avatar */}
              <div className="flex flex-col items-center md:items-start">
                <div className="w-32 h-32 rounded-3xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-5xl font-bold text-white">
                  ĐA
                </div>
              </div>

              {/* INFO */}
              <div className="flex-1 space-y-6">

                <div>
                  <p className="text-gray-400 text-sm">Họ và tên</p>
                  <p className="text-2xl font-semibold">Đỗ Anh</p>
                </div>

                <div className="grid md:grid-cols-2 gap-6">

                  <div>
                    <p className="text-gray-400 text-sm">Email</p>
                    <input
                      value="do.anh@company.vn"
                      readOnly
                      className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3 text-gray-400"
                    />
                  </div>

                  <div>
                    <p className="text-gray-400 text-sm">SĐT</p>
                    <input
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3 focus:border-blue-500 outline-none"
                    />
                  </div>

                </div>

                {/* ROLE */}
                <div>
                  <p className="text-gray-400 text-sm mb-3">
                    Vai trò theo cửa hàng
                  </p>

                  <div className="space-y-3">

                    <div className="bg-zinc-800 border border-zinc-700 rounded-2xl p-5 flex justify-between">
                      <div>
                        <p>Cửa hàng Quận 1</p>
                        <p className="text-sm text-gray-400">
                          Store Manager
                        </p>
                      </div>
                      <span className="text-emerald-400">Chính</span>
                    </div>

                    <div className="bg-zinc-800 border border-zinc-700 rounded-2xl p-5 flex justify-between">
                      <div>
                        <p>Cửa hàng Quận 7</p>
                        <p className="text-sm text-gray-400">
                          Nhân viên
                        </p>
                      </div>
                      <span className="text-gray-400">Phụ</span>
                    </div>

                  </div>
                </div>

                <button
                  onClick={updatePhone}
                  className="w-full md:w-auto px-8 py-4 bg-blue-600 hover:bg-blue-500 rounded-2xl"
                >
                  Cập nhật số điện thoại
                </button>

              </div>
            </div>
          </div>
        )}

        {/* TAB 1 */}
        {tab === 1 && (
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-10 max-w-lg mx-auto">

            <h2 className="text-2xl font-semibold text-center mb-8">
              Đổi Mật Khẩu
            </h2>

            <div className="space-y-5">

              <input
                type="password"
                placeholder="Mật khẩu hiện tại"
                value={currentPass}
                onChange={(e) => setCurrentPass(e.target.value)}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-4"
              />

              <input
                type="password"
                placeholder="Mật khẩu mới"
                value={newPass}
                onChange={(e) => setNewPass(e.target.value)}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-4"
              />

              <input
                type="password"
                placeholder="Xác nhận mật khẩu"
                value={confirmPass}
                onChange={(e) => setConfirmPass(e.target.value)}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-4"
              />

              <button
                onClick={changePassword}
                className="w-full py-4 bg-blue-600 hover:bg-blue-500 rounded-2xl font-semibold"
              >
                Xác nhận đổi mật khẩu
              </button>

            </div>
          </div>
        )}

        {/* MODAL */}
        {modal.open && (
          <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50">
            <div className="bg-zinc-900 p-6 rounded-2xl w-80 text-center">

              <h3 className="text-xl font-bold mb-2">
                {modal.title}
              </h3>

              <p className="text-gray-400 mb-6">
                {modal.message}
              </p>

              <button
                onClick={closeModal}
                className="px-6 py-2 bg-white text-black rounded-xl"
              >
                Đóng
              </button>

            </div>
          </div>
        )}

      </div>
    </div>
  );
}