import { useCallback, useEffect, useState } from "react";
import Button from "../../common/Button";
import { attributeApi } from "../../../hooks/catalogApi";
import { requestAdmin } from "../../../hooks/auth/adminSession";

export default function ProductMetadataPanel({ productId }) {
  const [attributes, setAttributes] = useState([]);
  const [tags, setTags] = useState([]);
  const [error, setError] = useState("");

  const load = useCallback(async (signal) => {
    try {
      const [attributePage, tagPage] = await Promise.all([
        attributeApi.list(productId, { page: 0, size: 100, sort: "createdAt,asc" }, { signal }),
        requestAdmin(`/api/products/${productId}/tags?page=0&size=100&sort=name,asc`, { signal }),
      ]);
      setAttributes(attributePage?.content || []);
      setTags(tagPage?.content || []);
      setError("");
    } catch (requestError) {
      if (requestError.name !== "AbortError") setError(requestError.message);
    }
  }, [productId]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.resolve().then(() => {
      if (!controller.signal.aborted) load(controller.signal);
    });
    return () => controller.abort();
  }, [load]);

  const addAttribute = async () => {
    const attributeName = window.prompt("Tên thuộc tính:");
    if (!attributeName?.trim()) return;
    const attributeValue = window.prompt("Giá trị thuộc tính:");
    if (!attributeValue?.trim()) return;
    try { await attributeApi.create(productId, { attributeName, attributeValue }); await load(); }
    catch (requestError) { setError(requestError.message); }
  };

  const deleteAttribute = async (id) => {
    try { await attributeApi.remove(productId, id); await load(); }
    catch (requestError) { setError(requestError.message); }
  };

  const linkTag = async () => {
    const tagId = window.prompt("UUID của Tag cần gắn:");
    if (!tagId?.trim()) return;
    try { await requestAdmin(`/api/products/${productId}/tags/${tagId.trim()}`, { method: "POST" }); await load(); }
    catch (requestError) { setError(requestError.message); }
  };

  const unlinkTag = async (tagId) => {
    try { await requestAdmin(`/api/products/${productId}/tags/${tagId}`, { method: "DELETE" }); await load(); }
    catch (requestError) { setError(requestError.message); }
  };

  return <div className="mt-6 space-y-5 border-t border-zinc-800 pt-5">
    {error && <p className="text-sm text-red-300">{error}</p>}
    <section><div className="mb-2 flex items-center justify-between"><h3 className="font-semibold">Thuộc tính Product</h3><Button permission="PRODUCT_UPDATE" type="button" onClick={addAttribute}>Thêm thuộc tính</Button></div>
      <div className="space-y-2">{attributes.map((item) => <div key={item.id} className="flex justify-between rounded-xl bg-zinc-800 p-3 text-sm"><span>{item.attributeName}: {item.attributeValue}</span><Button permission="PRODUCT_UPDATE" type="button" onClick={() => deleteAttribute(item.id)}>Xóa</Button></div>)}{attributes.length === 0 && <p className="text-sm text-zinc-500">Chưa có thuộc tính.</p>}</div>
    </section>
    <section><div className="mb-2 flex items-center justify-between"><h3 className="font-semibold">Product Tags</h3><Button permission="PRODUCT_UPDATE" type="button" onClick={linkTag}>Gắn Tag</Button></div>
      <div className="flex flex-wrap gap-2">{tags.map((tag) => <span key={tag.id} className="inline-flex items-center gap-2 rounded-full bg-zinc-800 px-3 py-1 text-sm">{tag.name}<Button permission="PRODUCT_UPDATE" type="button" onClick={() => unlinkTag(tag.id)}>×</Button></span>)}{tags.length === 0 && <p className="text-sm text-zinc-500">Chưa có Tag.</p>}</div>
    </section>
  </div>;
}
