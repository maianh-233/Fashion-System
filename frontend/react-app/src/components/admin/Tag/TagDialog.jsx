import { Component } from "react";

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

    if (!open) return null;

    return (
      <div className="fixed inset-0 bg-black bg-opacity-80 flex items-center justify-center z-50">
        <div className="bg-zinc-900 border border-zinc-700 rounded-3xl shadow-2xl w-full max-w-lg mx-4 overflow-hidden">

          {/* Header */}
          <div className="px-8 py-6 border-b border-zinc-700 flex justify-between items-center">
            <h2 className="text-2xl font-semibold text-white">
              {mode === "add" && "Thêm tag sản phẩm"}
              {mode === "edit" && "Cập nhật tag"}
              {mode === "view" && "Chi tiết tag"}
            </h2>

            <button
              onClick={onClose}
              className="text-4xl text-zinc-400 hover:text-white"
            >
              ×
            </button>
          </div>

          {/* Body */}
          <div className="p-8">
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
          </div>

          {/* Footer */}
          {mode !== "view" && (
            <div className="px-8 py-5 border-t border-zinc-700 flex justify-end gap-3">
              <button
                onClick={onClose}
                className="px-6 py-2 rounded-xl bg-zinc-700 hover:bg-zinc-600"
              >
                Hủy
              </button>

              <button
                onClick={this.handleSubmit}
                className="px-6 py-2 rounded-xl bg-blue-600 hover:bg-blue-500"
              >
                {mode === "add" ? "Thêm mới" : "Lưu thay đổi"}
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }
}