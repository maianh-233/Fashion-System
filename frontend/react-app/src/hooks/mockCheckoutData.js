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
      image: "https://picsum.photos/id/20/80",
    },
    {
      name: "Quần Cargo Jogger",
      price: 1240000,
      quantity: 1,
      color: "Đen",
      size: "M",
      image: "https://picsum.photos/id/64/80",
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

export const store = {
  name: "Chi nhánh Quận 1",
  address: "45 Lê Lợi, Q1, TP.HCM",
  phone: "028 3824 1234",
  latitude: 10.7762,
  longitude: 106.701,
};