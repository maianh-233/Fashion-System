// pages/CollectionDetailPage.jsx
import CollectionHero from "../../components/customer/Collection/CollectionHero";
import CollectionProducts from "../../components/customer/Collection/CollectionProducts";

export default function CollectionDetailPage() {
  const collection = {
    brand_name: "Éclat",
    brand_logo: "https://via.placeholder.com/80x80/ffffff/000000?text=ÉCLAT",
    name: "Summer Elegance",
    code: "SE-2026-001",
    season: "Summer",
    year: 2026,
    release_date: "2026-06-15",
    description:
      "Khám phá vẻ đẹp thanh lịch của mùa hè qua những thiết kế lụa cao cấp và đường cắt may tinh tế.",
    banner:
      "https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=2071",
  };

  const products = Array.from({ length: 12 }).map((_, i) => ({
    id: i + 1,
    name: `Sản phẩm #${i + 1}`,
    thumbnail:
      "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab",
    min_price: 299000,
    tags: i % 2 === 0 ? ["NEW"] : [],
  }));

  return (
    <div className="min-h-screen w-full bg-[#0b0f14] text-gray-100">
      <CollectionHero collection={collection} />
      <CollectionProducts products={products} />
    </div>
  );
}