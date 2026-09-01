import { motion, AnimatePresence } from "framer-motion";

export default function ProductLayout({
  header,
  sidebar,
  content,
  openFilter,
  setOpenFilter,
}) {
  return (
    <div className="customer-page customer-catalog-page flex flex-col min-h-screen bg-[#0b0f14] text-gray-100">
      <div className="customer-page__wide">{header}</div>

      <div className="customer-page__wide flex flex-1 items-start">
        {/* Sidebar desktop */}
        <aside className="customer-filter-panel sticky top-24 hidden w-64 shrink-0 self-start lg:flex">
          {sidebar}
        </aside>

        {/* Content */}
        <main className="customer-catalog-content min-w-0 flex-1 p-4 sm:p-6 lg:p-7">
          {content}
        </main>
      </div>

      {/* ================= MOBILE DRAWER ================= */}
      <AnimatePresence>
        {openFilter && (
          <>
            <motion.div
              className="customer-filter-overlay fixed bg-black/50 z-40 lg:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setOpenFilter(false)}
            />

            <motion.aside
              className="customer-filter-panel product-filter-panel lg:hidden"
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "tween", duration: 0.25 }}
              aria-modal="true"
              role="dialog"
              aria-label="Bộ lọc sản phẩm"
            >
              {sidebar}
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
