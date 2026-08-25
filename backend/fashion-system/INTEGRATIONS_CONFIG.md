# Cấu hình tích hợp bên thứ ba

Các biến mẫu nằm trong `.env.example`. Khi chạy local, sao chép thành `.env` và chỉ điền thông tin do nhà cung cấp cấp. Không commit `.env`, API key, secret key hoặc signing key lên Git.

## Thanh toán

Chọn nhà cung cấp bằng `PAYMENT_PROVIDER` (ví dụ `VNPAY`, `MOMO`, `STRIPE`) và chỉ đặt `PAYMENT_ENABLED=true` sau khi service của nhà cung cấp đó đã được triển khai.

- `PAYMENT_API_BASE_URL`: địa chỉ API theo đúng môi trường sandbox/production.
- `PAYMENT_MERCHANT_ID`, `PAYMENT_CLIENT_ID`, `PAYMENT_API_KEY`: mã định danh/xác thực tùy provider.
- `PAYMENT_SECRET_KEY`, `PAYMENT_SIGNING_KEY`: bí mật dùng ký hoặc kiểm tra request.
- `PAYMENT_WEBHOOK_SECRET`: bí mật kiểm tra webhook nếu provider hỗ trợ.
- `PAYMENT_RETURN_URL`, `PAYMENT_CANCEL_URL`: nơi trình duyệt khách quay lại.
- `PAYMENT_WEBHOOK_URL`: endpoint backend nhận kết quả thanh toán.

Không lấy redirect/return URL làm bằng chứng thanh toán thành công. Backend phải xác minh chữ ký webhook hoặc chủ động đối soát với API provider. Mỗi sự kiện cần được xử lý idempotent để webhook gửi lại không tạo giao dịch hai lần.

## Giao hàng

Chọn nhà vận chuyển bằng `SHIPPING_PROVIDER` (ví dụ `GHN`, `GHTK`, `VIETTEL_POST`). Các provider không dùng cùng tên credential, vì vậy chỉ điền những biến adapter tương ứng cần.

- `SHIPPING_API_BASE_URL`: URL sandbox/production của nhà vận chuyển.
- `SHIPPING_API_TOKEN`, `SHIPPING_CLIENT_ID`, `SHIPPING_CLIENT_SECRET`: thông tin xác thực.
- `SHIPPING_SHOP_ID`: mã shop/kho do provider cấp.
- `SHIPPING_WEBHOOK_SECRET`, `SHIPPING_WEBHOOK_URL`: xác thực và nhận cập nhật trạng thái vận đơn.
- Nhóm `SHIPPING_SENDER_*`: địa chỉ kho gửi mặc định; khi có nhiều kho nên chuyển dữ liệu này vào database.

Webhook giao vận cũng phải được xác thực trước khi đổi trạng thái đơn hàng. Không ghi token hoặc thông tin bí mật vào log.

## Chatbot AI

`AI_PROVIDER` nhận `OPENAI` hoặc `ANTHROPIC`. Chỉ đặt `AI_CHATBOT_ENABLED=true` sau khi đã có API key, model và service gọi provider.

Với OpenAI:

- `OPENAI_API_KEY`: API key phía server; không gửi xuống frontend.
- `OPENAI_BASE_URL`: mặc định `https://api.openai.com/v1`.
- `OPENAI_MODEL`: model mà project OpenAI đang được quyền dùng.
- `OPENAI_ORGANIZATION_ID`, `OPENAI_PROJECT_ID`: để trống nếu tài khoản không yêu cầu.

Với Anthropic:

- `ANTHROPIC_API_KEY`: API key phía server.
- `ANTHROPIC_BASE_URL`: mặc định `https://api.anthropic.com`.
- `ANTHROPIC_MODEL`: model mà tài khoản đang được quyền dùng.
- `ANTHROPIC_VERSION`: phiên bản API adapter yêu cầu; điền theo tài liệu provider tại thời điểm triển khai.

`AI_MAX_OUTPUT_TOKENS`, `AI_TEMPERATURE` và timeout là cấu hình dùng chung. Cần bổ sung giới hạn độ dài input, quota theo người dùng, lọc dữ liệu nhạy cảm và theo dõi chi phí trước khi mở chatbot công khai.

Lưu ý: tài khoản/gói ChatGPT không thay thế cho OpenAI API key. Backend tích hợp qua OpenAI API và việc sử dụng API được quản lý riêng.

## Trạng thái triển khai

Các thay đổi hiện tại mới chuẩn hóa biến môi trường và ánh xạ chúng vào `application.yaml`; chưa có adapter gọi cổng thanh toán, đơn vị giao hàng hoặc model AI. Vì vậy cả ba nhóm đều mặc định tắt để tránh tạo cảm giác hệ thống đã sẵn sàng tích hợp.
