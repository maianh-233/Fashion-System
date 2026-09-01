// pages/CollectionDetailPage.jsx
import CollectionHero from "../../components/customer/Collection/CollectionHero";
import CollectionProducts from "../../components/customer/Collection/CollectionProducts";

export default function CollectionDetailPage() {
  const collection = {
    brand_name: "Gucci",
    brand_logo: "https://commons.wikimedia.org/wiki/Special:Redirect/file/Gucci_Logo.svg",
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

  const productImages = [
    "https://images.unsplash.com/photo-1566174053879-31528523f8ae?q=80&w=700&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1595777457583-95e059d581b8?q=80&w=700&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?q=80&w=700&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1543163521-1bf539c55dd2?q=80&w=700&auto=format&fit=crop",
  ];

  const products = Array.from({ length: 12 }).map((_, i) => ({
    id: i + 1,
    name: `Sản phẩm #${i + 1}`,
    thumbnail: productImages[i % productImages.length],
    min_price: 1890000 + (i % 4) * 350000,
    tags: i % 2 === 0 ? ["NEW"] : [],
  }));

  return (
    <div className="customer-page customer-detail-page min-h-screen w-full text-gray-100">
      <CollectionHero collection={collection} />
      <CollectionProducts products={products} />
    </div>
  );
}
