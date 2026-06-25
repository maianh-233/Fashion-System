// DialogOverlay.jsx
export default function DialogOverlay({ children }) {
  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center">
      {children}
    </div>
  );
}