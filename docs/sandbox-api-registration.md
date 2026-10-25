# Hướng dẫn lấy khóa sandbox: VNPay, MoMo, Lalamove

Tài liệu cho mục đích **học tập / chạy local**, không deploy. Cả ba đều có môi trường test (sandbox): không mất tiền thật, không giao hàng thật.

> Giao diện các trang đăng ký có thể đổi theo thời gian. Nếu tên nút/menu khác với mô tả ở đây thì tìm mục tương tự ("Developer", "API keys", "Sandbox"...).

Tất cả khóa điền vào file `backend/fashion-system/.env` (copy từ `.env.example`). **Không commit file `.env`.**

---

## 0. Chuẩn bị chung: URL công khai cho callback (IPN)

Khi khách thanh toán xong, cổng thanh toán gọi **IPN** (server-to-server) về backend để báo kết quả. Trong dự án này, **chỉ IPN mới chuyển đơn sang "đã thanh toán"**; trang return (trình duyệt quay về) chỉ hiển thị kết quả.

VNPay/MoMo nằm trên Internet nên không gọi được `http://localhost:8080`. Có 2 cách:

**Cách A — mở tunnel (khuyên dùng):** dùng Cloudflare Tunnel, không cần tài khoản:

```powershell
winget install --id Cloudflare.cloudflared
cloudflared tunnel --url http://localhost:8080
```

Lệnh in ra một URL dạng `https://abc-xyz.trycloudflare.com`. Điền vào `.env`:

```env
PAYMENT_CALLBACK_BASE_URL=https://abc-xyz.trycloudflare.com
PAYMENT_RESULT_URL=http://localhost:5173/payment/result
```

URL này **đổi mỗi lần chạy lại** cloudflared → nhớ cập nhật `.env` và restart backend. (ngrok cũng được, nhưng cần đăng ký tài khoản.)

**Cách B — không tunnel:** để `PAYMENT_CALLBACK_BASE_URL=http://localhost:8080`, thanh toán xong thì tự "giả lập" IPN (xem mục 1.4). Đủ để học luồng, nhưng không giống thật bằng cách A.

---

## 1. VNPay sandbox

### 1.1. Đăng ký

1. Vào <https://sandbox.vnpayment.vn/devreg/>.
2. Điền form: tên, email, **URL website** (điền tạm `http://localhost:5173` hoặc URL tunnel đều được), mật khẩu.
3. VNPay gửi email chứa:
   - **vnp_TmnCode** (mã website, ~8 ký tự)
   - **vnp_HashSecret** (chuỗi bí mật để ký)
   - Tài khoản đăng nhập trang quản trị merchant sandbox: <https://sandbox.vnpayment.vn/merchantv2/>

Email có thể vào mục Spam/Quảng cáo — kiểm tra kỹ. Không cần số điện thoại xác thực OTP.

### 1.2. Cấu hình `.env`

```env
VNPAY_TMN_CODE=<vnp_TmnCode trong email>
VNPAY_HASH_SECRET=<vnp_HashSecret trong email>
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
```

### 1.3. Khai báo IPN URL

IPN URL của dự án: `{PAYMENT_CALLBACK_BASE_URL}/api/webhooks/payments/vnpay/ipn`

Với sandbox, IPN URL được cấu hình phía VNPay (không gửi kèm mỗi request). Thử tìm mục cấu hình IPN trong trang merchant sandbox; nếu không có, gửi email cho bộ phận hỗ trợ tích hợp (địa chỉ có trong email đăng ký) kèm TmnCode và IPN URL. Vì URL tunnel đổi liên tục nên với VNPay **cách B (mục 1.4) thường tiện hơn**.

### 1.4. Thẻ test và giả lập IPN

Ở trang thanh toán VNPay chọn **Thẻ nội địa → ngân hàng NCB**:

| Trường | Giá trị |
|---|---|
| Số thẻ | `9704198526191432198` |
| Tên chủ thẻ | `NGUYEN VAN A` |
| Ngày phát hành | `07/15` |
| OTP | `123456` |

(Danh sách thẻ test đầy đủ ở trang tài liệu sandbox của VNPay: <https://sandbox.vnpayment.vn/apis/>.)

**Giả lập IPN khi không có tunnel:** VNPay gửi cho return URL và IPN **cùng một bộ tham số và cùng chữ ký**. Vì vậy:

1. Thanh toán xong, trình duyệt bị chuyển về `http://localhost:8080/api/webhooks/payments/vnpay/return?vnp_Amount=...&vnp_SecureHash=...`
2. Copy toàn bộ phần sau dấu `?`.
3. Gọi IPN bằng chính chuỗi đó:

```powershell
curl.exe "http://localhost:8080/api/webhooks/payments/vnpay/ipn?<dán chuỗi query vào đây>"
```

Kết quả `{"RspCode":"00","Message":"Confirm Success"}` nghĩa là đơn đã được xác nhận thanh toán.

---

## 2. MoMo test environment

### 2.1. Dùng khóa test công khai (nhanh nhất, không cần đăng ký)

MoMo công bố sẵn một bộ khóa test trong tài liệu và code mẫu (<https://developers.momo.vn>, repo GitHub `momo-wallet/payment`):

```env
MOMO_PARTNER_CODE=MOMO
MOMO_ACCESS_KEY=F8BBA842ECF85
MOMO_SECRET_KEY=K951B6PE1waDMi640xX08PD3vg6EkVlz
MOMO_API_BASE_URL=https://test-payment.momo.vn
```

Đây là khóa dùng chung cho mọi người học/test, chỉ chạy được trên môi trường test. Nếu MoMo đổi bộ khóa này, lấy bộ mới trong trang tài liệu (mục "Test environment" / "Thông tin tích hợp").

### 2.2. Muốn có khóa riêng (tùy chọn)

Đăng ký tài khoản doanh nghiệp tại <https://business.momo.vn> → sau khi tạo được hồ sơ, vào mục thông tin tích hợp để lấy Partner Code / Access Key / Secret Key của môi trường test. Bước này có thể yêu cầu số điện thoại và thông tin doanh nghiệp → với mục đích học tập, **dùng khóa công khai ở 2.1 là đủ**.

### 2.3. IPN

Khác VNPay, dự án gửi `ipnUrl` trong **mỗi request** tạo thanh toán, nên chỉ cần `PAYMENT_CALLBACK_BASE_URL` trỏ tới URL tunnel (cách A) là MoMo gọi về được:
`{PAYMENT_CALLBACK_BASE_URL}/api/webhooks/payments/momo/ipn`

Không dùng tunnel thì đơn sẽ đứng ở trạng thái chờ thanh toán đến khi hết `PAYMENT_HOLD_MINUTES` rồi tự hủy.

### 2.4. Thanh toán thử

Dự án dùng `requestType=captureWallet` (thanh toán bằng ví). Trang thanh toán MoMo test sẽ hiện mã QR:

1. Tải **ứng dụng MoMo bản test (UAT)** theo link trong trang tài liệu developers.momo.vn (mục "Test App" / "Môi trường test"). App MoMo thật trên store **không** quét được QR test.
2. Đăng ký tài khoản trong app test theo hướng dẫn của MoMo (ví test có sẵn số dư ảo).
3. Quét QR trên trang thanh toán và xác nhận.

---

## 3. Lalamove sandbox

### 3.1. Đăng ký bình thường

1. Vào Partner Portal: <https://partnerportal.lalamove.com>.
2. Đăng ký bằng email + số điện thoại, chọn thị trường **Vietnam**.
3. Đăng nhập → mục **Developers / API Keys** → tab **Sandbox** → tạo key. Nhận được:
   - API key dạng `pk_test_...`
   - API secret dạng `sk_test_...` (chỉ hiện một lần, lưu lại ngay)

```env
SHIPPING_ENABLED=true
SHIPPING_PROVIDER=LALAMOVE
SHIPPING_API_BASE_URL=https://rest.sandbox.lalamove.com
SHIPPING_CLIENT_ID=pk_test_...
SHIPPING_CLIENT_SECRET=sk_test_...
SHIPPING_WEBHOOK_URL=https://abc-xyz.trycloudflare.com/api/webhooks/shipping/lalamove
LALAMOVE_MARKET=VN
LALAMOVE_LANGUAGE=vi_VN
LALAMOVE_SERVICE_TYPE=MOTORCYCLE

# Địa chỉ kho gửi hàng (phải là địa chỉ thật ở thành phố Lalamove có phục vụ, ví dụ TP.HCM / Hà Nội)
SHIPPING_SENDER_NAME=Fashion System
SHIPPING_SENDER_PHONE=+84xxxxxxxxx
SHIPPING_SENDER_ADDRESS=...
```

Webhook URL khai báo trong Partner Portal (mục Webhook) hoặc qua API `PATCH /v3/webhook`. Khi dùng tunnel thì URL thay đổi mỗi lần, phải cập nhật lại cả hai nơi.

### 3.2. Lỗi "số điện thoại đã được sử dụng"

Nguyên nhân thường gặp: **số sim được nhà mạng thu hồi rồi bán lại**, chủ cũ từng đăng ký Lalamove bằng số đó. Hướng xử lý, theo thứ tự nên thử:

1. **Liên hệ hỗ trợ Lalamove** (mục Help/Contact trên lalamove.com hoặc trong Partner Portal). Nói rõ: số mới mua, bị báo đã dùng, đề nghị gỡ liên kết số khỏi tài khoản cũ. Họ có thể yêu cầu chứng minh bạn là chủ sim hiện tại (ảnh thông tin thuê bao chính chủ). Đây là cách đúng và lâu dài.
2. **Dùng số điện thoại khác của bạn hoặc người thân** (có sự đồng ý của họ) để đăng ký, kèm email của bạn.
3. **Không** dùng "Quên mật khẩu" để chiếm tài khoản cũ đang gắn với số đó — đó là tài khoản (và dữ liệu) của người khác.
4. Tránh dùng dịch vụ "số ảo nhận OTP": dễ bị khóa tài khoản và vi phạm điều khoản.

### 3.3. Chưa có tài khoản Lalamove thì vẫn học được

Lalamove là tính năng **tùy chọn** trong dự án:

- Để `SHIPPING_ENABLED=false` (mặc định): toàn bộ phần đơn hàng, thanh toán vẫn chạy; chỉ thao tác tạo chuyến giao hàng sẽ báo "Chưa bật tích hợp vận chuyển".
- Logic Lalamove (ký HMAC, tạo quotation, tạo order, map trạng thái webhook) đã có unit test dùng response giả lập: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/shipping/LalamoveShippingProviderTest.java`. Đọc/chạy test này để hiểu luồng mà không cần API key:

```powershell
cd backend/fashion-system
.\mvnw.cmd test -Dtest=LalamoveShippingProviderTest
```

- Tài liệu API v3 đọc công khai không cần tài khoản: <https://developers.lalamove.com>.

---

## 4. Checklist kiểm tra nhanh

| Bước | Kiểm tra |
|---|---|
| Backend khởi động lại sau khi sửa `.env` | ✔ |
| Trang checkout hiện lựa chọn VNPay / MoMo | Cổng nào thiếu khóa sẽ bị ẩn |
| Đặt đơn → được chuyển sang trang thanh toán sandbox | Nếu lỗi chữ ký: kiểm tra lại TmnCode/HashSecret, không thừa khoảng trắng |
| Thanh toán xong → quay về `/payment/result` | Chỉ là trang hiển thị |
| Đơn chuyển sang "đã thanh toán" | Cần IPN tới được backend (tunnel) hoặc giả lập IPN (VNPay, mục 1.4) |
