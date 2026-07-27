import { useMemo, useState } from "react";
import {
  X,
  Search,
  ShoppingBag,
  Minus,
  Plus,
  Check,
} from "lucide-react";

export default function ProductPickerDialog({
  open,
  products = [],
  onClose,
  onAdd,
}) {
const [keyword, setKeyword] = useState("");
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [quantity, setQuantity] = useState(1);

  // ✅ Hook phải nằm trước mọi return
  const filteredProducts = useMemo(() => {
    return products.filter((item) =>
      item.name.toLowerCase().includes(keyword.toLowerCase())
    );
  }, [keyword, products]);

  // ✅ Sau khi tất cả Hooks đã được gọi mới được return
  if (!open) return null;

  const handleSelectProduct = (product) => {
    setSelectedProduct(product);
    setSelectedVariant(product.variants?.[0] || null);
    setQuantity(1);
  };

  const handleAdd = () => {
    if (!selectedProduct || !selectedVariant) return;

    onAdd?.({
      productId: selectedProduct.id,
      variantId: selectedVariant.id,

      name: selectedProduct.name,
      image: selectedProduct.image,

      sku: selectedVariant.sku,
      color: selectedVariant.color,
      size: selectedVariant.size,

      price: selectedVariant.price,
      quantity,

      total: quantity * selectedVariant.price,
    });

    setSelectedProduct(null);
    setSelectedVariant(null);
    setKeyword("");
    setQuantity(1);

    onClose();
  };

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/70 backdrop-blur-sm">

      <div className="w-full max-w-6xl rounded-2xl bg-[#1a1a1a] border border-zinc-700 overflow-hidden">

        {/* HEADER */}

        <div className="flex items-center justify-between border-b border-zinc-700 px-6 py-4">

          <div>

            <h2 className="text-xl font-bold text-orange-400">
              Chọn sản phẩm
            </h2>

            <p className="text-sm text-zinc-400">
              Thêm sản phẩm vào đơn hàng
            </p>

          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-zinc-800"
          >
            <X className="text-white" size={20} />
          </button>

        </div>

        {/* BODY */}

        <div className="grid grid-cols-2 h-[650px]">

          {/* LEFT */}

          <div className="border-r border-zinc-700 flex flex-col">

            <div className="p-5">

              <div className="relative">

                <Search
                  size={18}
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
                />

                <input
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  placeholder="Tìm sản phẩm..."
                  className="w-full rounded-lg bg-zinc-900 border border-zinc-700 py-2 pl-10 pr-4 text-white outline-none focus:ring-2 focus:ring-orange-500"
                />

              </div>

            </div>

            <div className="flex-1 overflow-y-auto">

              {filteredProducts.map((product) => (

                <button
                  key={product.id}
                  onClick={() => handleSelectProduct(product)}
                  className={`w-full flex gap-4 p-4 text-left transition border-b border-zinc-800 ${
                    selectedProduct?.id === product.id
                      ? "bg-orange-500/10"
                      : "hover:bg-zinc-800"
                  }`}
                >

                  <img
                    src={product.image}
                    className="w-20 h-20 rounded-lg object-cover border border-zinc-700"
                  />

                  <div className="flex-1">

                    <h4 className="text-white font-medium">
                      {product.name}
                    </h4>

                    <p className="text-zinc-500 text-sm mt-2">
                      {product.brand}
                    </p>

                    <p className="text-orange-400 mt-2 font-semibold">
                      {selectedProduct?.id === product.id
                        ? `${product.variants.length} biến thể`
                        : ""}
                    </p>

                  </div>

                </button>

              ))}

            </div>

          </div>

          {/* RIGHT */}

          <div className="p-6 overflow-y-auto">

            {!selectedProduct && (

              <div className="flex h-full items-center justify-center">

                <div className="text-center">

                  <ShoppingBag
                    size={70}
                    className="mx-auto text-zinc-600"
                  />

                  <p className="mt-5 text-zinc-500">
                    Chọn sản phẩm bên trái
                  </p>

                </div>

              </div>

            )}

            {selectedProduct && (

              <div>

                <img
                  src={selectedProduct.image}
                  className="w-44 h-44 rounded-xl object-cover border border-zinc-700 mx-auto"
                />

                <h3 className="mt-5 text-center text-xl text-white font-bold">
                  {selectedProduct.name}
                </h3>

                {/* VARIANT */}

                <div className="mt-8">

                  <label className="text-sm text-zinc-400">
                    Chọn biến thể
                  </label>

                  <div className="grid grid-cols-2 gap-3 mt-3">

                    {selectedProduct.variants.map((variant) => (

                      <button
                        key={variant.id}
                        onClick={() =>
                          setSelectedVariant(variant)
                        }
                        className={`rounded-xl border p-4 text-left transition ${
                          selectedVariant?.id === variant.id
                            ? "border-orange-500 bg-orange-500/10"
                            : "border-zinc-700 hover:border-orange-400"
                        }`}
                      >

                        <div className="flex justify-between">

                          <div>

                            <div className="text-white font-medium">
                              {variant.color}
                            </div>

                            <div className="text-zinc-400 text-sm">
                              Size {variant.size}
                            </div>

                          </div>

                          {selectedVariant?.id ===
                            variant.id && (
                            <Check
                              className="text-orange-400"
                              size={18}
                            />
                          )}

                        </div>

                        <div className="mt-3 text-orange-400 font-bold">
                          {variant.price.toLocaleString(
                            "vi-VN"
                          )}
                          ₫
                        </div>

                        <div className="mt-1 text-xs text-zinc-500">
                          SKU: {variant.sku}
                        </div>

                      </button>

                    ))}

                  </div>

                </div>

                {/* QUANTITY */}

                <div className="mt-8">

                  <label className="text-sm text-zinc-400">
                    Số lượng
                  </label>

                  <div className="flex items-center gap-3 mt-3">

                    <button
                      onClick={() =>
                        setQuantity((q) =>
                          Math.max(1, q - 1)
                        )
                      }
                      className="p-2 rounded-lg border border-zinc-700 hover:bg-zinc-800"
                    >
                      <Minus className="text-white" size={16} />
                    </button>

                    <div className="w-16 text-center text-lg text-white font-semibold">
                      {quantity}
                    </div>

                    <button
                      onClick={() =>
                        setQuantity((q) => q + 1)
                      }
                      className="p-2 rounded-lg border border-zinc-700 hover:bg-zinc-800"
                    >
                      <Plus className="text-white" size={16} />
                    </button>

                  </div>

                </div>

              </div>

            )}

          </div>

        </div>

        {/* FOOTER */}

        <div className="flex justify-end gap-3 border-t border-zinc-700 px-6 py-4">

          <button
            onClick={onClose}
            className="px-5 py-2 rounded-lg border border-zinc-700 text-white hover:bg-zinc-800"
          >
            Hủy
          </button>

          <button
            disabled={!selectedVariant}
            onClick={handleAdd}
            className="px-6 py-2 rounded-lg bg-orange-500 hover:bg-orange-600 text-white disabled:opacity-40"
          >
            Thêm vào đơn
          </button>

        </div>

      </div>

    </div>
  );
}