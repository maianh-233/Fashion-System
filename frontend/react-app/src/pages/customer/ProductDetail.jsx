import Breadcrumb from "../../components/customer/ProductDetail/Breadcrumb";
import ProductGallery from "../../components/customer/ProductDetail/ProductGallery";
import ProductInfo from "../../components/customer/ProductDetail/ProductInfo";

export default function ProductDetail() {
  return (
    <div className="w-full xl:px-12 px-4 py-8 text-gray-200">
      <Breadcrumb />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-12">
        <ProductGallery />
        <ProductInfo />
      </div>
    </div>
  );
}