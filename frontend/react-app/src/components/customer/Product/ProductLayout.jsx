import Button from "../../common/Button";
import { motion, AnimatePresence } from "framer-motion";

export default function ProductLayout({
  header,
  sidebar,
  content,
  openFilter,
  setOpenFilter,
}) {
  return (
    <div className="flex flex-col min-h-screen bg-[#0b0f14] text-gray-100">
      {header}

      <div className="flex flex-1 items-start">
        {/* Sidebar desktop */}
        <aside className="sticky top-20 hidden w-72 shrink-0 self-start border-r border-white/10 bg-zinc-900 lg:flex">
          {sidebar}
        </aside>

        {/* Content */}
        <main className="min-w-0 flex-1 bg-[#0b0f14] p-4 sm:p-6 lg:p-8">
          {content}
        </main>
      </div>

      {/* ================= MOBILE DRAWER ================= */}
      <AnimatePresence>
        {openFilter && (
          <>
            <motion.div
              className="fixed inset-0 bg-black/50 z-40 lg:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setOpenFilter(false)}
            />

            <motion.div
              className="fixed top-0 left-0 h-full w-[min(88vw,20rem)] bg-zinc-900 z-50 lg:hidden shadow-xl"
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "tween", duration: 0.25 }}
            >
              <div className="flex items-center justify-between p-4 border-b border-zinc-700">
                <h2 className="text-white font-bold">Bộ lọc</h2>

                <Button
                  onClick={() => setOpenFilter(false)}
                  className="text-white text-xl"
                >
                  ✕
                </Button>
              </div>

              <div className="h-[calc(100%-4.5rem)] overflow-y-auto p-4 pb-8">
                {sidebar}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
