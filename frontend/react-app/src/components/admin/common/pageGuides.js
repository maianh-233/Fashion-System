/** Nội dung "Hướng dẫn nhanh" theo trang (khóa = id truyền cho PageGuide). */
export const PAGE_GUIDES = {
  refunds: {
    title: "Xử lý hoàn tiền",
    steps: [
      { title: "Xem khoản chờ hoàn", text: "Gồm tiền về sau khi đơn đã hủy, khách trả trùng và đơn hủy sau khi giao thất bại." },
      { title: "Chuyển tiền cho khách", text: "Dùng mã giao dịch của cổng để hoàn trên trang quản trị VNPay / MoMo." },
      { title: "Xác nhận", text: "Bấm “Đã hoàn tiền” và nhập mã giao dịch. Khách nhận thông báo, trạng thái đơn tự cập nhật." },
    ],
    tips: ["Khoản hoàn của yêu cầu trả hàng được xác nhận ở màn Đổi trả.", "Hoàn khoản trả trùng không làm đổi trạng thái đơn và không trừ điểm của khách."],
  },
  myTasks: {
    title: "Công việc của bạn",
    steps: [
      { title: "Xem việc được giao", text: "Việc xếp theo hạn chót: Quá hạn, Hôm nay, Sắp tới. Bấm tên việc để xem chi tiết và bình luận." },
      { title: "Làm từng bước", text: "Nếu việc có danh sách việc con, tick từng bước trong trang chi tiết. Tick đầu tiên tự chuyển việc sang Đang làm." },
      { title: "Xác nhận hoàn thành", text: "Bấm vòng tròn đầu dòng hoặc nút “Xác nhận hoàn thành”. Người giao việc sẽ nhận được thông báo ngay." },
    ],
    tips: ["Tick nhầm thì nhờ người giao bấm “Trả lại”.", "Nếu bạn có thể giao việc, tab “Tôi đã giao” cho biết ai đã xong, ai chưa."],
  },
  tasks: {
    title: "Quản lý công việc",
    steps: [
      { title: "Giao việc", text: "Bấm “Giao việc”, chọn một hoặc nhiều người. Mỗi người nhận một việc riêng và được báo ngay." },
      { title: "Theo dõi", text: "Lọc theo trạng thái, người nhận hoặc “Quá hạn” để biết việc nào cần đôn đốc." },
      { title: "Kiểm tra kết quả", text: "Khi nhân viên hoàn thành bạn sẽ nhận thông báo. Chưa đạt thì mở việc và bấm “Trả lại” kèm lý do." },
    ],
    tips: ["Tab Thống kê cho biết số việc và tỷ lệ đúng hạn của từng nhân viên.", "Hệ thống tự nhắc trước hạn 24 giờ và báo khi quá hạn."],
  },
  marketingCampaigns: {
    title: "Chiến dịch marketing",
    steps: [
      { title: "Tạo chiến dịch", text: "Đặt tên, mục tiêu, thời gian, người phụ trách, ngân sách và chỉ tiêu. Chiến dịch mới ở trạng thái Nháp." },
      { title: "Liên kết ưu đãi", text: "Trong trang chi tiết, bấm “Liên kết ưu đãi” để gắn khuyến mãi, chiến dịch voucher hoặc flash sale đã tạo sẵn." },
      { title: "Gửi duyệt", text: "Người có quyền duyệt nhận thông báo, rồi duyệt hoặc từ chối kèm lý do. Bị từ chối thì sửa và gửi lại." },
      { title: "Theo dõi chi phí", text: "Nhập các khoản chi thực tế (quảng cáo, KOL, in ấn, sự kiện). Hệ thống cảnh báo khi vượt ngân sách." },
    ],
    tips: [
      "Mỗi ưu đãi chỉ thuộc một chiến dịch. Muốn chuyển sang chiến dịch khác thì gỡ liên kết ở chiến dịch cũ trước.",
      "Hủy chiến dịch không tắt khuyến mãi / voucher / flash sale; hãy tắt chúng ở màn hình riêng nếu cần.",
      "Không tự duyệt được chiến dịch do mình tạo hoặc gửi duyệt.",
      "Đợt gửi thông báo: chọn nhóm khách, nội dung và giờ gửi. Danh sách người nhận được chốt đúng lúc gửi; đợt thêm sau khi chiến dịch đã duyệt cần duyệt riêng.",
    ],
  },
  shiftSchedule: {
    title: "Xếp ca cho cửa hàng",
    steps: [
      { title: "Chọn cửa hàng và tuần", text: "Dùng mũi tên ‹ › để đổi tuần. Cột vàng là hôm nay, cột xám là ngày nghỉ tuần / lễ." },
      { title: "Xếp ca", text: "Kéo một ca mẫu (Ca sáng, Ca chiều…) thả vào ô của nhân viên, hoặc rê chuột vào ô trống rồi bấm “Xếp ca”." },
      { title: "Chỉnh sửa", text: "Kéo thẻ ca sang ô khác để đổi ngày / đổi người. Bấm vào thẻ để sửa giờ, ghi chú hoặc xóa." },
      { title: "Công bố", text: "Ca mới là nháp (viền nét đứt) — nhân viên chưa thấy. Bấm “Công bố” để gửi lịch và thông báo cho nhân viên." },
    ],
    tips: [
      "“Sao chép tuần trước” tạo nhanh lịch tuần này từ tuần trước (bỏ qua người đang nghỉ phép hoặc đã có ca).",
      "Nhãn Rảnh / Bận do nhân viên tự đăng ký; số giờ đỏ bên tên nghĩa là vượt 48 giờ / tuần.",
      "Chuyển sang “Theo giờ” để thấy khung giờ nào đông hoặc thiếu người.",
    ],
  },
  shiftSwaps: {
    title: "Duyệt đổi ca",
    steps: [
      { title: "Nhân viên gửi yêu cầu", text: "Từ trang Lịch làm của tôi: đổi ca với đồng nghiệp hoặc nhờ đồng nghiệp làm thay." },
      { title: "Đồng nghiệp đồng ý", text: "Yêu cầu ở trạng thái “Chờ đồng nghiệp” cho tới khi người được nhờ đồng ý." },
      { title: "Quản lý duyệt", text: "Yêu cầu “Chờ quản lý duyệt”: bấm Duyệt để lịch tự cập nhật, hoặc Từ chối kèm lý do." },
    ],
    tips: ["Hệ thống tự chặn đổi ca nếu gây trùng ca, rơi vào ngày nghỉ phép hoặc ca đã chấm công."],
  },
  myShifts: {
    title: "Lịch làm của bạn",
    steps: [
      { title: "Xem ca của tuần", text: "Lịch chỉ hiện ca quản lý đã công bố. Dùng mũi tên để xem tuần trước / sau." },
      { title: "Đăng ký rảnh / bận", text: "Chọn Rảnh hoặc Bận cho 14 ngày tới để quản lý biết khi xếp ca. Bấm lại để bỏ chọn." },
      { title: "Đổi ca hoặc nhờ làm thay", text: "Bấm vào một ca sắp tới, chọn đồng nghiệp. Đồng nghiệp đồng ý xong, quản lý sẽ duyệt." },
    ],
  },
  myAttendance: {
    title: "Chấm công hằng ngày",
    steps: [
      { title: "Check-in khi tới ca", text: "Bấm Check-in. Nhân viên cửa hàng cần cho phép định vị và đứng trong bán kính cửa hàng." },
      { title: "Check-out khi về", text: "Bấm Check-out khi hết ca. Giờ vào / ra lấy theo giờ máy chủ." },
      { title: "Gửi đơn khi cần", text: "Quên chấm → đơn giải trình; nghỉ → đơn nghỉ phép; làm thêm giờ → đơn tăng ca. Quản lý sẽ duyệt." },
    ],
  },
  stocktakes: {
    title: "Kiểm kê kho",
    steps: [
      { title: "Tạo phiếu", text: "Chọn toàn cửa hàng hoặc một danh mục. Số tồn hệ thống được chụp lại ngay lúc tạo." },
      { title: "Đếm hàng", text: "Mở phiếu, quét mã vạch (máy quét hoặc camera) hoặc gõ SKU. Có thể sửa số ở cột “Đếm được”." },
      { title: "Gửi duyệt", text: "Đếm xong bấm “Gửi duyệt”. Có thể chọn tính các mã chưa đếm là 0." },
      { title: "Quản lý duyệt", text: "Duyệt để sinh phiếu điều chỉnh tồn, hoặc trả về để đếm lại. Xem hao hụt ở tab Báo cáo." },
    ],
    tips: ["Bật “Đếm mù” để nhân viên không thấy số hệ thống khi đếm — kết quả khách quan hơn."],
  },
  stocktakeCount: {
    title: "Cách đếm trên phiếu",
    steps: [
      { title: "Quét liên tục", text: "Con trỏ luôn ở ô quét. Mỗi lần quét cộng thêm số lượng ở ô bên cạnh (mặc định 1), có tiếng bíp báo đúng / sai." },
      { title: "Đếm theo thùng", text: "Nhập số lượng (ví dụ 12) rồi quét một lần cho cả thùng." },
      { title: "Kiểm tra còn thiếu", text: "Lọc “Chưa đếm” để biết mã nào chưa đếm; lọc “Lệch” (nếu thấy được) để đếm lại." },
    ],
    tips: ["Hàng bán ra trong lúc đếm vẫn được tính đúng khi duyệt — không cần đóng cửa hàng."],
  },
  transfers: {
    title: "Điều chuyển hàng giữa cửa hàng",
    steps: [
      { title: "Tìm hàng sắp hết", text: "Tab “Hàng sắp hết” gợi ý cửa hàng đang dư hàng. Tạo yêu cầu xin hàng từ đó." },
      { title: "Nơi gửi duyệt & xuất", text: "Cửa hàng gửi duyệt và xuất (trừ tồn tại quầy) hoặc từ chối kèm lý do." },
      { title: "Nơi nhận xác nhận", text: "Khi hàng tới, cửa hàng nhận bấm xác nhận để cộng tồn." },
    ],
  },
  payroll: {
    title: "Quy trình tính lương tháng",
    steps: [
      { title: "Kiểm tra hồ sơ lương", text: "Tab Hồ sơ lương: mỗi nhân viên cần mức lương và phụ cấp đúng trước khi tính." },
      { title: "Tạo kỳ và tính lương", text: "Tab Kỳ lương: tạo kỳ cho tháng rồi bấm tính. Ngày công lấy từ chấm công đã duyệt." },
      { title: "Rà soát", text: "Xem các dòng “Cần xem xét”, điều chỉnh thưởng / khấu trừ và xác nhận." },
      { title: "Duyệt & thanh toán", text: "Gửi duyệt (dữ liệu công tháng bị khóa) → chốt → ghi nhận thanh toán và phát hành phiếu lương." },
    ],
  },
  recruitment: {
    title: "Quy trình tuyển dụng",
    steps: [
      { title: "Mở vị trí", text: "Tab Chiến dịch & vị trí: tạo chiến dịch và vị trí cần tuyển, chọn mẫu quy trình nhiều vòng." },
      { title: "Nhận hồ sơ", text: "Ứng viên nộp qua trang Tuyển dụng công khai, hoặc thêm tay ở tab Ứng viên." },
      { title: "Sàng lọc & phỏng vấn", text: "Tab Hồ sơ ứng tuyển: chuyển vòng, đánh giá, lên lịch phỏng vấn (xem ở Lịch phỏng vấn)." },
      { title: "Offer & tuyển", text: "Gửi offer, khi ứng viên nhận thì tạo hồ sơ nhân viên." },
    ],
  },
};
