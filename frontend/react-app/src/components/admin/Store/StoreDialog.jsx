import { Component } from "react";
import StoreDialogHeader from "./StoreDialogHeader";
import StoreBasicInfoForm from "./StoreBasicInfoForm";
import StoreMapView from "./StoreMapView";
import StoreStaffList from "./StoreStaffList";
import StoreDialogFooter from "./StoreDialogFooter";

export default class StoreDialog extends Component {
  constructor(props) {
    super(props);

    this.state = {
      id: "",
      code: "",
      name: "",
      phone: "",
      address: "",
      latitude: "",
      longitude: "",
      active: true,
    };
  }

  componentDidUpdate(prevProps) {
    if (this.props.store && this.props.store !== prevProps.store) {
      this.setState({ ...this.props.store });
    }

    if (this.props.mode === "add" && prevProps.mode !== "add") {
      this.resetForm();
    }
  }

  resetForm = () => {
    this.setState({
      id: "",
      code: "",
      name: "",
      phone: "",
      address: "",
      latitude: "",
      longitude: "",
      active: true,
    });
  };

  handleChange = (e) => {
    const { id, value } = e.target;
    this.setState({ [id]: value });
  };

  handleSubmit = () => {
    this.props.onSubmit(this.state);
  };

  render() {
    const { open, onClose, mode, staffData } = this.props;
    const isView = mode === "view";

    if (!open) return null;

    return (
      <div
        className="fixed inset-0 bg-black/80 
                  flex items-center justify-center z-50"
      >
        <div
          className="bg-zinc-900 border border-zinc-700 rounded-3xl 
                    w-full max-w-5xl mx-4 
                    h-[90vh] flex flex-col overflow-hidden  "
        >
          <div className="flex-1 overflow-y-auto scrollbar-hide">
            <StoreDialogHeader mode={mode} onClose={onClose} />

            <div className="p-6 space-y-6">
              {/* THÔNG TIN CƠ BẢN */}
              <StoreBasicInfoForm
                data={this.state}
                onChange={this.handleChange}
                isView={isView}
              />

              {/* MAP */}
              <StoreMapView
                latitude={this.state.latitude}
                longitude={this.state.longitude}
              />

              {/* NHÂN VIÊN - CHỈ HIỆN KHI VIEW */}
              {isView && (
              
                <StoreStaffList staffData={staffData} />
                
              )}
            </div>

            <StoreDialogFooter
              mode={mode}
              onClose={onClose}
              onSubmit={this.handleSubmit}
            />
          </div>
        </div>
      </div>
    );
  }
}
