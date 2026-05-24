import { Search } from "lucide-react";

export default function SearchBar({ value, onChange, placeholder }) {
  return (
    <div className="relative w-full">
      <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />

      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder || "Tìm kiếm..."}
        className="w-full bg-zinc-800 border border-zinc-700 rounded-xl py-2 pl-10 pr-4
                   focus:outline-none focus:border-amber-400 text-sm"
      />
    </div>
  );
}