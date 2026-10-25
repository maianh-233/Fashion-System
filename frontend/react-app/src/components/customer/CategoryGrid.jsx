import { ArrowUpRight } from "lucide-react";
import { Link } from "react-router-dom";
import { monogram } from "../../pages/customer/homeLogic";

/** Danh mục gốc đang có hàng; danh mục chưa có ảnh nên dùng chữ cái đầu làm điểm nhấn. */
export default function CategoryGrid({ categories }) {
  if (categories.length === 0) return null;

  return (
    <section className="home-section home-categories" aria-labelledby="categories-title">
      <div className="home-section__heading">
        <div>
          <p>Khám phá phong cách</p>
          <h2 id="categories-title">Danh mục nổi bật</h2>
        </div>
        <Link to="/products" className="home-section__link">Tất cả sản phẩm <ArrowUpRight size={14} aria-hidden="true" /></Link>
      </div>

      <div className="home-categories__grid">
        {categories.map((category, index) => (
          <article key={category.id} className="home-category">
            <Link to={category.link} className="home-category__main" aria-label={`Xem danh mục ${category.name}, ${category.countText}`}>
              <span className="home-category__index">{String(index + 1).padStart(2, "0")}</span>
              <span className="home-category__mark" aria-hidden="true">{monogram(category.name)}</span>
              <span className="home-category__name">
                {category.name}
                <ArrowUpRight size={16} aria-hidden="true" />
              </span>
              <span className="home-category__count">{category.countText}</span>
            </Link>
            {category.children.length > 0 && (
              <ul className="home-category__children" aria-label={`Danh mục con của ${category.name}`}>
                {category.children.map(child => (
                  <li key={child.id}><Link to={child.link}>{child.name}</Link></li>
                ))}
                {category.moreChildren > 0 && <li><Link to={category.link}>+{category.moreChildren}</Link></li>}
              </ul>
            )}
          </article>
        ))}
      </div>
    </section>
  );
}
