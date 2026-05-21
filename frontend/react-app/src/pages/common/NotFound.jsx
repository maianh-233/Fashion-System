
export default function NotFound() {
  return (
    <div className="min-h-screen bg-zinc-950 flex items-center justify-center px-6">
      <div className="max-w-xl text-center">

        <h1 className="text-8xl font-black text-white tracking-widest">
          404
        </h1>

        <h2 className="mt-4 text-3xl font-bold text-white">
          Trang không tồn tại
        </h2>

        <p className="mt-4 text-zinc-400 leading-relaxed">
          Xin lỗi, trang bạn đang tìm kiếm không tồn tại.
        </p>

        <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">

          <button
            onClick={() => window.history.back()}
            className="inline-flex items-center justify-center gap-2 px-6 py-4 rounded-2xl bg-amber-500 hover:bg-amber-400 text-white font-semibold transition-all"
          >
            Quay lại
          </button>
        </div>
      </div>
    </div>
  );
}