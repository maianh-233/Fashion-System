
export default function CollectionHero({ collection }) {
  return (
    <section
      className="relative min-h-[70vh] md:min-h-screen flex items-center"
      style={{
        backgroundImage: `
          linear-gradient(rgba(0,0,0,0.55), rgba(0,0,0,0.65)),
          url(${collection.banner})
        `,
        backgroundSize: "cover",
        backgroundPosition: "center",
        backgroundAttachment: "fixed",
      }}
    >
      <div className="max-w-7xl mx-auto px-4 md:px-6 py-24 text-white w-full">
        <div className="max-w-2xl">

          {/* Season */}
          <p className="text-amber-400 tracking-[3px] text-xs md:text-sm mb-3">
            {collection.season.toUpperCase()} {collection.year}
          </p>

          {/* Brand */}
          <div className="flex items-center gap-4 mb-6">
            <img
              src={collection.brand_logo}
              alt="Brand Logo"
              className="h-10 w-10 md:h-12 md:w-12 object-contain"
            />
            <p className="text-xl md:text-2xl font-light tracking-wider">
              {collection.brand_name}
            </p>
          </div>

          {/* Collection name */}
          <h1 className="text-4xl md:text-6xl lg:text-7xl font-serif font-bold mb-6 leading-tight">
            {collection.name}
          </h1>

          {/* Code */}
          <p className="text-amber-300 mb-4">
            Code: <span className="font-mono tracking-widest">{collection.code}</span>
          </p>

          {/* Description */}
          <p className="text-base md:text-lg text-gray-200 max-w-lg mb-6">
            {collection.description}
          </p>

          {/* Release date */}
          <p className="text-sm text-gray-300">
            Ngày ra mắt:{" "}
            {new Date(collection.release_date).toLocaleDateString("vi-VN")}
          </p>
        </div>
      </div>

      {/* Gradient bottom */}
      <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-black/80 to-transparent" />
    </section>
  );
}