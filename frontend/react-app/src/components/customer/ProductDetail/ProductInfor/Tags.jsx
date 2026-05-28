const tags = [
  "NewArrival",
  "Oversize",
  "Summer2026",
  "Cotton",
  "Streetwear",
];

export default function Tags() {
  return (
    <div className="mt-8">
      <h3 className="font-medium mb-3 text-gray-300">Tags</h3>

      <div className="flex flex-wrap gap-2">
        {tags.map((tag) => (
          <span
            key={tag}
            className="inline-block bg-gray-900 hover:bg-gray-800
                       px-5 py-2 rounded-3xl text-sm
                       text-[#FFCC00] cursor-pointer"
          >
            #{tag}
          </span>
        ))}
      </div>
    </div>
  );
}