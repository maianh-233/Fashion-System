import  { Component } from "react";

export default class CollectionDialog extends Component {
  constructor(props) {
    super(props);

  this.state = {
    id: "",
    brand_id: "",
    name: "",
    code: "",
    season: "",
    year: "",
    release_date: "",
    description: "",
    image: "",
    status: "ACTIVE",
  };
  }
  componentDidUpdate(prevProps) {
    if (
      this.props.collection &&
      this.props.collection !== prevProps.collection
    ) {
      this.setState({ ...this.props.collection });
    }

    if (this.props.mode === "add" && prevProps.mode !== "add") {
      this.resetForm();
    }
  }

  resetForm = () => {
    this.setState({
      id: "",
      brand_id: "",
      name: "",
      code: "",
      season: "",
      year: "",
      release_date: "",
      description: "",
      image: "",
      status: "ACTIVE",
    });
  };

  handleChange = (e) => {
    this.setState({ [e.target.id]: e.target.value });
  };

  handleImageChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      this.setState({ image: reader.result });
    };
    reader.readAsDataURL(file);
  };

  handleSubmit = () => {
    this.props.onSubmit(this.state);
  };

  renderStatusBadge = (status) => {
    if (status === "ACTIVE") {
      return (
        <span className="px-4 py-2 rounded-full text-xs bg-emerald-500/15 text-emerald-400 border border-emerald-500/20">
          Hoạt động
        </span>
      );
    }
    return (
      <span className="px-4 py-2 rounded-full text-xs bg-amber-500/15 text-amber-400 border border-amber-500/20">
        Ngừng hoạt động
      </span>
    );
  };

  render() {
    const { open, onClose, mode, brands = [] } = this.props;
    const isView = mode === "view";

    if (!open) return null;

    return (
      <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50">
        <div
          className="bg-zinc-900 border border-zinc-700 rounded-3xl 
                        w-full max-w-5xl mx-4 
                        max-h-[90vh] flex flex-col overflow-hidden"
        >
          {/* ================= HEADER ================= */}
          <div className="px-8 py-6 border-b border-zinc-700 flex justify-between items-center">
            <h2 className="text-2xl font-semibold">
              {mode === "add" && "Thêm bộ sưu tập"}
              {mode === "edit" && "Cập nhật bộ sưu tập"}
              {mode === "view" && "Chi tiết bộ sưu tập"}
            </h2>
            <button
              onClick={onClose}
              className="text-4xl text-zinc-400 hover:text-white"
            >
              ×
            </button>
          </div>

          {/* ================= BODY ================= */}
          <div className="flex flex-1 overflow-hidden">
            {/* ==== LEFT: IMAGE ==== */}
            <div className="w-80 border-r border-zinc-700 p-6 flex flex-col items-center gap-4">
              <div
                onClick={() =>
                  !isView && document.getElementById("collectionImage").click()
                }
                className="w-full aspect-square border-2 border-dashed border-zinc-600 
                           rounded-3xl bg-zinc-800 flex items-center justify-center 
                           overflow-hidden cursor-pointer"
              >
                {this.state.image ? (
                  <img
                    src={this.state.image}
                    alt="collection"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <i className="fas fa-image text-6xl text-zinc-600"></i>
                )}
              </div>

              {!isView && (
                <button className="text-sm text-blue-400 hover:underline">
                  Chọn ảnh bộ sưu tập
                </button>
              )}

              <input
                id="collectionImage"
                type="file"
                accept="image/*"
                hidden
                onChange={this.handleImageChange}
              />
            </div>

            {/* ==== RIGHT: FORM (SCROLL) ==== */}
            <div className="flex-1 overflow-y-auto p-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="text-sm text-gray-400">Thương hiệu *</label>
                  <select
                    id="brand_id"
                    value={this.state.brand_id}
                    onChange={this.handleChange}
                    disabled={isView}
                    className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3.5"
                  >
                    <option value="">-- Chọn thương hiệu --</option>
                    {brands.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="text-sm text-gray-400">Bộ sưu tập *</label>

                  <input
                    id="name"
                    list="collectionList"
                    value={this.state.name}
                    onChange={this.handleChange}
                    onBlur={this.handleCollectionBlur}   // 👈 SỰ KIỆN GIẢ
                    disabled={isView}
                    placeholder="Chọn hoặc nhập bộ sưu tập"
                    className="w-full bg-zinc-800 border border-zinc-700 
                              rounded-2xl px-5 py-3.5"
                  />

                </div>

                <div>
                  <label className="text-sm text-gray-400">Mã *</label>
                  <input
                    id="code"
                    value={this.state.code}
                    onChange={this.handleChange}
                    disabled={isView}
                    className="uppercase w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3.5"
                  />
                </div>

                <div>
                  <label className="text-sm text-gray-400">Mùa</label>
                  <select
                    id="season"
                    value={this.state.season}
                    onChange={this.handleChange}
                    disabled={isView}
                    className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3.5"
                  >
                    <option value="">-- Chọn mùa --</option>
                    <option>Spring</option>
                    <option>Summer</option>
                    <option>Fall</option>
                    <option>Winter</option>
                  </select>
                </div>

                <div>
                  <label className="text-sm text-gray-400">Năm</label>
                  <input
                    id="year"
                    type="number"
                    value={this.state.year}
                    onChange={this.handleChange}
                    disabled={isView}
                    className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3.5"
                  />
                </div>

                <div>
                  <label className="text-sm text-gray-400">Ngày ra mắt</label>
                  <input
                    id="release_date"
                    type="date"
                    value={this.state.release_date || ""}
                    onChange={this.handleChange}
                    disabled={isView}
                    className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3.5"
                  />
                </div>

                {mode !== "add" && (
                  <div className="md:col-span-2">
                    <label className="text-sm text-gray-400 block mb-1">
                      Trạng thái
                    </label>
                    {this.renderStatusBadge(this.state.status)}
                  </div>
                )}

                <div className="md:col-span-2">
                  <label className="text-sm text-gray-400">Mô tả</label>
                  <textarea
                    id="description"
                    rows="4"
                    value={this.state.description}
                    onChange={this.handleChange}
                    disabled={isView}
                    className="w-full bg-zinc-800 border border-zinc-700 rounded-3xl px-5 py-3.5"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* ================= FOOTER ================= */}
          {mode !== "view" && (
            <div className="px-8 py-5 border-t border-zinc-700 flex justify-end gap-3">
              <button
                onClick={onClose}
                className="px-6 py-2 rounded-xl bg-zinc-700"
              >
                Hủy
              </button>
              <button
                onClick={this.handleSubmit}
                className="px-6 py-2 rounded-xl bg-blue-600"
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
