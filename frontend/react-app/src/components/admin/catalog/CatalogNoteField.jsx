import { useState } from "react";
import { shouldClampNote } from "./catalogFieldLogic";

export default function CatalogNoteField({ value, disabled, onChange, required }) {
  const [expanded, setExpanded] = useState(false);
  const note = String(value || "");
  const clamped = shouldClampNote(note);

  if (disabled) {
    if (!note) return <p className="rounded-xl border border-zinc-800 bg-zinc-950/40 p-3 text-zinc-500">—</p>;
    return <div className="space-y-2"><p className={`whitespace-pre-wrap break-words rounded-xl border border-zinc-800 bg-zinc-950/40 p-3 leading-6 text-zinc-200 ${clamped && !expanded ? "line-clamp-6" : ""}`}>{note}</p>{clamped && <button type="button" aria-expanded={expanded} onClick={() => setExpanded((current) => !current)} className="text-sm font-medium text-amber-400 hover:text-amber-300">{expanded ? "Thu gọn" : "Xem thêm"}</button>}</div>;
  }

  return <div className="space-y-1"><textarea required={required} value={note} onChange={(event) => onChange(event.target.value)} className="min-h-32 w-full rounded-xl border border-zinc-700 bg-zinc-800 p-3 leading-6" /><p className="text-right text-xs text-zinc-500">{note.length.toLocaleString("vi-VN")} ký tự</p></div>;
}
