import { useCallback, useEffect, useState } from "react";
import Button from "../../common/Button";
import Pagination from "../../common/Pagination";
import { useSystemNotification } from "../../common/SystemNotification";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import { attributeApi, tagApi } from "../../../api/catalogApi";
import { requestAdmin } from "../../../api/auth/adminSession";

export default function ProductMetadataPanel({ productId, mode }) {
  const notification = useSystemNotification();
  const { isGlobal, hasPermission } = useAdminPermissions();
  const canEdit = mode === "edit" && isGlobal && hasPermission("PRODUCT_UPDATE");
  const canViewTags = hasPermission("TAG_VIEW");
  const [attributes, setAttributes] = useState([]);
  const [tags, setTags] = useState([]);
  const [attributePage, setAttributePage] = useState(1);
  const [tagPage, setTagPage] = useState(1);
  const [attributePages, setAttributePages] = useState(1);
  const [tagPages, setTagPages] = useState(1);
  const [attributeName, setAttributeName] = useState("");
  const [attributeValue, setAttributeValue] = useState("");
  const [editingAttributeId, setEditingAttributeId] = useState(null);
  const [tagKeyword, setTagKeyword] = useState("");
  const [tagOptions, setTagOptions] = useState([]);
  const [selectedTag, setSelectedTag] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async (signal) => {
    try {
      const [attributeResult, tagResult] = await Promise.all([
        attributeApi.list(productId, { page: attributePage - 1, size: 20, sort: "createdAt,asc" }, { signal }),
        canViewTags ? requestAdmin(`/api/products/${productId}/tags?page=${tagPage - 1}&size=20&sort=name,asc`, { signal }) : Promise.resolve({ content: [], totalPages: 1 }),
      ]);
      setAttributes(attributeResult?.content || []);
      setTags(tagResult?.content || []);
      setAttributePages(Math.max(1, attributeResult?.totalPages || 1));
      setTagPages(Math.max(1, tagResult?.totalPages || 1));
      setError("");
    } catch (requestError) {
      if (requestError.name !== "AbortError") setError(requestError.message || "Không thể tải thuộc tính và Tag.");
    }
  }, [productId, attributePage, tagPage, canViewTags]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.resolve().then(() => {
      if (!controller.signal.aborted) load(controller.signal);
    });
    return () => controller.abort();
  }, [load]);

  useEffect(() => {
    if (!canEdit || !canViewTags) return undefined;
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      tagApi.list({ keyword: tagKeyword.trim(), active: true, page: 0, size: 20, sort: "name,asc" },
        { signal: controller.signal }).then((result) => setTagOptions(result?.content || []))
        .catch((requestError) => { if (requestError.name !== "AbortError") setTagOptions([]); });
    }, 250);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [canEdit, canViewTags, tagKeyword]);

  const saveAttribute = async (event) => {
    event.preventDefault();
    if (!attributeName.trim() || !attributeValue.trim()) {
      notification.warning("Tên và giá trị thuộc tính không được trống.");
      return;
    }
    setSaving(true);
    try {
      const body = { attributeName: attributeName.trim(), attributeValue: attributeValue.trim() };
      if (editingAttributeId) await attributeApi.update(productId, editingAttributeId, body);
      else await attributeApi.create(productId, body);
      setAttributeName(""); setAttributeValue(""); setEditingAttributeId(null);
      notification.success(editingAttributeId ? "Cập nhật thuộc tính thành công" : "Thêm thuộc tính thành công");
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

  return <div className="grid gap-4 lg:grid-cols-2">
    {error && <p role="alert" className="text-sm text-red-300 lg:col-span-2">{error}</p>}
    <section className="min-w-0 rounded-2xl border border-zinc-700 bg-zinc-800/30 p-4">
      <h3 className="font-semibold">Thuộc tính sản phẩm</h3>
      <p className="mt-1 text-xs text-zinc-400">Thông tin mô tả như chất liệu, kiểu dáng hoặc hướng dẫn chăm sóc.</p>
      {canEdit && <form onSubmit={saveAttribute} className="mt-4 space-y-2">
        <div className="grid gap-2 sm:grid-cols-2">
          <input value={attributeName} onChange={(event) => setAttributeName(event.target.value)} placeholder="Tên thuộc tính" aria-label="Tên thuộc tính" maxLength={100} required className="h-10 min-w-0 rounded-xl border border-zinc-700 bg-zinc-800 px-3" />
          <input value={attributeValue} onChange={(event) => setAttributeValue(event.target.value)} placeholder="Giá trị" aria-label="Giá trị thuộc tính" maxLength={255} required className="h-10 min-w-0 rounded-xl border border-zinc-700 bg-zinc-800 px-3" />
        </div>
        <div className="flex flex-wrap gap-2"><Button permission="PRODUCT_UPDATE" type="submit" loading={saving}>{editingAttributeId ? "Lưu thuộc tính" : "Thêm thuộc tính"}</Button>{editingAttributeId && <Button type="button" onClick={() => { setEditingAttributeId(null); setAttributeName(""); setAttributeValue(""); }}>Hủy sửa</Button>}</div>
      </form>}
      <div className="mt-4 space-y-2">{attributes.map((item) => <div key={item.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-zinc-800 p-3 text-sm"><span className="min-w-0 break-words"><strong>{item.attributeName}</strong>: {item.attributeValue}</span>{canEdit && <div className="flex gap-2"><Button permission="PRODUCT_UPDATE" type="button" onClick={() => { setEditingAttributeId(item.id); setAttributeName(item.attributeName); setAttributeValue(item.attributeValue); }}>Sửa</Button><Button permission="PRODUCT_UPDATE" type="button" onClick={() => deleteAttribute(item.id)}>Xóa</Button></div>}</div>)}{attributes.length === 0 && <p className="text-sm text-zinc-500">Chưa có thuộc tính.</p>}</div>
      <Pagination currentPage={attributePage} totalPages={attributePages} onPageChange={setAttributePage} />
    </section>
    <section className="min-w-0 rounded-2xl border border-zinc-700 bg-zinc-800/30 p-4">
      <h3 className="font-semibold">Tag sản phẩm</h3>
      <p className="mt-1 text-xs text-zinc-400">Gắn tag đã có vào sản phẩm để phân loại và tìm kiếm.</p>
      {canEdit && canViewTags && <div className="mt-4 space-y-2">
        <input value={tagKeyword} onChange={(event) => setTagKeyword(event.target.value)} placeholder="Tìm tag…" aria-label="Tìm tag" className="h-10 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" />
        <div className="flex flex-wrap gap-2"><select value={selectedTag} onChange={(event) => setSelectedTag(event.target.value)} aria-label="Chọn tag" className="h-10 min-w-0 flex-1 rounded-xl border border-zinc-700 bg-zinc-800 px-3"><option value="">-- Chọn tag --</option>{tagOptions.filter((tag) => !tags.some((linked) => linked.id === tag.id)).map((tag) => <option key={tag.id} value={tag.id}>{tag.name}</option>)}</select><Button permission="PRODUCT_UPDATE" type="button" onClick={linkTag} loading={saving}>Gắn tag</Button></div>
      </div>}
      {!canViewTags && <p className="mt-4 text-sm text-zinc-500">Cần quyền xem tag để quản lý nhãn của sản phẩm.</p>}
      {canViewTags && <div className="mt-4 flex flex-wrap gap-2">{tags.map((tag) => <span key={tag.id} className="inline-flex items-center gap-2 rounded-full bg-zinc-800 px-3 py-1 text-sm">{tag.name}{canEdit && <Button permission="PRODUCT_UPDATE" type="button" onClick={() => unlinkTag(tag.id)} aria-label={`Gỡ tag ${tag.name}`}>×</Button>}</span>)}{tags.length === 0 && <p className="text-sm text-zinc-500">Chưa gắn tag.</p>}</div>}
      {canViewTags && <Pagination currentPage={tagPage} totalPages={tagPages} onPageChange={setTagPage} />}
    </section>
  </div>;
}
