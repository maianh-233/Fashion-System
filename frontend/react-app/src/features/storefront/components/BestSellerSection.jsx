import ProductCard from "./ProductCard";

export default function BestSellerSection({ products }) {
  // Dữ liệu giả để test giao diện
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

  const mappedProducts = (products && products.length > 0)
    ? products.map((p) => ({
        id: p.id,
        name: p.name,
        min_price: p.min_price || p.price || 0,
        thumbnail: p.thumbnail || "/images/placeholder.jpg",
        tags: p.tags || [],
      }))
    : mockProducts;

  return (
    <section>
      <h2 className="mb-8 text-2xl font-light tracking-[0.18em] sm:mb-10 sm:text-3xl sm:tracking-[0.25em]">
        TOP BÁN CHẠY
      </h2>

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-6 lg:grid-cols-4 lg:gap-8">
        {mappedProducts.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </section>
  );
}