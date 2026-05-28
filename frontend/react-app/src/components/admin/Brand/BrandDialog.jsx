import React, { Component } from "react";

export default class BrandDialog extends Component {
  constructor(props) {
    super(props);

    this.state = {
      id: "",
      name: "",
      code: "",
      logo: "",
      description: "",
      status: "ACTIVE",
      terminated_at: "",
    };
  }

  componentDidUpdate(prevProps) {
    if (this.props.brand && this.props.brand !== prevProps.brand) {
      this.setState({ ...this.props.brand });
    }

    if (this.props.mode === "add" && prevProps.mode !== "add") {
      this.resetForm();
    }
  }

  resetForm = () => {
    this.setState({
      id: "",
      name: "",
      code: "",
      logo: "",
      description: "",
      status: "ACTIVE",
      terminated_at: "",
    });
  };

  handleChange = (e) => {
    this.setState({ [e.target.id]: e.target.value });
  };

  handleSubmit = () => {
    this.props.onSubmit(this.state);
  };

  renderStatusBadge = (status) => {
    switch (status) {
      case "ACTIVE":
        return (
          <span className="px-4 py-2 rounded-full text-xs font-medium bg-emerald-500/15 text-emerald-400 border border-emerald-500/20">
            Hoạt động
          </span>
        );

      case "STOPPED":
        return (
          <span className="px-4 py-2 rounded-full text-xs font-medium bg-amber-500/15 text-amber-400 border border-amber-500/20">
            Ngừng hợp tác
          </span>
        );

      case "DELETED":
        return (
          <span className="px-4 py-2 rounded-full text-xs font-medium bg-red-500/15 text-red-400 border border-red-500/20">
            Đã xóa
          </span>
        );

      default:
        return null;
    }
  };

  render() {
    const { open, onClose, mode } = this.props;
    const isView = mode === "view";

    if (!open) return null;

    return (
      <div className="fixed inset-0 bg-black bg-opacity-80 flex items-center justify-center z-50">
        <div className="bg-zinc-900 border border-zinc-700 rounded-3xl shadow-2xl w-full max-w-2xl mx-4 overflow-hidden">
          {/* Header */}
          <div className="px-8 py-6 border-b border-zinc-700 flex justify-between items-center">
            <h2 className="text-2xl font-semibold text-white">
              {mode === "add" && "Thêm thương hiệu mới"}
              {mode === "edit" && "Cập nhật thương hiệu"}
              {mode === "view" && "Chi tiết thương hiệu"}
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
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Logo */}
              <div className="md:col-span-2 flex flex-col items-center">
                {/* Preview */}
                <div className="w-40 h-40 border-2 border-dashed border-zinc-600 rounded-3xl bg-zinc-800 flex items-center justify-center mb-4 overflow-hidden">
                  {this.state.logo ? (
                    <img
                      src={this.state.logo}
                      alt="logo"
                      className="w-full h-full object-contain"
                    />
                  ) : (
                    <i className="fas fa-image text-7xl text-zinc-600"></i>
                  )}
                </div>

                {/* Input file (hidden) */}
                <input
                  type="file"
                  id="logoFile"
                  accept="image/*"
                  hidden
                  disabled={isView}
                  onChange={this.handleLogoChange}
                />

                {/* Button chọn ảnh */}
                {!isView && (
                  <button
                    type="button"
                    onClick={() => document.getElementById("logoFile").click()}
                    className="bg-zinc-700 hover:bg-zinc-600 text-sm px-5 py-3 rounded-2xl transition"
                  >
                    Chọn ảnh logo
                  </button>
                )}
              </div>

              {/* Name */}
              <div>
                <label className="text-sm text-gray-400">
                  Tên thương hiệu *
                </label>
                <input
                  id="name"
                  value={this.state.name}
                  onChange={this.handleChange}
                  disabled={isView}
                  className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3.5 text-white"
                />
              </div>

              {/* Code */}
              <div>
                <label className="text-sm text-gray-400">
                  Mã thương hiệu *
                </label>
                <input
                  id="code"
                  value={this.state.code}
                  onChange={this.handleChange}
                  disabled={isView}
                  className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3.5 uppercase text-white"
                />
              </div>

              {/* Status */}
              {mode !== "add" && (
                <div>
                  <label className="text-sm text-gray-400 mb-1 block">
                    Trạng thái
                  </label>
                  {this.renderStatusBadge(this.state.status)}
                </div>
              )}

              {/* Terminated At */}
              {mode !== "add" && this.state.status !== "ACTIVE" && (
                <div>
                  <label className="text-sm text-gray-400">
                    Thời điểm ngừng hoạt động
                  </label>
                  <input
                    id="terminated_at"
                    type="datetime-local"
                    value={this.state.terminated_at || ""}
                    onChange={this.handleChange}
                    disabled={isView}
                    className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3.5 text-white"
                  />
                </div>
              )}

              {/* Description */}
              <div className="md:col-span-2">
                <label className="text-sm text-gray-400">Mô tả</label>
                <textarea
                  id="description"
                  rows="4"
                  value={this.state.description}
                  onChange={this.handleChange}
                  disabled={isView}
                  className="w-full bg-zinc-800 border border-zinc-700 rounded-3xl px-5 py-3.5 text-white"
                />
              </div>
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
