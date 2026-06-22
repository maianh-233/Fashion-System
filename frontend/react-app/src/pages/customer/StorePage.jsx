import { useState } from "react";

const MOCK_STORES = [
  {
    id: 1,
    name: "Lunar Fashion Store Q1",
    address: "123 Nguyễn Trãi, Quận 1, TP.HCM",
    image: "https://picsum.photos/300/200?random=1",
    distance: "1.2 km",
  },
  {
    id: 2,
    name: "Lunar Fashion Store Q3",
    address: "45 Lê Văn Sỹ, Quận 3, TP.HCM",
    image: "https://picsum.photos/300/200?random=2",
    distance: "2.8 km",
  },
  {
    id: 3,
    name: "Lunar Fashion Store Bình Thạnh",
    address: "88 Xô Viết Nghệ Tĩnh, Bình Thạnh",
    image: "https://picsum.photos/300/200?random=3",
    distance: "3.5 km",
  },
    {
    id: 1,
    name: "Lunar Fashion Store Q1",
    address: "123 Nguyễn Trãi, Quận 1, TP.HCM",
    image: "https://picsum.photos/300/200?random=1",
    distance: "1.2 km",
  },
  {
    id: 2,
    name: "Lunar Fashion Store Q3",
    address: "45 Lê Văn Sỹ, Quận 3, TP.HCM",
    image: "https://picsum.photos/300/200?random=2",
    distance: "2.8 km",
  },
  {
    id: 3,
    name: "Lunar Fashion Store Bình Thạnh",
    address: "88 Xô Viết Nghệ Tĩnh, Bình Thạnh",
    image: "https://picsum.photos/300/200?random=3",
    distance: "3.5 km",
  },
    {
    id: 1,
    name: "Lunar Fashion Store Q1",
    address: "123 Nguyễn Trãi, Quận 1, TP.HCM",
    image: "https://picsum.photos/300/200?random=1",
    distance: "1.2 km",
  },
  {
    id: 2,
    name: "Lunar Fashion Store Q3",
    address: "45 Lê Văn Sỹ, Quận 3, TP.HCM",
    image: "https://picsum.photos/300/200?random=2",
    distance: "2.8 km",
  },
  {
    id: 3,
    name: "Lunar Fashion Store Bình Thạnh",
    address: "88 Xô Viết Nghệ Tĩnh, Bình Thạnh",
    image: "https://picsum.photos/300/200?random=3",
    distance: "3.5 km",
  },
];

export default function StorePage() {
  const [search, setSearch] = useState("");
  const [location, setLocation] = useState("Cà Sóp, TP.HCM");

  const filteredStores = MOCK_STORES.filter((s) =>
    s.name.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div className="w-full h-full bg-zinc-950 text-zinc-200 flex flex-col">
      {/* ================= HEADER ================= */}
      <header className="border-b border-zinc-800 bg-zinc-900 sticky top-0 z-40">
        <div className="px-4 sm:px-8 py-4 flex flex-col gap-3">
          {/* SEARCH */}
          <div className="flex-1 relative">
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm kiếm cửa hàng..."
              className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 pl-11 focus:outline-none focus:border-amber-400"
            />
            <i className="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" />
          </div>

          {/* LOCATION INPUT */}
          <div className="flex gap-2 items-center">
            <input
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Nhập vị trí của bạn..."
              className="flex-1 bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-2 focus:outline-none focus:border-amber-400"
            />
            <button className="px-4 py-2 rounded-xl bg-amber-500 text-black font-semibold hover:bg-amber-400">
              Tìm
            </button>
          </div>
        </div>
      </header>

      {/* ================= MAIN ================= */}
      <main className="flex-1 overflow-y-auto p-4 sm:p-8 min-h-0 flex flex-col space-y-6">
        {/* MAP FRAME */}
        <section className="w-full h-[300px] sm:h-[400px] rounded-2xl overflow-hidden border border-zinc-800 relative bg-zinc-900">
          <img
            src="https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1"
            className="w-full h-full object-cover opacity-60"
            alt="map"
          />

          {/* fake pins */}
          <div className="absolute top-10 left-10 bg-red-500 w-4 h-4 rounded-full animate-pulse"></div>
          <div className="absolute top-20 left-1/2 bg-red-500 w-4 h-4 rounded-full animate-pulse"></div>
          <div className="absolute bottom-20 right-20 bg-red-500 w-4 h-4 rounded-full animate-pulse"></div>

          <div className="absolute bottom-3 left-3 text-xs bg-black/60 px-2 py-1 rounded">
            Bản đồ giả lập (Google Maps tích hợp sau)
          </div>
        </section>

        {/* STORE LIST */}
        <section className="flex gap-4 overflow-x-auto pb-2 scroll-smooth [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {filteredStores.map((store) => (
            <div
              key={store.id}
              className="min-w-[300px] sm:min-w-[340px] bg-zinc-900 border border-zinc-800 rounded-2xl overflow-hidden hover:border-amber-400 transition flex-shrink-0"
            >
              <img
                src={store.image}
                className="w-full h-40 object-cover"
                alt={store.name}
              />

              <div className="p-4 space-y-2">
                <h3 className="font-semibold text-lg">{store.name}</h3>
                <p className="text-sm text-zinc-400">{store.address}</p>

                <div className="flex justify-between items-center pt-2">
                  <span className="text-xs text-emerald-400">
                    {store.distance} từ bạn
                  </span>

                  <button className="text-xs px-3 py-1 rounded-lg bg-zinc-800 hover:bg-zinc-700">
                    Xem chi tiết
                  </button>
                </div>
              </div>
            </div>
          ))}
        </section>
      </main>
    </div>
  );
}
