import Button from "../../common/Button";
import { Component } from "react";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

export default class TagDialog extends Component {
  constructor(props) {
    super(props);

    this.state = {
      id: "",
      name: "",
      created_at: "",
    };
  }

  componentDidUpdate(prevProps) {
    if (this.props.tag && this.props.tag !== prevProps.tag) {
      this.setState({ ...this.props.tag });
    }

    if (this.props.mode === "add" && prevProps.mode !== "add") {
      this.resetForm();
    }
  }

  resetForm = () => {
    this.setState({
      id: "",
      name: "",
      created_at: "",
    });
  };

  handleChange = (e) => {
    this.setState({ [e.target.id]: e.target.value });
  };

  handleSubmit = () => {
    this.props.onSubmit(this.state);
  };

  render() {
    const { open, onClose, mode } = this.props;
    const isView = mode === "view";
    const title = {
      add: "Thêm tag sản phẩm",
      edit: "Cập nhật tag",
      view: "Chi tiết tag",
    }[mode];

    if (!open) return null;

    return (
      <AdminDialog open={open} onClose={onClose} size="sm">
          <AdminDialogHeader title={title} onClose={onClose} />

          {/* Body */}
          <AdminDialogBody>
            <div className="space-y-6">

              {/* ID */}
              {mode !== "add" && (
                <div>
                  <label className="text-sm text-gray-400">ID</label>
                  <input
                    value={this.state.id}
                    disabled
                    className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3 text-white"
                  />
                </div>
              )}

              {/* Name */}
              <div>
                <label className="text-sm text-gray-400">
                  Tên tag *
                </label>
                <input
                  id="name"
                  value={this.state.name}
                  onChange={this.handleChange}
                  disabled={isView}
                  placeholder="Ví dụ: New, Sale, Trending..."
                  className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3 text-white"
                />
              </div>

              {/* Created At */}
              {mode !== "add" && (
                <div>
                  <label className="text-sm text-gray-400">
                    Thời điểm tạo
                  </label>
                  <input
                    value={this.state.created_at || ""}
                    disabled
                    className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3 text-white"
                  />
                </div>
              )}

            </div>
          </AdminDialogBody>

          {/* Footer */}
          {mode !== "view" && (
            <AdminDialogFooter>
              <Button
                onClick={onClose}
                className="px-6 py-2 rounded-xl bg-zinc-700 hover:bg-zinc-600"
              >
                Hủy
              </Button>

              <Button
                onClick={this.handleSubmit}
                className="px-6 py-2 rounded-xl bg-blue-600 hover:bg-blue-500"
              >
                {mode === "add" ? "Thêm mới" : "Lưu thay đổi"}
              </Button>
            </AdminDialogFooter>
          )}
      </AdminDialog>
    );
  }
}
