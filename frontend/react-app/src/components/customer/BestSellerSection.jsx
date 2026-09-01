import ProductCard from "./Product/ProductCard";

export default function BestSellerSection({ products }) {
  const mockProducts = [
    {
      id: "1",
      name: "Áo Thun Basic Cotton Nam",
      min_price: 299000,
      thumbnail: "/images/placeholder.jpg",
      tags: ["BEST SELLER"],
    },
    {
      id: "2",
      name: "Quần Jean Slim Fit",
      min_price: 599000,
      thumbnail: "/images/placeholder.jpg",
      tags: ["SALE"],
    },
    {
      id: "3",
      name: "Áo Hoodie Premium",
      min_price: 799000,
      thumbnail: "/images/placeholder.jpg",
      tags: ["NEW", "BEST SELLER"],
    },
    {
      id: "4",
      name: "Quần Shorts Nam Mùa Hè",
      min_price: 399000,
      thumbnail: "/images/placeholder.jpg",
      tags: ["TRENDING"],
    },
  ];

  const mappedProducts =
    products && products.length > 0
      ? products.map((p) => ({
          id: p.id,
          name: p.name,
          min_price: p.min_price || p.price || 0,
          thumbnail: p.thumbnail || "/images/placeholder.jpg",
          tags: p.tags || [],
        }))
      : mockProducts;

  return (
    <section className="home-section">
      <div className="home-section__heading">
        <div>
          <p>Được yêu thích nhất</p>
          <h2>Top bán chạy</h2>
        </div>
        <span>Tuyển chọn của Lunaria</span>
      </div>

      <div
        className="
          flex gap-3
          overflow-x-auto
          snap-x snap-mandatory

          [&::-webkit-scrollbar]:hidden
          [scrollbar-width:none]

          sm:grid sm:grid-cols-2 sm:gap-5 sm:overflow-visible
          lg:grid-cols-4 lg:gap-6
        "
      >
        {mappedProducts.map((product) => (
          <div
            key={product.id}
            className="min-w-[68%] snap-start sm:min-w-0"
          >
            <ProductCard product={product} />
          </div>
        ))}
      </div>
    </section>
  );
}
