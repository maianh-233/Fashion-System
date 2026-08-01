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

  // order | receipt
  mode = "order",
}) {
  const [keyword, setKeyword] = useState("");
  const [selectedProduct, setSelectedProduct] =
    useState(null);
  const [selectedVariant, setSelectedVariant] =
    useState(null);
  const [quantity, setQuantity] = useState(1);

  /* =====================================================
      CONFIG
  ===================================================== */

  const config = {
    order: {
      title: "Chọn sản phẩm",
      description: "Thêm sản phẩm vào đơn hàng",
      button: "Thêm vào đơn",
      priceField: "price",
      showTotal: true,
      showPrice: true,
    },

    receipt: {
      title: "Chọn sản phẩm",
      description: "Thêm sản phẩm vào phiếu nhập",
      button: "Thêm vào phiếu nhập",
      priceField: "costPrice",
      showTotal: true,
      showPrice: true,
    },

    issue: {
      title: "Chọn sản phẩm",
      description: "Thêm sản phẩm vào phiếu xuất",
      button: "Thêm vào phiếu xuất",
      priceField: "costPrice",
      showTotal: false,
      showPrice: false,
    },
  };

  const currentConfig =
    config[mode] || config.order;

  /* =====================================================
      FILTER
  ===================================================== */

  const filteredProducts = useMemo(() => {
    return products.filter((item) =>
      item.name
        .toLowerCase()
        .includes(keyword.toLowerCase())
    );
  }, [keyword, products]);

  /* =====================================================
      RETURN NULL
  ===================================================== */

  if (!open) return null;

  /* =====================================================
      SELECT PRODUCT
  ===================================================== */

  const handleSelectProduct = (product) => {
    setSelectedProduct(product);
    setSelectedVariant(
      product.variants?.[0] || null
    );
    setQuantity(1);
  };

  /* =====================================================
      PRICE
  ===================================================== */

  const getVariantPrice = (variant) => {
    return (
      Number(
        variant?.[currentConfig.priceField]
      ) || 0
    );
  };

  /* =====================================================
      ADD
  ===================================================== */

  const handleAdd = () => {
    if (!selectedProduct || !selectedVariant)
      return;

    const price = getVariantPrice(
      selectedVariant
    );

    const item = {
      productId: selectedProduct.id,
      variantId: selectedVariant.id,

      name: selectedProduct.name,
      image: selectedProduct.image,

      sku: selectedVariant.sku,
      color: selectedVariant.color,
      size: selectedVariant.size,

      quantity,
    };

    if (mode === "order") {
      item.price = price;
      item.total = quantity * price;
    }

    if (mode === "receipt") {
      item.costPrice = price;
      item.total = quantity * price;
    }

    if (mode === "issue") {
      item.costPrice = price;
    }

    onAdd?.(item);

    // reset
    setSelectedProduct(null);
    setSelectedVariant(null);
    setKeyword("");
    setQuantity(1);

    onClose?.();
  };

    return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <div className="w-full max-w-6xl overflow-hidden rounded-2xl border border-zinc-700 bg-[#1a1a1a]">

        {/* ================= HEADER ================= */}

        <div className="flex items-center justify-between border-b border-zinc-700 px-6 py-4">
          <div>
            <h2 className="text-xl font-bold text-orange-400">
              {currentConfig.title}
            </h2>

            <p className="text-sm text-zinc-400">
              {currentConfig.description}
            </p>
          </div>

          <button
            onClick={onClose}
            className="rounded-lg p-2 hover:bg-zinc-800"
          >
            <X
              size={20}
              className="text-white"
            />
          </button>
        </div>

        {/* ================= BODY ================= */}

        <div className="grid h-[650px] grid-cols-2">

          {/* ================= LEFT ================= */}

          <div className="flex flex-col border-r border-zinc-700">

            {/* SEARCH */}

            <div className="p-5">
              <div className="relative">
                <Search
                  size={18}
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
                />

                <input
                  value={keyword}
                  onChange={(e) =>
                    setKeyword(e.target.value)
                  }
                  placeholder="Tìm sản phẩm..."
                  className="w-full rounded-lg border border-zinc-700 bg-zinc-900 py-2 pl-10 pr-4 text-white outline-none focus:ring-2 focus:ring-orange-500"
                />
              </div>
            </div>

            {/* PRODUCT LIST */}

            <div className="flex-1 overflow-y-auto">

              {filteredProducts.length === 0 && (
                <div className="flex h-full items-center justify-center text-zinc-500">
                  Không tìm thấy sản phẩm
                </div>
              )}

              {filteredProducts.map((product) => (
                <button
                  key={product.id}
                  onClick={() =>
                    handleSelectProduct(product)
                  }
                  className={`flex w-full gap-4 border-b border-zinc-800 p-4 text-left transition ${
                    selectedProduct?.id === product.id
                      ? "bg-orange-500/10"
                      : "hover:bg-zinc-800"
                  }`}
                >
                  <img
                    src={product.image}
                    alt={product.name}
                    className="h-20 w-20 rounded-lg border border-zinc-700 object-cover"
                  />

                  <div className="flex flex-1 flex-col justify-center">
                    <h4 className="font-medium text-white">
                      {product.name}
                    </h4>

                    <p className="mt-2 text-sm text-zinc-500">
                      {product.brand}
                    </p>

                    <div className="mt-3 flex items-center justify-between">
                      <span className="text-xs text-zinc-500">
                        {product.variants?.length || 0} biến thể
                      </span>

                      {selectedProduct?.id === product.id && (
                        <span className="rounded-full bg-orange-500 px-2 py-1 text-xs text-white">
                          Đã chọn
                        </span>
                      )}
                    </div>
                  </div>
                </button>
              ))}

            </div>
          </div>

          {/* ====== RIGHT (Phần 2 sẽ bắt đầu từ đây) ====== */}
          {/* ================= RIGHT ================= */}

          <div className="overflow-y-auto p-6">

            {!selectedProduct ? (
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
            ) : (
              <div>
                {/* IMAGE */}

                <img
                  src={selectedProduct.image}
                  alt={selectedProduct.name}
                  className="mx-auto h-44 w-44 rounded-xl border border-zinc-700 object-cover"
                />

                <h3 className="mt-5 text-center text-xl font-bold text-white">
                  {selectedProduct.name}
                </h3>

                <p className="mt-2 text-center text-sm text-zinc-500">
                  {selectedProduct.brand}
                </p>

                {/* VARIANT */}

                <div className="mt-8">
                  <label className="text-sm text-zinc-400">
                    Chọn biến thể
                  </label>

                  <div className="mt-3 grid grid-cols-2 gap-3">

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
                            <div className="font-medium text-white">
                              {variant.color}
                            </div>

                            <div className="text-sm text-zinc-400">
                              Size {variant.size}
                            </div>
                          </div>

                          {selectedVariant?.id === variant.id && (
                            <Check
                              size={18}
                              className="text-orange-400"
                            />
                          )}
                        </div>

                        <div className="mt-3 font-bold text-orange-400">
                          {getVariantPrice(variant).toLocaleString(
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

                  <div className="mt-3 flex items-center gap-3">
                    <button
                      onClick={() =>
                        setQuantity((q) =>
                          Math.max(1, q - 1)
                        )
                      }
                      className="rounded-lg border border-zinc-700 p-2 hover:bg-zinc-800"
                    >
                      <Minus
                        size={16}
                        className="text-white"
                      />
                    </button>

                    <div className="w-16 text-center text-lg font-semibold text-white">
                      {quantity}
                    </div>

                    <button
                      onClick={() =>
                        setQuantity((q) => q + 1)
                      }
                      className="rounded-lg border border-zinc-700 p-2 hover:bg-zinc-800"
                    >
                      <Plus
                        size={16}
                        className="text-white"
                      />
                    </button>
                  </div>

                  {/* SUMMARY */}

                  {selectedVariant && currentConfig.showTotal && (
                    <div className="mt-6 rounded-xl border border-zinc-700 bg-zinc-900 p-4">
                      <div className="flex justify-between text-sm">
                        <span className="text-zinc-400">
                          Đơn giá
                        </span>

                        <span className="font-medium text-white">
                          {getVariantPrice(
                            selectedVariant
                          ).toLocaleString("vi-VN")}
                          ₫
                        </span>
                      </div>

                      <div className="mt-3 flex justify-between text-sm">
                        <span className="text-zinc-400">
                          Số lượng
                        </span>

                        <span className="font-medium text-white">
                          {quantity}
                        </span>
                      </div>

                      <div className="mt-4 border-t border-zinc-700 pt-4">
                        <div className="flex justify-between">
                          <span className="font-medium text-zinc-300">
                            Thành tiền
                          </span>

                          <span className="text-lg font-bold text-orange-400">
                            {(
                              getVariantPrice(selectedVariant) *
                              quantity
                            ).toLocaleString("vi-VN")}
                            ₫
                          </span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* ================= FOOTER ================= */}

        <div className="flex justify-end gap-3 border-t border-zinc-700 px-6 py-4">
          <button
            onClick={onClose}
            className="rounded-lg border border-zinc-700 px-5 py-2 text-white hover:bg-zinc-800"
          >
            Hủy
          </button>

          <button
            disabled={!selectedVariant}
            onClick={handleAdd}
            className="rounded-lg bg-orange-500 px-6 py-2 text-white hover:bg-orange-600 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {currentConfig.button}
          </button>
        </div>
      </div>
    </div>
  );
}