export default function Price() {
  return (
    <div className="flex items-center gap-4 mt-6">
      <span className="text-4xl font-bold text-[#FFCC00]">450,000 ₫</span>
      <span className="text-2xl text-gray-500 line-through">650,000 ₫</span>
      <span className="bg-[#FFCC00]/20 text-[#FFCC00] px-4 py-1.5 rounded-2xl text-sm">
        -31%
      </span>
    </div>
  );
}