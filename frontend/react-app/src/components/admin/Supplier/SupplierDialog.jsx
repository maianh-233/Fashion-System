import Button from "../../common/Button";
import { Component } from "react";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
    const title = {
      add: "Thêm nhà cung cấp",
      edit: "Cập nhật nhà cung cấp",
      view: "Chi tiết nhà cung cấp",
    }[mode];

    if (!open) return null;

    return (
      <AdminDialog open={open} onClose={onClose} size="md">
          <AdminDialogHeader title={title} onClose={onClose} />

          {/* BODY */}
          <AdminDialogBody className="space-y-5">

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
          </AdminDialogBody>

          {/* FOOTER */}
          {mode !== "view" && (
            <AdminDialogFooter>
              <Button onClick={onClose} className="px-5 py-2 bg-zinc-700 rounded-xl">
                Hủy
              </Button>
              <Button onClick={this.handleSubmit} className="px-5 py-2 bg-blue-600 rounded-xl">
                {mode === "add" ? "Thêm mới" : "Lưu thay đổi"}
              </Button>
            </AdminDialogFooter>
          )}
      </AdminDialog>
    );
  }
}
