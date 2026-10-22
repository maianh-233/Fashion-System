import { ChevronLeft, ChevronRight, ZoomIn } from "lucide-react";
import { useState } from "react";

/** Ảnh sản phẩm; {@code images} là danh sách URL (ảnh của màu đang chọn đứng trước). */
export default function ProductGallery({ name, images, badge }) {
  const [activeIndex, setActiveIndex] = useState(0);
  const many = images.length > 1;

  const showPrevious = () => {
    setActiveIndex((current) =>
      current === 0 ? images.length - 1 : current - 1,
    );
  };

  const showNext = () => {
    setActiveIndex((current) =>
      current === images.length - 1 ? 0 : current + 1,
    );
  };

  const moveZoom = (event) => {
    const bounds = event.currentTarget.getBoundingClientRect();
    const x = ((event.clientX - bounds.left) / bounds.width) * 100;
    const y = ((event.clientY - bounds.top) / bounds.height) * 100;
    event.currentTarget.style.setProperty("--zoom-x", `${x}%`);
    event.currentTarget.style.setProperty("--zoom-y", `${y}%`);
  };

  return (
    <section className="product-detail-gallery" aria-label="Hình ảnh sản phẩm">
      <div className="product-detail-gallery__main" onMouseMove={moveZoom}>
        <img
          key={images[activeIndex]}
          src={images[activeIndex]}
          alt={`${name} - ảnh ${activeIndex + 1}`}
        />
        {badge && <span className="product-detail-gallery__edition">{badge}</span>}
        <span className="product-detail-gallery__zoom-hint"><ZoomIn size={14} /> Di chuột để xem gần</span>
        {many && <small>{String(activeIndex + 1).padStart(2, "0")} / {String(images.length).padStart(2, "0")}</small>}
        {many && (
          <div className="product-detail-gallery__nav">
            <button type="button" onClick={showPrevious} aria-label="Xem ảnh trước"><ChevronLeft size={17} /></button>
            <button type="button" onClick={showNext} aria-label="Xem ảnh tiếp theo"><ChevronRight size={17} /></button>
          </div>
        )}
      </div>

      {many && (
        <div className="product-detail-gallery__thumbs">
          {images.map((image, index) => (
            <button
              key={image}
              type="button"
              className={index === activeIndex ? "is-active" : ""}
              onClick={() => setActiveIndex(index)}
              aria-label={`Xem ảnh ${index + 1} của ${name}`}
            >
              <img src={image} alt="" />
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
