export const mockProducts = [
  {
    id: "P001",
    name: "Áo Thun Oversize",
    brand: "Routine",
    image: "https://placehold.co/300x300",

    variants: [
      {
        id: "V001",
        sku: "AT-Black-M",
        color: "Đen",
        size: "M",
        price: 250000,
      },
      {
        id: "V002",
        sku: "AT-White-L",
        color: "Trắng",
        size: "L",
        price: 250000,
      },
    ],
  },

  {
    id: "P002",
    name: "Quần Jean Slim Fit",
    brand: "Coolmate",
    image: "https://placehold.co/300x300",

    variants: [
      {
        id: "V003",
        sku: "QJ-30",
        color: "Xanh",
        size: "30",
        price: 450000,
      },
      {
        id: "V004",
        sku: "QJ-32",
        color: "Đen",
        size: "32",
        price: 450000,
      },
    ],
  },
];

export const mockPromotions = [
  {
    id: "PR001",
    promotionCode: "SALE10",
    name: "Giảm 10% toàn đơn",
    discountAmount: 100000,
    minOrderValue: 500000,
  },
  {
    id: "PR002",
    promotionCode: "VIP50",
    name: "Khách hàng VIP",
    discountAmount: 50000,
    minOrderValue: 300000,
  },
  {
    id: "PR003",
    promotionCode: "FREESHIP",
    name: "Miễn phí vận chuyển",
    discountAmount: 30000,
    minOrderValue: 200000,
  },
];

export const mockStatusHistories = [
  {
    id: "1",
    fromStatus: "PENDING",
    toStatus: "CONFIRMED",
    changedBy: "EMP001",
    changedByName: "Nguyễn Văn A",
    note: "Đã xác nhận đơn hàng",
    changedAt: "2026-07-27T08:00:00",
  },
  {
    id: "2",
    fromStatus: "CONFIRMED",
    toStatus: "PROCESSING",
    changedBy: "EMP002",
    changedByName: "Trần Thị B",
    note: "Chuẩn bị đóng gói",
    changedAt: "2026-07-27T09:20:00",
  },
  {
    id: "3",
    fromStatus: "PROCESSING",
    toStatus: "SHIPPING",
    changedBy: "EMP003",
    changedByName: "Lê Văn C",
    note: "Đã bàn giao cho đơn vị vận chuyển",
    changedAt: "2026-07-27T10:30:00",
  },
  {
    id: "4",
    fromStatus: "SHIPPING",
    toStatus: "DELIVERED",
    changedBy: "EMP003",
    changedByName: "Lê Văn C",
    note: "Khách đã nhận hàng",
    changedAt: "2026-07-28T15:45:00",
  },
];

export const mockSuppliers = [
  {
    id: "sup-001",
    code: "NCC001",
    name: "Công ty Thời Trang ABC",
    contactName: "Nguyễn Văn A",
    phone: "0901234567",
    email: "abc@gmail.com",
    address: "Quận 1, TP.HCM",
    status: "ACTIVE",
  },
  {
    id: "sup-002",
    code: "NCC002",
    name: "Xưởng May Minh Phát",
    contactName: "Trần Thị B",
    phone: "0912345678",
    email: "minhphat@gmail.com",
    address: "Bình Tân, TP.HCM",
    status: "ACTIVE",
  },
  {
    id: "sup-003",
    code: "NCC003",
    name: "Phụ kiện Fashion Plus",
    contactName: "Lê Văn C",
    phone: "0988888888",
    email: "fashionplus@gmail.com",
    address: "Thủ Đức, TP.HCM",
    status: "INACTIVE",
  },
];
export const mockReceiptStatusHistories = [
  {
    id: 1,
    status: "PENDING",
    description: "Tạo phiếu nhập",
    createdBy: "Admin",
    createdAt: "2026-07-31 08:00",
  },
  {
    id: 2,
    status: "APPROVED",
    description: "Đã duyệt phiếu nhập",
    createdBy: "Quản lý",
    createdAt: "2026-07-31 09:30",
  },
  {
    id: 3,
    status: "RECEIVED",
    description: "Đã nhập kho",
    createdBy: "Thủ kho",
    createdAt: "2026-07-31 10:15",
  },
];

export const mockIssueStatusHistories = [
  {
    id: "ISH001",
    status: "PENDING",
    time: "01/08/2026 09:00",
    description: "Tạo phiếu xuất kho.",
    user: "Nguyễn Văn A",
  },
  {
    id: "ISH002",
    status: "APPROVED",
    time: "01/08/2026 09:15",
    description: "Phiếu xuất kho đã được duyệt.",
    user: "Trần Văn B",
  },
  {
    id: "ISH003",
    status: "ISSUED",
    time: "01/08/2026 09:30",
    description: "Đã xuất hàng khỏi kho.",
    user: "Nguyễn Văn A",
  },
];