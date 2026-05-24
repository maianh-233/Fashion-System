export const getStatusText = (status) => {
  const map = {
    PENDING: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    SHIPPING: "Đang giao",
    DELIVERED: "Đã nhận",
    CANCELLED: "Đã hủy",
  };
  return map[status] || status;
};

export const getStatusColor = (status) => {
  const map = {
    PENDING: "text-yellow-400",
    SHIPPING: "text-cyan-400",
    DELIVERED: "text-emerald-400",
    CANCELLED: "text-red-400",
  };
  return map[status] || "text-zinc-400";
};