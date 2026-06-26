export default function StoreBasicInfoForm({ data, onChange, isView }) {
  return (
    <div className="space-y-5">

      <div>
        <label className="text-sm text-gray-400">Tên cửa hàng *</label>
        <input
          id="name"
          value={data.name}
          onChange={onChange}
          disabled={isView}
          className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
        />
      </div>

      <div>
        <label className="text-sm text-gray-400">Mã cửa hàng *</label>
        <input
          id="code"
          value={data.code}
          onChange={onChange}
          disabled={isView}
          className="uppercase w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
        />
      </div>

      <div>
        <label className="text-sm text-gray-400">Số điện thoại</label>
        <input
          id="phone"
          value={data.phone}
          onChange={onChange}
          disabled={isView}
          className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
        />
      </div>

      <div>
        <label className="text-sm text-gray-400">Địa chỉ</label>
        <textarea
          id="address"
          rows="2"
          value={data.address}
          onChange={onChange}
          disabled={isView}
          className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="text-sm text-gray-400">Kinh độ</label>
          <input
            id="longitude"
            value={data.longitude}
            onChange={onChange}
            disabled={isView}
            className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
          />
        </div>

        <div>
          <label className="text-sm text-gray-400">Vĩ độ</label>
          <input
            id="latitude"
            value={data.latitude}
            onChange={onChange}
            disabled={isView}
            className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3"
          />
        </div>
      </div>

    </div>
  );
}