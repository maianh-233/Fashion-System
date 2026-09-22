import { useCallback, useEffect, useState } from "react";
import Button from "../../common/Button";
import Pagination from "../../common/Pagination";
import { useSystemNotification } from "../../common/SystemNotification";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import { attributeApi, tagApi } from "../../../api/catalogApi";
import { requestAdmin } from "../../../api/auth/adminSession";

export default function ProductMetadataPanel({ productId }) {
  const notification = useSystemNotification();
  const { isGlobal, hasPermission } = useAdminPermissions();
  const canEdit = isGlobal && hasPermission("PRODUCT_UPDATE");
  const [attributes, setAttributes] = useState([]);
  const [tags, setTags] = useState([]);
  const [attributePage, setAttributePage] = useState(1);
  const [tagPage, setTagPage] = useState(1);
  const [attributePages, setAttributePages] = useState(1);
  const [tagPages, setTagPages] = useState(1);
  const [attributeName, setAttributeName] = useState("");
  const [attributeValue, setAttributeValue] = useState("");
  const [tagKeyword, setTagKeyword] = useState("");
  const [tagOptions, setTagOptions] = useState([]);
  const [selectedTag, setSelectedTag] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async (signal) => {
    try {
      const [attributeResult, tagResult] = await Promise.all([
        attributeApi.list(productId, { page: attributePage - 1, size: 20, sort: "createdAt,asc" }, { signal }),
        requestAdmin(`/api/products/${productId}/tags?page=${tagPage - 1}&size=20&sort=name,asc`, { signal }),
      ]);
      setAttributes(attributeResult?.content || []);
      setTags(tagResult?.content || []);
      setAttributePages(Math.max(1, attributeResult?.totalPages || 1));
      setTagPages(Math.max(1, tagResult?.totalPages || 1));
      setError("");
    } catch (requestError) {
      if (requestError.name !== "AbortError") setError(requestError.message || "Không thể tải thuộc tính và Tag.");
    }
  }, [productId, attributePage, tagPage]);

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  useEffect(() => {
    if (!canEdit) return undefined;
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      tagApi.list({ keyword: tagKeyword.trim(), active: true, page: 0, size: 20, sort: "name,asc" },
        { signal: controller.signal }).then((result) => setTagOptions(result?.content || []))
        .catch((requestError) => { if (requestError.name !== "AbortError") setTagOptions([]); });
    }, 250);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [canEdit, tagKeyword]);

  const addAttribute = async (event) => {
    event.preventDefault();
    if (!attributeName.trim() || !attributeValue.trim()) {
      notification.warning("Tên và giá trị thuộc tính không được trống.");
      return;
    }
    setSaving(true);
    try {
      await attributeApi.create(productId, { attributeName: attributeName.trim(), attributeValue: attributeValue.trim() });
      setAttributeName(""); setAttributeValue("");
      notification.success("Thêm thuộc tính thành công");
      await load();
    } catch (requestError) { notification.error(requestError.message); }
    finally { setSaving(false); }
  };

  const deleteAttribute = async (id) => {
    if (!await notification.confirm({ message: "Xóa thuộc tính này khỏi sản phẩm?", destructive: true })) return;
    try { await attributeApi.remove(productId, id); notification.success("Đã xóa thuộc tính"); await load(); }
    catch (requestError) { notification.error(requestError.message); }
  };

  const linkTag = async () => {
    if (!selectedTag) { notification.warning("Hãy chọn Tag."); return; }
    setSaving(true);
    try {
      await requestAdmin(`/api/products/${productId}/tags/${selectedTag}`, { method: "POST" });
      setSelectedTag(""); notification.success("Đã gắn Tag"); await load();
    } catch (requestError) { notification.warning(requestError.message); }
    finally { setSaving(false); }
  };

  const unlinkTag = async (tagId) => {
    if (!await notification.confirm({ message: "Gỡ Tag này khỏi sản phẩm?", destructive: true })) return;
    try { await requestAdmin(`/api/products/${productId}/tags/${tagId}`, { method: "DELETE" }); notification.success("Đã gỡ Tag"); await load(); }
    catch (requestError) { notification.error(requestError.message); }
  };

  return <div className="space-y-5">
    {error && <p className="text-sm text-red-300">{error}</p>}
    <section><h3 className="mb-2 font-semibold">Thuộc tính sản phẩm</h3>
      {canEdit && <form onSubmit={addAttribute} className="mb-3 flex flex-wrap gap-2"><input value={attributeName} onChange={(event) => setAttributeName(event.target.value)} placeholder="Tên thuộc tính" className="h-10 rounded-xl border border-zinc-700 bg-zinc-800 px-3" /><input value={attributeValue} onChange={(event) => setAttributeValue(event.target.value)} placeholder="Giá trị" className="h-10 rounded-xl border border-zinc-700 bg-zinc-800 px-3" /><Button permission="PRODUCT_UPDATE" type="submit" loading={saving}>Thêm thuộc tính</Button></form>}
      <div className="space-y-2">{attributes.map((item) => <div key={item.id} className="flex justify-between rounded-xl bg-zinc-800 p-3 text-sm"><span>{item.attributeName}: {item.attributeValue}</span>{canEdit && <Button permission="PRODUCT_UPDATE" type="button" onClick={() => deleteAttribute(item.id)}>Xóa</Button>}</div>)}{attributes.length === 0 && <p className="text-sm text-zinc-500">Chưa có thuộc tính.</p>}</div>
      <Pagination currentPage={attributePage} totalPages={attributePages} onPageChange={setAttributePage} />
    </section>
    <section><h3 className="mb-2 font-semibold">Product Tags</h3>
      {canEdit && <div className="mb-3 flex flex-wrap gap-2"><input value={tagKeyword} onChange={(event) => setTagKeyword(event.target.value)} placeholder="Tìm Tag…" className="h-10 rounded-xl border border-zinc-700 bg-zinc-800 px-3" /><select value={selectedTag} onChange={(event) => setSelectedTag(event.target.value)} className="h-10 rounded-xl border border-zinc-700 bg-zinc-800 px-3"><option value="">-- Chọn Tag --</option>{tagOptions.map((tag) => <option key={tag.id} value={tag.id}>{tag.name}</option>)}</select><Button permission="PRODUCT_UPDATE" type="button" onClick={linkTag} loading={saving}>Gắn Tag</Button></div>}
      <div className="flex flex-wrap gap-2">{tags.map((tag) => <span key={tag.id} className="inline-flex items-center gap-2 rounded-full bg-zinc-800 px-3 py-1 text-sm">{tag.name}{canEdit && <Button permission="PRODUCT_UPDATE" type="button" onClick={() => unlinkTag(tag.id)}>×</Button>}</span>)}{tags.length === 0 && <p className="text-sm text-zinc-500">Chưa có Tag.</p>}</div>
      <Pagination currentPage={tagPage} totalPages={tagPages} onPageChange={setTagPage} />
    </section>
  </div>;
}
