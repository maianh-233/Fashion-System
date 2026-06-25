// src/mock/mockCustomer.js
const mockCustomer = {
  id: 1,
  name: "Nguyễn Văn An",
  email: "an.nguyen@example.com",
  phone: "0987 654 321",
  gender: "Nam",
  dob: "15/03/1995",
  avatar: "https://picsum.photos/id/64/300/300",
  totalSpent: 42850000,
  points: 2450,
  tier: "Khách hàng Vàng",
  addresses: [
    {
      label: "Nhà riêng",
      address:
        "123 Đường Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
      isDefault: true,
    },
    {
      label: "Công ty",
      address:
        "Tầng 15, Tòa nhà Bitexco, 2 Hải Triều, Quận 1, TP. Hồ Chí Minh",
    },
  ],
};

export default mockCustomer;