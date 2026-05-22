export default function ValuesSection({ values }) {
  return (
    <section className="mt-16 sm:mt-20 lg:mt-24">
      <div className="rounded-3xl border border-zinc-800 bg-zinc-900/50 p-8 sm:p-10 lg:p-12">
        <div className="max-w-3xl mx-auto text-center">
          <h2 className="text-2xl font-light tracking-[0.18em] sm:text-3xl sm:tracking-[0.25em] mb-6">
            TÔN CHỈ CUA LUNARIA
          </h2>
          
          <div className="grid grid-cols-1 gap-8 md:grid-cols-3 mt-10">
            {values.map((value) => (
              <div key={value.id} className="space-y-3">
                <div className="text-4xl mb-3">{value.icon}</div>
                <h3 className="text-lg font-medium">{value.title}</h3>
                <p className="text-sm text-zinc-400 leading-relaxed">{value.description}</p>
              </div>
            ))}
          </div>

          <div className="mt-10 pt-8 border-t border-zinc-800">
            <p className="text-base text-zinc-300 leading-relaxed italic">
              "{values[0]?.quote || 'Chúng tôi không chỉ tạo ra quần áo, chúng tôi tạo ra những khoảnh khắc đáng nhớ cho mỗi phụ nữ.'}"
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
