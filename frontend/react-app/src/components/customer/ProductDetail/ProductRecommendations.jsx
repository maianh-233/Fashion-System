import { useEffect, useState } from "react";
import ProductCard from "../Product/ProductCard";
import { storefrontApi } from "../../../api/storefront/storefrontApi";
import { recommendationIds, splitRecommendations } from "../../../pages/customer/recommendationLogic";

function Row({ eyebrow, title, products }) {
  if (products.length === 0) return null;
  return (
    <section className="home-section product-recommendations">
      <div className="home-section__heading">
        <div>
          <p>{eyebrow}</p>
          <h2>{title}</h2>
        </div>
      </div>
      <div className="flex gap-3 overflow-x-auto snap-x snap-mandatory [&::-webkit-scrollbar]:hidden [scrollbar-width:none]
        sm:grid sm:grid-cols-2 sm:gap-5 sm:overflow-visible lg:grid-cols-4 lg:gap-6">
        {products.map((product) => (
          <div key={product.id} className="min-w-[68%] snap-start sm:min-w-0">
            <ProductCard product={product} />
          </div>
        ))}
      </div>
    </section>
  );
}

/** "Thường được mua cùng" (từ các đơn có sản phẩm này) và "Có thể bạn cũng thích" (cùng danh mục / BST / thương hiệu). */
export default function ProductRecommendations({ productId }) {
  const [rows, setRows] = useState({ boughtTogether: [], similar: [] });

  useEffect(() => {
    const controller = new AbortController();
    // 4 sản phẩm mỗi nhóm: vừa một hàng trên desktop, cuộn ngang trên mobile.
    storefrontApi.recommendations(productId, 4, { signal: controller.signal })
      .then(async (result) => {
        const ids = recommendationIds(result);
        if (ids.length === 0) return;
        const query = new URLSearchParams({ page: "0", size: String(ids.length) });
        ids.forEach((id) => query.append("ids", id));
        const page = await storefrontApi.products(query, { signal: controller.signal });
        setRows(splitRecommendations(result, page.content || []));
      })
      .catch(() => {});
    return () => controller.abort();
  }, [productId]);

  return (
    <>
      <Row eyebrow="Khách khác cũng mua" title="Thường được mua cùng" products={rows.boughtTogether} />
      <Row eyebrow="Gợi ý cho bạn" title="Có thể bạn cũng thích" products={rows.similar} />
    </>
  );
}
