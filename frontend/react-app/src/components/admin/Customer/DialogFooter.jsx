// DialogFooter.jsx
export default function DialogFooter({ mode, onClose }) {
  if (mode !== "view") return null;

  return (
    <div className="bg-zinc-950 px-8 py-5 border-t flex justify-end">
      <button
        onClick={onClose}
        className="text-zinc-400 hover:text-white"
      >
        Đóng
      </button>
    </div>
  );
}