// CustomerInfoView.jsx
import InfoRow from "./InfoRow";

export default function CustomerInfoView({ customer }) {
  return (
    <div className="flex-1 grid grid-cols-1 sm:grid-cols-2 gap-6">
      <InfoRow label="EMAIL" value={customer.email} />
      <InfoRow label="SỐ ĐIỆN THOẠI" value={customer.phone} />
      <InfoRow label="GIỚI TÍNH" value={customer.gender} />
      <InfoRow label="NGÀY SINH" value={customer.dob} />
      <InfoRow
        label="TỔNG TIỀN ĐÃ CHI"
        value={`${customer.totalSpent.toLocaleString()} ₫`}
        big
      />
      <InfoRow label="ĐIỂM THƯỞNG" value={customer.points} big />
    </div>
  );
}