import { Gem, Leaf, ShieldCheck } from "lucide-react";

const valueIcons = {
  "value-1": Gem,
  "value-2": ShieldCheck,
  "value-3": Leaf,
};

export default function ValuesSection({ values }) {
  return (
    <section className="home-section home-values" aria-labelledby="values-title">
      <div className="home-values__intro">
        <p className="home-values__eyebrow">Giá trị tạo nên Lunaria</p>
        <h2 id="values-title">Đẹp không chỉ ở dáng vẻ.</h2>
        <p>
          Mỗi thiết kế cần đẹp khi nhìn, dễ chịu khi mặc và có giá trị đủ lâu
          để trở thành một phần trong câu chuyện của bạn.
        </p>
        <blockquote>
          “{values[0]?.quote}”
        </blockquote>
      </div>

      <div className="home-values__list">
        {values.map((value, index) => {
          const Icon = valueIcons[value.id] ?? Gem;
          return (
            <article key={value.id} className="home-value-card">
              <span className="home-value-card__index">0{index + 1}</span>
              <span className="home-value-card__icon"><Icon size={20} /></span>
              <div>
                <h3>{value.title}</h3>
                <p>{value.description}</p>
                <strong>{value.promise}</strong>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
