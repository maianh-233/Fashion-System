import ProductBrowser from "../Product/ProductBrowser";

/** Sản phẩm của một bộ sưu tập (API storefront, lọc theo collectionId). */
export default function CollectionProducts({ collection }) {
  return (
    <section className="customer-collection-products w-full py-10">
      <ProductBrowser
        scope={{ collectionId: collection.id }}
        intro={{ eyebrow: collection.name, title: "Sản phẩm trong bộ sưu tập", description: "Tìm và lọc trong các thiết kế của bộ sưu tập này." }}
      />
    </section>
  );
}
