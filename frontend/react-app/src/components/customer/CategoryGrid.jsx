import { ArrowUpRight } from "lucide-react";
import Button from "../common/Button";

export default function CategoryGrid({ categories }) {
  return (
    <section className="home-section home-categories" aria-labelledby="categories-title">
      <div className="home-section__heading">
        <div>
          <p>Khám phá phong cách</p>
          <h2 id="categories-title">Danh mục nổi bật</h2>
        </div>
        <span>Chọn món đồ dành cho bạn</span>
      </div>

      <div className="home-categories__grid">
        {categories.map((category) => (
          <Button
            key={category.id}
            type="button"
            variant="unstyled"
            className="home-category"
            aria-label={`Xem danh mục ${category.label}`}
          >
            <span className="home-category__media">
              <img src={category.image} alt={category.label} loading="lazy" />
              <span className="home-category__shade" />
            </span>
            <span className="home-category__label">
              {category.label}
              <ArrowUpRight size={14} aria-hidden="true" />
            </span>
          </Button>
        ))}
      </div>
    </section>
  );
}
