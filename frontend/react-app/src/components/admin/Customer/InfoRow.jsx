// InfoRow.jsx
export default function InfoRow({ label, value, big }) {
  return (
    <div>
      <div className="text-zinc-400 text-xs tracking-widest">{label}</div>
      <div className={big ? "text-3xl font-semibold" : "font-medium"}>
        {value || "-"}
      </div>
    </div>
  );
}