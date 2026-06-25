// CustomerAvatar.jsx
export default function CustomerAvatar({ customer }) {
  return (
    <div className="flex-shrink-0 flex flex-col items-center">
      <img
        src={customer?.avatar || "https://picsum.photos/200"}
        className="w-40 h-40 rounded-3xl object-cover border-4 border-[#FFB300]"
      />

      <div className="mt-4 text-2xl font-semibold text-center">
        {customer?.name || "Khách hàng mới"}
      </div>

      {customer?.tier && (
        <div className="mt-2 px-5 py-1.5 bg-[#FFB300] rounded-3xl text-black font-medium">
          🥇 {customer.tier}
        </div>
      )}
    </div>
  );
}