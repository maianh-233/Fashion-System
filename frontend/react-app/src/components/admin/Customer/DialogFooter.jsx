export default function DialogFooter({ mode, onClose }) {
  if (mode !== "view") return null;

  return (
    <div className="px-8 py-6 border-t border-zinc-700 bg-zinc-950 
                    flex items-center justify-between rounded-b-3xl">

      <button
        onClick={onClose}
        className="px-5 py-2 bg-blue-600 rounded-xl">
        Đóng
      </button>
    </div>
  );
}