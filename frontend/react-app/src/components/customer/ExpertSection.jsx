function ExpertCard({ expert }) {
  return (
    <article className="rounded-2xl sm:rounded-3xl border border-zinc-800 bg-zinc-900 p-4 sm:p-6">
      <div className="flex items-start gap-3 sm:gap-4">
        
        {/* AVATAR */}
        <div className="flex h-10 w-10 sm:h-12 sm:w-12 flex-shrink-0 items-center justify-center rounded-full bg-amber-400 text-base sm:text-xl font-bold text-black">
          {expert.avatar}
        </div>

        {/* CONTENT */}
        <div className="flex-1">
          <p className="text-sm sm:text-sm text-zinc-400 italic leading-relaxed mb-3 sm:mb-4 line-clamp-4">
            “{expert.quote}”
          </p>

          <div>
            <p className="text-sm font-medium leading-tight">
              {expert.name}
            </p>
            <p className="text-xs text-zinc-400">
              {expert.title}
            </p>
          </div>
        </div>
      </div>
    </article>
  );
}

export default function ExpertSection({ experts }) {
  return (
    <section className="mt-12 sm:mt-16 lg:mt-24 px-4 sm:px-0">
      
      {/* TITLE */}
      <h2 className="mb-6 sm:mb-8 text-lg sm:text-2xl font-light tracking-[0.18em] sm:tracking-[0.25em] text-center sm:text-left">
        CHUYÊN GIA NÓI GÌ VỀ CHÚNG TÔI
      </h2>

      {/* GRID */}
      <div className="grid grid-cols-1 gap-4 sm:gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {experts.map((expert) => (
          <ExpertCard key={expert.id} expert={expert} />
        ))}
      </div>
    </section>
  );
}