import { motion, AnimatePresence } from "framer-motion";
export default function ProductLayout({
  header,
  sidebar,
  content,
  openFilter,
  setOpenFilter,
}) {
  return (
    <div className="flex flex-col min-h-screen">
      {header}

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar desktop */}
        <aside className="hidden lg:flex w-72 border-r bg-zinc-900">
          {sidebar}
        </aside>

        {/* Content */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          {content}
        </main>
      </div>

      {/* ================= MOBILE DRAWER ================= */}
      <AnimatePresence>
        {openFilter && (
          <>
            {/* overlay */}
            <motion.div
              className="fixed inset-0 bg-black/50 z-40 lg:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setOpenFilter(false)}
            />

            {/* drawer */}
            <motion.div
              className="fixed top-0 left-0 h-full w-80 bg-zinc-900 z-50 lg:hidden shadow-xl"
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "tween", duration: 0.25 }}
            >
              <div className="flex items-center justify-between p-4 border-b border-zinc-700">
                <h2 className="text-white font-bold">Bộ lọc</h2>

                <button
                  onClick={() => setOpenFilter(false)}
                  className="text-white text-xl"
                >
                  ✕
                </button>
              </div>

              <div className="p-4 overflow-y-auto h-full">
                {sidebar}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}