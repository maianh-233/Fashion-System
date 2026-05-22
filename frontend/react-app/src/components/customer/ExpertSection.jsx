function ExpertCard({ expert }) {
  return (
    <article className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 sm:p-8">
      <div className="flex items-start gap-4">
        <div className="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-amber-400 text-xl font-bold text-black">
          {expert.avatar}
        </div>
        <div className="flex-1">
          <p className="text-sm text-zinc-400 italic leading-relaxed mb-4">"{expert.quote}"</p>
          <div>
            <p className="text-sm font-medium">{expert.name}</p>
            <p className="text-xs text-zinc-400">{expert.title}</p>
          </div>
        </div>
      </div>
    </article>
  );
}

export default function ExpertSection({ experts }) {
  return (
    <section className="mt-16 sm:mt-20 lg:mt-24">
      <h2 className="mb-8 text-2xl font-light tracking-[0.18em] sm:mb-10 sm:text-3xl sm:tracking-[0.25em]">
        CHUYÊN GIA NÓI GÌ VỀ CHÚNG TÔI
      </h2>
      
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {experts.map((expert) => (
          <ExpertCard key={expert.id} expert={expert} />
        ))}
      </div>
    </section>
  );
}
