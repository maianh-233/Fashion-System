import { Component } from "react";

export default class CategoryDialog extends Component {
  constructor(props) {
    super(props);

    this.state = {
      id: "",
      parent_id: null,
      name: "",
      code: "",
      description: "",
      type: "PARENT", // PARENT | CHILD
    };
  }

  componentDidUpdate(prevProps) {
    if (
      this.props.category &&
      this.props.category !== prevProps.category
    ) {
      this.setState({
        ...this.props.category,
        type: this.props.category.parent_id ? "CHILD" : "PARENT",
      });
    }

    if (this.props.mode === "add" && prevProps.mode !== "add") {
      this.resetForm();
    }
  }

  resetForm = () => {
    this.setState({
      id: "",
      parent_id: null,
      name: "",
      code: "",
      description: "",
      type: "PARENT",
    });
  };

  handleChange = (e) => {
    this.setState({ [e.target.id]: e.target.value });
  };

  handleTypeChange = (e) => {
    const type = e.target.value;

    this.setState({
      type,
      parent_id: type === "PARENT" ? null : "",
    });
  };

  handleSubmit = () => {
    const payload = {
      ...this.state,
      parent_id: this.state.type === "PARENT" ? null : this.state.parent_id,
    };

    delete payload.type;

    this.props.onSubmit(payload);
  };

  render() {
    const { open, onClose, mode, categories = [] } = this.props;
    const isView = mode === "view";

    if (!open) return null;

    return (
      <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50">
        <div className="bg-zinc-900 border border-zinc-700 rounded-3xl w-full max-w-3xl mx-4">

          {/* HEADER */}
          <div className="px-8 py-6 border-b border-zinc-700 flex justify-between">
            <h2 className="text-xl font-semibold">
              {mode === "add" && "Thêm danh mục"}
              {mode === "edit" && "Cập nhật danh mục"}
              {mode === "view" && "Chi tiết danh mục"}
            </h2>
            <button onClick={onClose} className="text-3xl text-zinc-400">×</button>
          </div>

          {/* BODY */}
          <div className="p-6 space-y-5">

            {/* LOẠI DANH MỤC */}
            <div>
              <label className="text-sm text-gray-400">Loại danh mục *</label>
              <select
                value={this.state.type}
                onChange={this.handleTypeChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              >
                <option value="PARENT">Danh mục cha</option>
                <option value="CHILD">Danh mục con</option>
              </select>
            </div>

            {/* PARENT CATEGORY */}
            {this.state.type === "CHILD" && (
              <div>
                <label className="text-sm text-gray-400">Danh mục cha *</label>
                <select
                  id="parent_id"
                  value={this.state.parent_id || ""}
                  onChange={this.handleChange}
                  disabled={isView}
                  className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
                >
                  <option value="">-- Chọn danh mục cha --</option>
                  {categories
                    .filter((c) => !c.parent_id)
                    .map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                </select>
              </div>
            )}

            {/* NAME */}
            <div>
              <label className="text-sm text-gray-400">Tên danh mục *</label>
              <input
                id="name"
                value={this.state.name}
                onChange={this.handleChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
            </div>

            {/* CODE */}
            <div>
              <label className="text-sm text-gray-400">Mã *</label>
              <input
                id="code"
                value={this.state.code}
                onChange={this.handleChange}
                disabled={isView}
                className="uppercase w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
            </div>

            {/* DESCRIPTION */}
            <div>
              <label className="text-sm text-gray-400">Mô tả</label>
              <textarea
                id="description"
                rows="3"
                value={this.state.description}
                onChange={this.handleChange}
                disabled={isView}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
              />
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