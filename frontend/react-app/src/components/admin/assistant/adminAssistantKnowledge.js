// Trợ lý quản trị theo kịch bản cố định, không gọi AI hay API bên thứ ba.
// Câu hỏi được bỏ dấu rồi so khớp từ khóa với từng chủ đề; không khớp chủ đề nào thì bot từ chối vì
// câu hỏi nằm ngoài phạm vi hệ thống. Nội dung trả lời bám theo quy tắc nghiệp vụ đang chạy.

export const ASSISTANT_NAME = "Trợ lý Lunaria";

export const topics = [
  {
    id: "dashboard",
    title: "Trang tổng quan",
    keywords: ["tong quan", "dashboard", "trang chu", "doanh thu hom nay", "don moi nhat", "ban chay"],
    path: "/admin",
    permission: "DASHBOARD_VIEW",
    answer:
      "Trang Tổng quan hiển thị số liệu trực tiếp: doanh thu, số đơn, sản phẩm bán chạy, hàng sắp hết, phiếu nhập/xuất gần đây.\n" +
      "• Doanh thu chỉ tính đơn Hoàn thành, theo ngày tạo đơn.\n" +
      "• Số đơn không tính đơn đã hủy, đơn đang chờ thanh toán online hoặc đã bỏ dở.\n" +
      "• Nhân viên cấp chuỗi có thể xem toàn chuỗi; nhân viên cửa hàng chỉ thấy cửa hàng mình.",
  },
  {
    id: "statistics",
    title: "Thống kê & báo cáo",
    keywords: ["thong ke", "bao cao", "loi nhuan", "xuat excel", "excel", "csv", "doanh so"],
    path: "/admin/statistics",
    permission: "STATISTICS_VIEW",
    answer:
      "Vào Báo cáo → Thống kê để xem doanh thu, đơn hàng và lợi nhuận theo khoảng thời gian.\n" +
      "• Lợi nhuận gộp là số ước tính: doanh thu − phí vận chuyển − giá vốn bình quân từ phiếu nhập đã hoàn thành.\n" +
      "• Có bảng chia theo phương thức thanh toán.\n" +
      "• Nút xuất Excel tải về file CSV (mở được bằng Excel, giữ đúng tiếng Việt).",
  },
  {
    id: "orders",
    title: "Quản lý đơn hàng",
    keywords: ["don hang", "quan ly don", "trang thai don", "xem don", "tim don", "don tach", "tach don", "don online"],
    path: "/admin/orders",
    permission: "ORDER_VIEW",
    answer:
      "Vào Bán hàng → Đơn hàng để xem, lọc và xử lý đơn.\n" +
      "• Hệ thống có 2 loại đơn: ONLINE (khách đặt trên web) và OFFLINE (bán tại quầy).\n" +
      "• Đơn online thiếu hàng ở một cửa hàng sẽ được tách cho nhiều cửa hàng: vẫn là một đơn của khách, mỗi cửa hàng xử lý phần của mình; phí ship và voucher tính trên đơn tổng.\n" +
      "• Đơn VNPay/MoMo chưa thanh toán sẽ bị ẩn khỏi cửa hàng cho tới khi thanh toán xong.",
  },
  {
    id: "pos",
    title: "Bán tại quầy (POS)",
    keywords: ["ban tai quay", "pos", "tai quay", "don offline", "offline", "tao don", "thu ngan"],
    path: "/admin/orders/pos",
    permission: "ORDER_CREATE",
    answer:
      "Bán tại quầy: vào Đơn hàng → bấm \"Bán tại quầy\".\n" +
      "• Đơn tại quầy trừ vào tồn kho khả dụng của cửa hàng (không dùng tồn bán online).\n" +
      "• Nhập khách thành viên để được giảm giá theo hạng, dùng voucher và tích điểm; khách vãng lai không tích điểm.\n" +
      "• Đơn tại quầy hoàn thành ngay khi thanh toán.",
  },
  {
    id: "cancel",
    title: "Hủy đơn & hoàn tiền",
    keywords: ["huy don", "huy", "hoan tien", "refund", "tra tien"],
    path: "/admin/orders",
    permission: "ORDER_VIEW",
    answer:
      "Quy tắc hủy đơn:\n" +
      "• Đơn COD/tiền mặt hủy bình thường theo trạng thái đơn.\n" +
      "• Đơn đã thanh toán VNPay/MoMo chỉ được hủy khi đơn vị vận chuyển giao thất bại hoặc không tìm được tài xế. Khi đó hệ thống tạo phiếu hoàn tiền trạng thái CHỜ XỬ LÝ để kế toán hoàn thủ công.\n" +
      "• Tiền về sau khi đơn đã hủy (hoặc thanh toán trùng) cũng tạo phiếu hoàn tiền chờ xử lý.",
  },
  {
    id: "payment",
    title: "Thanh toán",
    keywords: ["thanh toan", "vnpay", "momo", "cod", "chuyen khoan", "chua thanh toan", "giu hang"],
    path: "/admin/orders",
    permission: "ORDER_VIEW",
    answer:
      "Các hình thức thanh toán: COD, VNPay, MoMo (online) và tiền mặt tại quầy.\n" +
      "• VNPay/MoMo giữ hàng 15 phút. Quá hạn chưa trả tiền thì hệ thống tự hủy, trả lại tồn kho, voucher KHÔNG được hoàn.\n" +
      "• Thanh toán lỗi: khách có thể thử lại bằng link mới hoặc đổi sang COD.\n" +
      "• Đơn COD: khi đơn vị vận chuyển báo giao thành công, hệ thống tự ghi nhận đã thu tiền (đơn tách thì mỗi phần thu phần của mình).",
  },
  {
    id: "shipping",
    title: "Vận chuyển",
    keywords: ["van chuyen", "giao hang", "lalamove", "tai xe", "shipper", "phi ship", "ship"],
    path: "/admin/orders",
    permission: "ORDER_VIEW",
    answer:
      "Đơn online được giao qua Lalamove.\n" +
      "• Trạng thái chuyến (tìm tài xế, đã lấy hàng, giao thành công/thất bại) cập nhật tự động từ Lalamove.\n" +
      "• Thông tin tài xế và link theo dõi xem trong chi tiết đơn.\n" +
      "• Giao thất bại thì cửa hàng sắp xếp lại; đơn VNPay/MoMo lúc này mới được phép hủy.",
  },
  {
    id: "support",
    title: "Hỗ trợ đơn hàng (chat với khách)",
    keywords: ["ho tro don", "chat voi khach", "tin nhan khach", "khach nhan tin", "support", "bot ho tro"],
    path: "/admin/support",
    permission: "ORDER_SUPPORT_CHAT",
    answer:
      "Bán hàng → Hỗ trợ đơn hàng là nơi nhân viên trả lời chat của khách về đơn.\n" +
      "• Bot tự động trả lời trước: trạng thái đơn, thông tin vận chuyển và các câu hỏi chính sách (đổi trả, hủy, giao hàng, thanh toán).\n" +
      "• Bot không bao giờ tự hủy đơn hay thao tác thay khách.\n" +
      "• Khi khách bấm \"Gặp nhân viên\" hoặc bot không hiểu câu hỏi, cuộc trò chuyện được chuyển cho nhân viên.",
  },
  {
    id: "returns",
    title: "Đổi trả",
    keywords: ["doi tra", "tra hang", "doi hang", "doi size", "yeu cau doi", "hang loi"],
    path: "/admin/returns",
    permission: "RETURN_VIEW",
    answer:
      "Bán hàng → Đổi trả để duyệt yêu cầu của khách.\n" +
      "• Khách gửi yêu cầu online trong 14 ngày kể từ khi đơn hoàn thành: trả hàng hoàn tiền, hoặc đổi sang màu/size khác của cùng sản phẩm.\n" +
      "• Cửa hàng duyệt, khách mang hàng tới cửa hàng đã bán.\n" +
      "• Khi nhận hàng, nhân viên chọn từng món: còn tốt → về tồn quầy, lỗi → tồn hàng hỏng.\n" +
      "• Hoàn tiền: tiền mặt/COD hoàn tại quầy; VNPay/MoMo tạo phiếu hoàn chờ kế toán xử lý.",
  },
  {
    id: "return-report",
    title: "Báo cáo đổi trả theo size",
    keywords: ["ty le doi tra", "ty le tra", "bao cao doi tra", "tra theo size", "sai size"],
    path: "/admin/returns?tab=size",
    permission: "RETURN_VIEW",
    answer:
      "Trang Đổi trả có tab báo cáo theo sản phẩm và size.\n" +
      "• Tính các yêu cầu Đã duyệt và Hoàn tất, cả trả hàng lẫn đổi hàng.\n" +
      "• Tỷ lệ = số lượng trả / số lượng đã bán của từng sản phẩm + size.\n" +
      "• Lý do \"Sai size\" được làm nổi bật để điều chỉnh bảng size.",
  },
  {
    id: "customers",
    title: "Khách hàng",
    keywords: ["khach hang", "thong tin khach", "tim khach", "sinh nhat khach", "ngay sinh"],
    path: "/admin/customers",
    permission: "CUSTOMER_VIEW",
    answer:
      "Bán hàng → Khách hàng để xem và cập nhật thông tin khách.\n" +
      "• Khách chỉ tự đặt ngày sinh được một lần; muốn sửa sau đó phải nhờ cửa hàng sửa ở trang này.\n" +
      "• Tab \"Hạng khách hàng\" dùng để quản lý hạng thành viên.",
  },
  {
    id: "tiers",
    title: "Hạng khách hàng",
    keywords: ["hang khach", "hang thanh vien", "thanh vien", "tier", "len hang", "giam gia theo hang", "vip"],
    path: "/admin/customers",
    permission: "CUSTOMER_VIEW",
    answer:
      "Hạng khách hàng nằm ở tab \"Hạng khách hàng\" trong trang Khách hàng.\n" +
      "• Khách tự lên hạng theo tổng chi tiêu, không bao giờ bị hạ hạng.\n" +
      "• % giảm giá của hạng tự áp dụng cho cả đơn online và tại quầy (khách thành viên).\n" +
      "• Thứ tự giảm: flash sale / khuyến mãi sản phẩm → % theo hạng trên số tiền còn lại → voucher đơn hàng tính sau cùng.\n" +
      "• Sửa hạng cần quyền CUSTOMER_TIER_MANAGE cấp chuỗi. Hạng REGULAR không xóa, không đổi tên, không đặt ngưỡng được; các ngưỡng không được trùng nhau.",
  },
  {
    id: "loyalty",
    title: "Điểm thưởng",
    keywords: ["diem thuong", "tich diem", "diem", "loyalty", "doi diem", "het han diem"],
    path: "/admin/customers",
    permission: "CUSTOMER_VIEW",
    answer:
      "Quy tắc điểm thưởng:\n" +
      "• 1 điểm cho mỗi 1.000đ tiền hàng thực trả (sau giảm giá, không tính phí ship, làm tròn xuống).\n" +
      "• Điểm được cộng khi đơn Hoàn thành (online = khi giao xong từng phần; tại quầy = khi thanh toán).\n" +
      "• Hoàn tiền sẽ trừ điểm và tổng chi tiêu theo tỷ lệ; số dư không âm.\n" +
      "• Mỗi lần tích là một đợt, hết hạn sau 12 tháng; khi dùng/trừ điểm thì trừ đợt cũ nhất trước. Khách được nhắc trước 30 ngày và 7 ngày.",
  },
  {
    id: "promotions",
    title: "Khuyến mãi & voucher",
    keywords: ["khuyen mai", "voucher", "ma giam gia", "giam gia", "coupon", "promotion"],
    path: "/admin/promotions",
    permission: "PROMOTION_VIEW",
    answer:
      "Bán hàng → Khuyến mãi để tạo và quản lý chương trình.\n" +
      "• Mỗi đơn dùng tối đa 1 voucher cấp đơn (giảm đơn hoặc đổi điểm) + 1 khuyến mãi sản phẩm cho mỗi dòng hàng.\n" +
      "• Khuyến mãi áp dụng cho tất cả cửa hàng.\n" +
      "• Từ trang này có nút sang Flash sale và Chiến dịch voucher.",
  },
  {
    id: "flash-sale",
    title: "Flash sale",
    keywords: ["flash sale", "flash", "gia soc", "sale gio vang"],
    path: "/admin/flash-sales",
    permission: "PROMOTION_VIEW",
    answer:
      "Flash sale: giá sale theo khung giờ cho các biến thể được chọn, có tổng số suất.\n" +
      "• Mỗi khách mua tối đa 2 sản phẩm giá flash cho mỗi biến thể, mua thêm thì tính giá thường.\n" +
      "• Hàng giá flash không được áp thêm khuyến mãi sản phẩm, nhưng voucher đơn hàng vẫn dùng được.\n" +
      "• Flash sale chỉ áp dụng cho đơn online.",
  },
  {
    id: "campaigns",
    title: "Chiến dịch voucher",
    keywords: ["chien dich", "campaign", "phan khuc", "gui voucher", "lau chua mua"],
    path: "/admin/voucher-campaigns",
    permission: "PROMOTION_VIEW",
    answer:
      "Chiến dịch voucher gửi voucher cho một nhóm khách.\n" +
      "• Lọc khách (kết hợp tất cả điều kiện): theo hạng, \"lâu chưa mua\" N ngày, \"từng mua danh mục\".\n" +
      "• Nhập mức giảm (%/số tiền, tối đa, đơn tối thiểu, số ngày hiệu lực); hệ thống tạo voucher 1 lần dùng, dùng được online và tại quầy.\n" +
      "• Gửi ngay hoặc hẹn giờ; danh sách khách được tính lại lúc gửi; chiến dịch chưa gửi có thể hủy.\n" +
      "• Mặc định bỏ qua khách đã nhận voucher chiến dịch trong 14 ngày gần nhất.",
  },
  {
    id: "birthday",
    title: "Voucher sinh nhật",
    keywords: ["voucher sinh nhat", "sinh nhat", "birthday"],
    path: "/admin/promotions",
    permission: "PROMOTION_VIEW",
    answer:
      "Voucher sinh nhật: tạo một khuyến mãi loại BIRTHDAY (mức giảm, đơn tối thiểu, giảm tối đa).\n" +
      "• Ngày 1 của tháng sinh, hệ thống tự phát cho mỗi khách một voucher riêng dùng trong cả tháng đó.\n" +
      "• Mỗi khách một lần mỗi năm; dùng được online và tại quầy.",
  },
  {
    id: "products",
    title: "Sản phẩm & biến thể",
    keywords: ["san pham", "them san pham", "bien the", "mau sac", "gia ban", "hinh anh san pham", "sku"],
    path: "/admin/products",
    permission: "PRODUCT_VIEW",
    answer:
      "Sản phẩm → Quản lý sản phẩm để thêm/sửa sản phẩm, hình ảnh và biến thể (màu, size, giá).\n" +
      "• Brand, bộ sưu tập, danh mục và tag nằm ở các mục riêng trong nhóm Sản phẩm.\n" +
      "• Khách đã đánh dấu yêu thích sẽ được báo khi giá giảm từ 5%, khi có flash sale hoặc khi sắp hết hàng.",
  },
  {
    id: "catalog",
    title: "Brand, danh mục, bộ sưu tập, tag",
    keywords: ["brand", "thuong hieu", "danh muc", "bo suu tap", "collection", "tag"],
    path: "/admin/categories",
    permission: "CATEGORY_VIEW",
    answer:
      "Nhóm Sản phẩm có các trang:\n" +
      "• Quản lý brand: /admin/brands\n" +
      "• Quản lý bộ sưu tập: /admin/collections\n" +
      "• Quản lý danh mục: /admin/categories (kèm bảng size theo danh mục)\n" +
      "• Quản lý tag: /admin/product-tags",
  },
  {
    id: "size-chart",
    title: "Bảng size",
    keywords: ["bang size", "size", "size chart", "tu van size", "chon size", "chieu cao", "can nang"],
    path: "/admin/categories",
    permission: "CATEGORY_VIEW",
    answer:
      "Bảng size được nhập theo danh mục: vào Quản lý danh mục → bấm nút \"Bảng size\" ở dòng danh mục.\n" +
      "• Mỗi size có khoảng chiều cao/cân nặng.\n" +
      "• Trên web, khách nhập số đo vào hồ sơ để được gợi ý size; size khách từng mua và giữ lại trong cùng danh mục được ưu tiên.",
  },
  {
    id: "inventory",
    title: "Tồn kho",
    keywords: ["ton kho", "kho", "het hang", "sap het", "so luong ton", "inventory", "hang hong"],
    path: "/admin/inventory",
    permission: "INVENTORY_VIEW",
    answer:
      "Sản phẩm → Quản lý kho để xem tồn theo cửa hàng.\n" +
      "• Tồn khả dụng dùng cho bán tại quầy; tồn online dùng cho đơn trên web.\n" +
      "• Hàng sắp hết hiển thị trên trang Tổng quan; có thể tạo phiếu điều chuyển từ cửa hàng khác.\n" +
      "• Muốn tăng/giảm tồn thì dùng Phiếu nhập / Phiếu xuất.",
  },
  {
    id: "receipts",
    title: "Phiếu nhập / phiếu xuất",
    keywords: ["phieu nhap", "nhap hang", "phieu xuat", "xuat hang", "xuat kho", "nhap kho", "gia von"],
    path: "/admin/imports",
    permission: "IMPORT_RECEIPT_VIEW",
    answer:
      "Kho vận → Phiếu nhập (/admin/imports) và Phiếu xuất (/admin/exports).\n" +
      "• Phiếu nhập hoàn thành sẽ cộng tồn và là cơ sở tính giá vốn bình quân trong báo cáo lợi nhuận.\n" +
      "• Phiếu xuất dùng để xuất hàng khỏi kho cửa hàng.",
  },
  {
    id: "transfers",
    title: "Điều chuyển hàng",
    keywords: ["dieu chuyen", "chuyen hang", "chuyen kho", "transfer"],
    path: "/admin/transfers",
    permission: "TRANSFER_VIEW",
    answer:
      "Kho vận → Điều chuyển hàng giữa các cửa hàng:\n" +
      "1. Cửa hàng thiếu hàng tạo yêu cầu điều chuyển.\n" +
      "2. Cửa hàng gửi duyệt và xuất hàng (hàng rời tồn quầy, chuyển sang đang vận chuyển).\n" +
      "3. Cửa hàng nhận xác nhận đã nhận, tồn được cộng.\n" +
      "Hệ thống gợi ý các mặt hàng sắp hết dựa trên ngưỡng tồn của trang Tổng quan.",
  },
  {
    id: "suppliers",
    title: "Nhà cung cấp",
    keywords: ["nha cung cap", "supplier", "ncc"],
    path: "/admin/suppliers",
    permission: "SUPPLIER_VIEW",
    answer: "Kho vận → Nhà cung cấp để quản lý danh sách nhà cung cấp dùng khi lập phiếu nhập.",
  },
  {
    id: "reviews",
    title: "Đánh giá sản phẩm",
    keywords: ["danh gia", "review", "binh luan", "phan hoi danh gia", "an danh gia"],
    path: "/admin/reviews",
    permission: "REVIEW_VIEW",
    answer:
      "Sản phẩm → Đánh giá sản phẩm.\n" +
      "• Chỉ khách đã mua (đơn hoàn thành) mới được đánh giá, mỗi sản phẩm một lần cho mỗi đơn, có nhãn \"Đã mua hàng\".\n" +
      "• Đánh giá hiển thị ngay; admin có thể ẩn/hiện và trả lời.\n" +
      "• Đánh giá không được cộng điểm thưởng.",
  },
  {
    id: "employees",
    title: "Nhân viên, phòng ban, vị trí",
    keywords: ["nhan vien", "them nhan vien", "tai khoan nhan vien", "phong ban", "vi tri", "chuc vu", "nhan su"],
    path: "/admin/employees",
    permission: "USER_VIEW",
    answer:
      "Nhóm Nhân sự gồm:\n" +
      "• Nhân viên: /admin/employees\n" +
      "• Phòng ban: /admin/departments\n" +
      "• Vị trí: /admin/positions\n" +
      "• Cửa hàng: /admin/stores (chỉ quản trị viên)\n" +
      "Quyền của nhân viên được cấp qua vai trò ở trang Phân quyền.",
  },
  {
    id: "stores",
    title: "Cửa hàng",
    keywords: ["cua hang", "chi nhanh", "store"],
    path: "/admin/stores",
    permission: "STORE_VIEW",
    answer:
      "Nhân sự → Cửa hàng để quản lý danh sách chi nhánh (chỉ quản trị viên).\n" +
      "Nhân viên cửa hàng chỉ thấy dữ liệu của cửa hàng mình; nhân viên cấp chuỗi xem được toàn chuỗi.",
  },
  {
    id: "roles",
    title: "Phân quyền",
    keywords: ["phan quyen", "vai tro", "quyen", "role", "permission", "khong co quyen", "khong vao duoc"],
    path: "/admin/roles",
    permission: "ROLE_VIEW",
    answer:
      "Hệ thống → Phân quyền để tạo vai trò và gán quyền.\n" +
      "• Menu bên trái chỉ hiện các trang mà tài khoản có quyền.\n" +
      "• Không vào được một trang nghĩa là vai trò của bạn thiếu quyền xem trang đó, hãy nhờ quản trị viên cấp thêm.",
  },
  {
    id: "settings",
    title: "Cài đặt hệ thống",
    keywords: ["cai dat", "setting", "cau hinh"],
    path: "/admin/settings",
    permission: "SETTINGS_MANAGE",
    answer: "Hệ thống → Quản lý setting dùng để cấu hình hệ thống (chỉ quản trị viên có quyền SETTINGS_MANAGE).",
  },
  {
    id: "logs",
    title: "Log hệ thống",
    keywords: ["log", "nhat ky", "lich su thao tac", "audit", "ai da sua"],
    path: "/admin/logs",
    permission: "LOG_VIEW",
    answer: "Hệ thống → Log hệ thống ghi lại thao tác của nhân viên và hệ thống (ai làm gì, lúc nào), dùng để tra cứu khi cần.",
  },
  {
    id: "profile",
    title: "Tài khoản của tôi",
    keywords: ["doi mat khau", "mat khau", "ho so", "tai khoan cua toi", "profile", "dang xuat", "quen mat khau"],
    path: "/admin/profile",
    answer:
      "Bấm vào tên tài khoản ở góc trên bên phải → Hồ sơ để cập nhật thông tin và đổi mật khẩu.\n" +
      "Quên mật khẩu thì dùng \"Quên mật khẩu\" ở trang đăng nhập quản trị.",
  },
  {
    id: "notifications",
    title: "Thông báo",
    keywords: ["thong bao", "chuong", "notification", "nhac gio hang", "gio hang bo quen"],
    answer:
      "Biểu tượng chuông trên thanh đầu trang hiển thị thông báo theo thời gian thực (đơn mới, cập nhật đơn...).\n" +
      "Thông báo tự động cho khách: nhắc giỏ hàng sau 24 giờ không thay đổi (một lần cho tới khi giỏ thay đổi), báo sản phẩm yêu thích giảm giá/flash sale/sắp hết, nhắc điểm sắp hết hạn.",
  },
];

const topicById = new Map(topics.map((topic) => [topic.id, topic]));

export const DEFAULT_SUGGESTIONS = ["pos", "returns", "tiers", "promotions", "transfers", "statistics"];

const GREETINGS = ["xin chao", "chao", "hello", "hi", "alo", "hey"];
const THANKS = ["cam on", "thank", "thanks", "ok", "oke"];
const HELP = ["giup", "huong dan", "lam duoc gi", "ban la ai", "help", "menu"];

export function normalize(value) {
  return String(value || "")
    .toLowerCase()
    .replace(/đ/g, "d")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/[^a-z0-9 ]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

// Khớp nguyên cụm từ để "kho" không khớp nhầm trong "khong".
function containsPhrase(text, phrase) {
  return ` ${text} `.includes(` ${phrase} `);
}

/** Chủ đề khớp nhiều nhất; cụm từ dài được tính điểm cao hơn từ đơn lẻ. */
export function findTopic(question) {
  const text = normalize(question);
  if (!text) return null;
  let best = null;
  let bestScore = 0;
  for (const topic of topics) {
    const score = topic.keywords
      .filter((keyword) => containsPhrase(text, keyword))
      .reduce((sum, keyword) => sum + keyword.split(" ").length ** 2, 0);
    if (score > bestScore) {
      best = topic;
      bestScore = score;
    }
  }
  return best;
}

function suggestionsFor(ids) {
  return ids.map((id) => topicById.get(id)).filter(Boolean).map(({ id, title }) => ({ id, title }));
}

function isOnly(text, words) {
  return words.some((word) => text === word || text.startsWith(`${word} `)) && text.split(" ").length <= 4;
}

export function welcomeMessage() {
  return {
    text:
      `Xin chào! Mình là ${ASSISTANT_NAME}, trợ lý hướng dẫn sử dụng hệ thống quản trị.\n` +
      "Mình trả lời các câu hỏi về chức năng và quy tắc nghiệp vụ của hệ thống (đơn hàng, kho, khuyến mãi, khách hàng...). Bạn cần hỗ trợ gì?",
    suggestions: suggestionsFor(DEFAULT_SUGGESTIONS),
  };
}

/**
 * Trả lời một câu hỏi tự gõ hoặc một gợi ý được bấm ({@code topicId}).
 * @param {(code: string) => boolean} [hasPermission] ẩn link tới trang mà tài khoản không có quyền
 */
export function answerQuestion(question, { topicId, hasPermission } = {}) {
  const text = normalize(question);
  const topic = topicId ? topicById.get(topicId) : findTopic(question);

  if (!topic) {
    if (isOnly(text, GREETINGS)) return welcomeMessage();
    if (isOnly(text, THANKS))
      return { text: "Rất vui được hỗ trợ bạn! Cần gì thêm cứ hỏi mình nhé.", suggestions: [] };
    if (HELP.some((word) => containsPhrase(text, word)))
      return {
        text: "Mình có thể hướng dẫn các chủ đề sau, bạn chọn một mục hoặc gõ câu hỏi nhé:",
        suggestions: suggestionsFor(topics.map((item) => item.id)),
      };
    return {
      text:
        "Xin lỗi, mình chỉ trả lời các câu hỏi liên quan đến hệ thống quản trị Lunaria (đơn hàng, kho, sản phẩm, khuyến mãi, khách hàng, nhân sự, báo cáo...). " +
        "Bạn thử hỏi lại theo một trong các chủ đề bên dưới nhé.",
      suggestions: suggestionsFor(DEFAULT_SUGGESTIONS),
      outOfScope: true,
    };
  }

  const allowed = !topic.permission || !hasPermission || hasPermission(topic.permission);
  return {
    text: allowed
      ? topic.answer
      : `${topic.answer}\n\nLưu ý: tài khoản của bạn hiện chưa có quyền mở trang này, hãy liên hệ quản trị viên nếu cần.`,
    topicId: topic.id,
    link: allowed && topic.path ? { path: topic.path, label: `Mở ${topic.title}` } : null,
    suggestions: [],
  };
}
