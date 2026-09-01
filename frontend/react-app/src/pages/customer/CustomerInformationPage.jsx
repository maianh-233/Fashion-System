import { useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  ArrowRight,
  Check,
  Clock3,
  HeartHandshake,
  History,
  Mail,
  PackageCheck,
  Phone,
  ReceiptText,
  RefreshCcw,
  ShieldCheck,
  Sparkles,
  Store,
} from "lucide-react";

const milestones = [
  { year: "2018", title: "Khởi đầu từ một xưởng may nhỏ", text: "Lunaria ra đời tại TP. Hồ Chí Minh với mong muốn tạo nên những thiết kế thanh lịch, dễ mặc và được hoàn thiện chỉn chu." },
  { year: "2020", title: "Cửa hàng đầu tiên", text: "Không gian Lunaria đầu tiên mở cửa, đưa trải nghiệm tư vấn phong cách cá nhân đến gần hơn với khách hàng." },
  { year: "2023", title: "Mở rộng hệ thống", text: "Lunaria phát triển mạng lưới cửa hàng và chuẩn hóa dịch vụ nhận hàng, đổi trả và chỉnh sửa trang phục tại chi nhánh." },
  { year: "2026", title: "Một Lunaria liền mạch", text: "Trải nghiệm trực tuyến và tại cửa hàng được kết nối, giúp khách hàng mua sắm, nhận hàng và được hỗ trợ thuận tiện hơn." },
];

const returnConditions = [
  "Sản phẩm còn nguyên tem, nhãn và chưa qua sử dụng hoặc giặt ủi.",
  "Sản phẩm không có mùi lạ, vết bẩn, hư hỏng hay dấu hiệu đã chỉnh sửa.",
  "Có hóa đơn điện tử, mã đơn hàng hoặc thông tin mua hàng hợp lệ.",
  "Quà tặng và phụ kiện đi kèm phải được hoàn trả đầy đủ.",
];

const regulations = [
  { icon: ReceiptText, title: "Giá bán & thanh toán", text: "Giá niêm yết tại cửa hàng và website đã bao gồm thuế theo quy định. Lunaria chấp nhận các phương thức thanh toán được hiển thị tại thời điểm mua hàng." },
  { icon: PackageCheck, title: "Kiểm tra khi nhận hàng", text: "Khách hàng vui lòng kiểm tra mẫu mã, màu sắc, kích thước và tình trạng sản phẩm trước khi nhận. Nếu có sai lệch, hãy báo ngay cho nhân viên hỗ trợ." },
  { icon: ShieldCheck, title: "Bảo mật thông tin", text: "Thông tin khách hàng chỉ được sử dụng để xử lý đơn, chăm sóc khách hàng và cung cấp quyền lợi thành viên theo chính sách bảo mật của Lunaria." },
  { icon: HeartHandshake, title: "Ứng xử tại cửa hàng", text: "Vui lòng giữ gìn tài sản chung, tôn trọng nhân viên và khách hàng khác. Trẻ em cần có người lớn đi cùng trong suốt thời gian trải nghiệm." },
  { icon: Clock3, title: "Giữ hàng & nhận hàng", text: "Sản phẩm đặt giữ được bảo lưu tối đa 24 giờ nếu chưa thanh toán. Đơn nhận tại cửa hàng được lưu giữ tối đa 07 ngày kể từ khi có thông báo sẵn sàng." },
  { icon: Store, title: "Quyền từ chối phục vụ", text: "Lunaria có quyền từ chối giao dịch có dấu hiệu gian lận, lợi dụng chính sách hoặc gây ảnh hưởng đến an toàn và hoạt động của cửa hàng." },
];

export default function CustomerInformationPage() {
  const location = useLocation();

  useEffect(() => {
    if (!location.hash) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    const target = document.getElementById(location.hash.slice(1));
    window.setTimeout(() => target?.scrollIntoView({ behavior: "smooth", block: "start" }), 0);
  }, [location.hash]);

  return (
    <div className="customer-page customer-information-page">
      <section className="information-hero">
        <div className="information-hero__inner">
          <p><Sparkles size={14} /> About Lunaria</p>
          <h1>Mỗi thiết kế mang theo<br /><em>một câu chuyện.</em></h1>
          <span>Tìm hiểu hành trình của Lunaria và những chính sách giúp trải nghiệm mua sắm của bạn luôn rõ ràng, an tâm.</span>
        </div>
      </section>

      <nav className="information-nav" aria-label="Mục lục thông tin">
        <div>
          <a href="#history"><History size={15} /> Lịch sử thương hiệu</a>
          <a href="#returns"><RefreshCcw size={15} /> Chính sách đổi trả</a>
          <a href="#regulations"><ShieldCheck size={15} /> Quy định cửa hàng</a>
        </div>
      </nav>

      <main className="information-content">
        <section id="history" className="information-section information-history">
          <div className="information-section__intro">
            <div><p>Our story</p><h2>Hành trình tạo nên Lunaria</h2></div>
            <blockquote>“Thanh lịch không nằm ở việc chạy theo mọi xu hướng, mà ở cách mỗi người tự tin kể câu chuyện của riêng mình.”</blockquote>
          </div>

          <div className="information-history__layout">
            <div className="information-history__image">
              <img src="https://images.unsplash.com/photo-1558618666-fcd25c85cd64?q=85&w=1100&auto=format&fit=crop" alt="Không gian làm việc và thiết kế thời trang Lunaria" />
              <span><strong>2018</strong> Khởi nguồn tại Sài Gòn</span>
            </div>
            <div className="information-timeline">
              {milestones.map((milestone) => (
                <article key={milestone.year}>
                  <strong>{milestone.year}</strong>
                  <div><h3>{milestone.title}</h3><p>{milestone.text}</p></div>
                </article>
              ))}
            </div>
          </div>

          <div className="information-values">
            <article><strong>06</strong><span>Cửa hàng trong hệ thống</span></article>
            <article><strong>08+</strong><span>Năm theo đuổi sự chỉn chu</span></article>
            <article><strong>30 ngày</strong><span>Hỗ trợ lỗi sản xuất</span></article>
          </div>
        </section>

        <section id="returns" className="information-section information-returns">
          <div className="information-section__intro">
            <div><p>Return & exchange</p><h2>Chính sách đổi trả</h2></div>
            <span>Áp dụng cho sản phẩm mua tại website và cửa hàng Lunaria. Cập nhật ngày 01/09/2026.</span>
          </div>

          <div className="information-return-highlight">
            <div><RefreshCcw size={25} /><span><strong>07 ngày</strong><small>Đổi sản phẩm do nhu cầu</small></span></div>
            <div><ShieldCheck size={25} /><span><strong>30 ngày</strong><small>Hỗ trợ lỗi từ nhà sản xuất</small></span></div>
            <p>Sản phẩm đủ điều kiện có thể được đổi một lần sang sản phẩm cùng hoặc cao hơn giá trị. Phần chênh lệch sẽ do khách hàng thanh toán.</p>
          </div>

          <div className="information-return-grid">
            <article>
              <h3>Điều kiện áp dụng</h3>
              <ul>{returnConditions.map((condition) => <li key={condition}><Check size={14} /> {condition}</li>)}</ul>
            </article>
            <article>
              <h3>Sản phẩm không áp dụng</h3>
              <ul className="is-muted">
                <li>Đồ lót, trang sức, phụ kiện tóc và sản phẩm vệ sinh cá nhân.</li>
                <li>Sản phẩm thuộc chương trình đồng giá hoặc ghi rõ “không đổi trả”.</li>
                <li>Sản phẩm đã chỉnh sửa theo số đo hoặc yêu cầu riêng.</li>
                <li>Hư hỏng do sử dụng, bảo quản không đúng hướng dẫn.</li>
              </ul>
            </article>
          </div>

          <div className="information-return-steps">
            <h3>Quy trình đổi trả</h3>
            <ol>
              <li><span>01</span><div><strong>Gửi yêu cầu</strong><p>Liên hệ hotline hoặc mang sản phẩm đến cửa hàng cùng mã đơn.</p></div></li>
              <li><span>02</span><div><strong>Kiểm tra sản phẩm</strong><p>Nhân viên xác nhận tình trạng và điều kiện áp dụng.</p></div></li>
              <li><span>03</span><div><strong>Hoàn tất đổi trả</strong><p>Chọn sản phẩm thay thế hoặc nhận phương án xử lý phù hợp.</p></div></li>
            </ol>
          </div>
        </section>

        <section id="regulations" className="information-section information-regulations">
          <div className="information-section__intro">
            <div><p>Store regulations</p><h2>Quy định mua sắm</h2></div>
            <span>Các nguyên tắc chung nhằm bảo đảm quyền lợi và trải nghiệm tốt cho mọi khách hàng.</span>
          </div>

          <div className="information-regulation-grid">
            {regulations.map(({ icon: Icon, title, text }, index) => (
              <article key={title}>
                <span><Icon size={19} /></span>
                <small>0{index + 1}</small>
                <h3>{title}</h3>
                <p>{text}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="information-contact">
          <div><p>Cần hỗ trợ thêm?</p><h2>Chúng tôi luôn sẵn lòng lắng nghe.</h2><span>Đội ngũ Lunaria hỗ trợ từ 09:00 đến 21:00, tất cả các ngày trong tuần.</span></div>
          <div className="information-contact__actions">
            <a href="tel:1800123456"><Phone size={16} /> 1800 123 456</a>
            <a href="mailto:hello@lunaria.vn"><Mail size={16} /> hello@lunaria.vn</a>
            <Link to="/stores">Tìm cửa hàng <ArrowRight size={15} /></Link>
          </div>
        </section>
      </main>
    </div>
  );
}
