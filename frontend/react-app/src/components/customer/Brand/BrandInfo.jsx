import { Hash, Layers3, Package } from "lucide-react";

/** Số liệu thật của thương hiệu: sản phẩm đang bán, bộ sưu tập đang phát hành, mã thương hiệu. */
export default function BrandInfo({ brand }) {
  const stats = [
    { icon: Package, label: "Sản phẩm đang bán", value: brand.productCount },
    { icon: Layers3, label: "Bộ sưu tập", value: brand.collectionCount },
    { icon: Hash, label: "Mã thương hiệu", value: brand.code },
  ];
  return (
    <div className="customer-brand-info mb-12 grid grid-cols-1 gap-4 md:grid-cols-3">
      {stats.map(({ icon: Icon, label, value }) => (
        <div key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5">
          <Icon className="mb-3 text-amber-500" size={24} />
          <h3 className="text-base font-semibold">{value}</h3>
          <p className="mt-1 text-gray-400">{label}</p>
        </div>
      ))}
    </div>
  );
}
