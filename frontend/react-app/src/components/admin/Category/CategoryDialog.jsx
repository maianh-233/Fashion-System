import Button from "../../common/Button";
import { Component } from "react";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
    const title = {
      add: "Thêm danh mục",
      edit: "Cập nhật danh mục",
      view: "Chi tiết danh mục",
    }[mode];

    if (!open) return null;

    return (
      <AdminDialog open={open} onClose={onClose} size="md">
          <AdminDialogHeader title={title} onClose={onClose} />

          {/* BODY */}
          <AdminDialogBody className="space-y-5">

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
