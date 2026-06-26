import { Component } from "react";

export default class SupplierDialog extends Component {
  constructor(props) {
    super(props);

    this.state = {
      id: "",
      code: "",
      name: "",
      contact_name: "",
      phone: "",
      email: "",
      address: "",
      status: "ACTIVE", // ACTIVE | INACTIVE
    };
  }

  componentDidUpdate(prevProps) {
    // Khi mở dialog sửa / xem
    if (this.props.supplier && this.props.supplier !== prevProps.supplier) {
      this.setState({ ...this.props.supplier });
    }

    // Khi chuyển sang chế độ thêm mới
    if (this.props.mode === "add" && prevProps.mode !== "add") {
      this.resetForm();
    }
  }

  resetForm = () => {
    this.setState({
      id: "",
      code: "",
      name: "",
      contact_name: "",
      phone: "",
      email: "",
      address: "",
      status: "ACTIVE",
    });
  };

  handleChange = (e) => {
    this.setState({ [e.target.id]: e.target.value });
  };

  handleSubmit = () => {
    const payload = { ...this.state };
    this.props.onSubmit(payload);
  };

  render() {
    const { open, onClose, mode } = this.props;
    const isView = mode === "view";

    if (!open) return null;

    return (
      <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50">
        <div className="bg-zinc-900 border border-zinc-700 rounded-3xl w-full max-w-3xl mx-4">

          {/* HEADER */}
          <div className="px-8 py-6 border-b border-zinc-700 flex justify-between">
            <h2 className="text-xl font-semibold">
              {mode === "add" && "Thêm nhà cung cấp"}
              {mode === "edit" && "Cập nhật nhà cung cấp"}
              {mode === "view" && "Chi tiết nhà cung cấp"}
            </h2>
            <button onClick={onClose} className="text-3xl text-zinc-400">×</button>
          </div>

          {/* BODY */}
          <div className="p-6 space-y-5">

            {/* CODE */}
            <div>
              <label className="text-sm text-gray-400">Mã nhà cung cấp *</label>
              <input
                id="code"
                value={this.state.code}
                onChange={this.handleChange}
                disabled={isView}
                className="uppercase w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
            </div>

            {/* NAME */}
            <div>
              <label className="text-sm text-gray-400">Tên nhà cung cấp *</label>
              <input
                id="name"
                value={this.state.name}
                onChange={this.handleChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
            </div>

            {/* CONTACT NAME */}
            <div>
              <label className="text-sm text-gray-400">Người liên hệ</label>
              <input
                id="contact_name"
                value={this.state.contact_name}
                onChange={this.handleChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
            </div>

            {/* PHONE */}
            <div>
              <label className="text-sm text-gray-400">Số điện thoại</label>
              <input
                id="phone"
                value={this.state.phone}
                onChange={this.handleChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
            </div>

            {/* EMAIL */}
            <div>
              <label className="text-sm text-gray-400">Email</label>
              <input
                id="email"
                type="email"
                value={this.state.email}
                onChange={this.handleChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
            </div>

            {/* ADDRESS */}
            <div>
              <label className="text-sm text-gray-400">Địa chỉ</label>
              <textarea
                id="address"
                rows="3"
                value={this.state.address}
                onChange={this.handleChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
            </div>

            {/* STATUS */}
            <div>
              <label className="text-sm text-gray-400">Trạng thái</label>
              <select
                id="status"
                value={this.state.status}
                onChange={this.handleChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              >
                <option value="ACTIVE">Hoạt động</option>
                <option value="INACTIVE">Ngưng hoạt động</option>
              </select>
            </div>
          </div>

          {/* FOOTER */}
          {mode !== "view" && (
            <div className="px-8 py-5 border-t border-zinc-700 flex justify-end gap-3">
              <button onClick={onClose} className="px-5 py-2 bg-zinc-700 rounded-xl">
                Hủy
              </button>
              <button onClick={this.handleSubmit} className="px-5 py-2 bg-blue-600 rounded-xl">
                {mode === "add" ? "Thêm mới" : "Lưu thay đổi"}
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }
}