import Breadcrumb from "../../components/customer/ProductDetail/Breadcrumb";
import ProductGallery from "../../components/customer/ProductDetail/ProductGallery";
import ProductInfo from "../../components/customer/ProductDetail/ProductInfo";

const PRODUCT = {
  id: "LN-AW26-014",
  sku: "LNR-AUR-2604",
  name: "Đầm Midi Lụa Aurelia",
  brand: "Lunaria Atelier",
  collection: "Autumn Reverie 2026",
  price: 2890000,
  originalPrice: 3490000,
  rating: 4.9,
  reviewCount: 128,
  stock: 18,
  description:
    "Aurelia được cắt may trên nền lụa satin có độ rủ tự nhiên, cổ đổ mềm và chiết eo vừa vặn. Thiết kế mang tinh thần tối giản, phù hợp từ buổi tiệc tối đến những dịp trang trọng ban ngày.",
  images: [
    "https://images.unsplash.com/photo-1566174053879-31528523f8ae?q=85&w=1200&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1595777457583-95e059d581b8?q=85&w=1200&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1585487000160-6ebcfceb0d03?q=85&w=1200&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1566479179817-c0d9d1868b18?q=85&w=1200&auto=format&fit=crop",
  ],
  colors: [
    { id: "champagne", label: "Champagne", hex: "#c7aa7b" },
    { id: "midnight", label: "Midnight", hex: "#25252a" },
    { id: "rosewood", label: "Rosewood", hex: "#754b4c" },
  ],
  sizes: ["XS", "S", "M", "L", "XL"],
  details: [
    ["Chất liệu", "Lụa satin cao cấp"],
    ["Phom dáng", "Midi · ôm nhẹ phần eo"],
    ["Lớp lót", "Lụa viscose thoáng khí"],
    ["Sản xuất", "Hoàn thiện tại Việt Nam"],
  ],
  tags: ["Lụa satin", "Midi", "Limited", "Autumn 2026"],
};

export default function ProductDetail() {
  return (
    <div className="customer-page product-detail-page min-h-screen w-full px-4 py-6 text-gray-200 sm:px-6 sm:py-8">
      <div className="customer-page__wide">
        <Breadcrumb product={PRODUCT} />
        <div className="customer-product-detail-grid">
          <ProductGallery product={PRODUCT} />
          <ProductInfo product={PRODUCT} />
        </div>
      </div>
    </div>
  );
}
