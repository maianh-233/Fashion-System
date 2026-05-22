export default function CategoryGrid({ categories }) {
  return (
    <section className="mb-12 sm:mb-14 lg:mb-16">
      <h2 className="mb-8 text-2xl font-light tracking-[0.18em] sm:mb-10 sm:text-3xl sm:tracking-[0.25em]">
        DANH MỤC
      </h2>

      <div className="grid grid-cols-2 gap-4 sm:gap-5 md:grid-cols-4 lg:grid-cols-8 lg:gap-6">
        {categories.map((category) => (
          <button
            key={category.id}
            type="button"
            className="group text-center transition"
          >
            <div
              className="
                mx-auto flex h-16 w-16 items-center justify-center
                rounded-2xl bg-zinc-900 text-3xl text-white
                scale-95
                transition-all duration-300 ease-out
                group-hover:scale-110
                group-hover:bg-pink-500
                group-hover:text-white
                sm:h-[4.5rem] sm:w-[4.5rem] sm:text-4xl
                lg:h-20 lg:w-20 lg:rounded-3xl
              "
            >
              {category.icon}
            </div>

            <p
              className="
                mt-3 text-xs font-medium text-zinc-400
                transition-colors duration-300
                group-hover:text-pink-500
                sm:mt-4 sm:text-sm
              "
            >
              {category.label}
            </p>
          </button>
        ))}
      </div>
    </section>
  );
}