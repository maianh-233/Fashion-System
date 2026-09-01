import { defaultStore, stores } from "./stores";

export { stores };

export const orderData = {
  subtotal: 1890000,
  discount_total: 320000,
  shipping_fee: 35000,
  tax: 45000,
  items: [
    {
      name: "Áo Hoodie Oversize",
      price: 650000,
      quantity: 1,
      color: "Be",
      size: "L",
      image: "https://images.unsplash.com/photo-1556821840-3a63f95609a7?q=85&w=500&auto=format&fit=crop",
    },
    {
      name: "Quần Cargo Jogger",
      price: 1240000,
      quantity: 1,
      color: "Đen",
      size: "M",
      image: "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?q=85&w=500&auto=format&fit=crop",
    },
  ],
  promotions: [
    { code: "FASHION20", name: "Giảm 20%", discount: 250000 },
    { code: "FREESHIP", name: "Miễn phí vận chuyển", discount: 35000 },
    { code: "VIP10", name: "VIP giảm thêm 10%", discount: 35000 },
  ],
};

export const savedAddresses = [
  {
    id: "addr1",
    receiver_name: "Trần Minh Trang",
    receiver_phone: "0909123456",
    province: "TP. Hồ Chí Minh",
    district: "Quận 3",
    ward: "Phường Võ Thị Sáu",
    address_line: "28 Trần Quốc Thảo",
    latitude: 10.7769,
    longitude: 106.7009,
    is_default: false,
  },
    {
    id: "addr2",
    receiver_name: "Nguyễn Văn A",
    receiver_phone: "0987654321",
    province: "TP. Hồ Chí Minh",
    district: "Quận 1",
    ward: "Phường Bến Nghé",
    address_line: "123 Nguyễn Huệ",
    latitude: 10.7769,
    longitude: 106.7009,
    is_default: true,
  },
];

export const store = defaultStore;
