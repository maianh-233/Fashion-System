import Price from "./ProductInfor/Price";
import ColorSelector from "./ProductInfor/ColorSelector";
import SizeSelector from "./ProductInfor/SizeSelector";
import ActionButtons from "./ProductInfor/ActionButtons";
import Description from "./ProductInfor/Description";
import ExtraInfo from "./ProductInfor/ExtraInfo";
import Tags from "./ProductInfor/Tags";

export default function ProductInfo() {
  return (
    <div>
      {/* Brand */}
      <div className="flex items-center gap-3 mb-3">
        <img src="https://picsum.photos/40/40" className="w-8 h-8 rounded-full" />
        <a className="text-xl font-semibold hover:text-[#FFCC00]">Nike</a>
      </div>

      <h1 className="text-3xl md:text-4xl font-bold text-white">
        Ultra Cotton Oversize Tee
      </h1>

      <p className="text-gray-400 mt-2">
        Collection: <span className="text-[#FFCC00]">Summer 2026</span>
      </p>

      <Price />
      <ColorSelector />
      <SizeSelector />
      <ActionButtons />
      <Description />
      <ExtraInfo />
      <Tags />
    </div>
  );
}