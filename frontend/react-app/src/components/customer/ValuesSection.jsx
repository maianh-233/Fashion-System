export default function ValuesSection({ values }) {
  return (
    <section className="mt-12 sm:mt-16 lg:mt-24 px-4 sm:px-0">
      <div className="rounded-2xl sm:rounded-3xl border border-zinc-800 bg-zinc-900/50 p-6 sm:p-10 lg:p-12">
        
        <div className="max-w-3xl mx-auto text-center">
          
          {/* ===== TITLE ===== */}
          <h2 className="text-lg sm:text-2xl font-light tracking-[0.18em] sm:tracking-[0.25em] mb-4 sm:mb-6">
            TÔN CHỈ CỦA LUNARIA
          </h2>

          {/* ===== VALUES GRID ===== */}
          <div className="grid grid-cols-1 gap-6 sm:gap-8 md:grid-cols-3 mt-8 sm:mt-10">
            {values.map((value) => (
              <div
                key={value.id}
                className="space-y-2 sm:space-y-3 px-2 sm:px-0"
              >
                <div className="text-3xl sm:text-4xl mb-2 sm:mb-3">
                  {value.icon}
                </div>

                <h3 className="text-base sm:text-lg font-medium">
                  {value.title}
                </h3>

                <p className="text-sm text-zinc-400 leading-relaxed">
                  {value.description}
                </p>
              </div>
            ))}
          </div>

          {/* ===== QUOTE ===== */}
          <div className="mt-8 sm:mt-10 pt-6 sm:pt-8 border-t border-zinc-800">
            <p className="text-sm sm:text-base text-zinc-300 leading-relaxed italic px-2 sm:px-0">
              “{values[0]?.quote ||
                "Chúng tôi không chỉ tạo ra quần áo, chúng tôi tạo ra những khoảnh khắc đáng nhớ cho mỗi phụ nữ."}”
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}