import { useState } from "react";
import { CircleHelp, Lightbulb, X } from "lucide-react";
import { PAGE_GUIDES } from "./pageGuides";

const key = (id) => `lunaria.guide.${id}`;

function isHidden(id) {
  try {
    return window.localStorage.getItem(key(id)) === "hidden";
  } catch {
    return false;
  }
}

/**
 * Hướng dẫn nhanh cho người mới: các bước làm việc chính của trang. Lần đầu tự mở; "Đã hiểu" thu gọn thành nút
 * "Hướng dẫn" (nhớ theo trình duyệt) để mở lại bất cứ lúc nào.
 */
export default function PageGuide({ id }) {
  const guide = PAGE_GUIDES[id];
  const [open, setOpen] = useState(() => !isHidden(id));
  if (!guide) return null;

  const close = () => {
    setOpen(false);
    try {
      window.localStorage.setItem(key(id), "hidden");
    } catch {
      // Không lưu được: chỉ ẩn trong phiên này.
    }
  };

  if (!open) {
    return (
      <div className="flex justify-end">
        <button type="button" onClick={() => setOpen(true)}
          className="inline-flex items-center gap-1.5 rounded-full border border-zinc-700 px-3 py-1 text-xs text-zinc-400 hover:border-amber-400/60 hover:text-amber-300">
          <CircleHelp size={14} aria-hidden="true" />Hướng dẫn: {guide.title}
        </button>
      </div>
    );
  }

  return (
    <section className="rounded-3xl border border-amber-400/30 bg-amber-400/[0.06] p-4 sm:p-5" aria-label={`Hướng dẫn: ${guide.title}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="grid h-8 w-8 place-items-center rounded-xl bg-amber-400/15 text-amber-300"><CircleHelp size={17} aria-hidden="true" /></span>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wider text-amber-300">Hướng dẫn nhanh</p>
            <h2 className="text-sm font-semibold text-zinc-100">{guide.title}</h2>
          </div>
        </div>
        <button type="button" onClick={close} className="rounded-lg p-1 text-zinc-500 hover:text-zinc-200" aria-label="Ẩn hướng dẫn">
          <X size={16} />
        </button>
      </div>
      <ol className={`mt-4 grid gap-3 ${guide.steps.length >= 4 ? "md:grid-cols-2 xl:grid-cols-4" : "md:grid-cols-3"}`}>
        {guide.steps.map((step, i) => (
          <li key={step.title} className="flex gap-3 rounded-2xl border border-zinc-800 bg-zinc-900 p-3">
            <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-amber-400 text-xs font-bold text-zinc-950">{i + 1}</span>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-zinc-100">{step.title}</p>
              <p className="mt-0.5 text-xs leading-relaxed text-zinc-400">{step.text}</p>
            </div>
          </li>
        ))}
      </ol>
      {guide.tips?.length > 0 && (
        <ul className="mt-3 space-y-1">
          {guide.tips.map((tip) => (
            <li key={tip} className="flex items-start gap-2 text-xs text-zinc-400"><Lightbulb size={13} className="mt-0.5 shrink-0 text-amber-300" aria-hidden="true" />{tip}</li>
          ))}
        </ul>
      )}
      <div className="mt-3 flex justify-end">
        <button type="button" onClick={close} className="rounded-xl bg-amber-400 px-3 py-1.5 text-xs font-semibold text-zinc-950 hover:bg-amber-300">Đã hiểu, ẩn đi</button>
      </div>
    </section>
  );
}
