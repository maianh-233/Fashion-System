export default function ProductBasicInfo({ mode, product }) {
  const readOnly = mode === "view";

  return (
    <section>
      <h3 className="text-lg font-semibold mb-4 text-orange-400">
        Thông tin cơ bản
      </h3>

      <div className="grid grid-cols-2 gap-4">
        <Input label="Tên sản phẩm" value={product?.name} readOnly={readOnly} />
        <Input label="Slug" value={product?.slug} readOnly={readOnly} />
        <Input label="Chất liệu" value={product?.material} readOnly={readOnly} />
        <Input label="Form dáng" value={product?.fit} readOnly={readOnly} />

        <Select label="Giới tính" disabled={readOnly} />
        <Select label="Trạng thái" disabled={readOnly} />

        <Select label="Brand" disabled={readOnly} />
        <Select label="Collection" disabled={readOnly} />
        <Select label="Category" disabled={readOnly} />

        <Textarea
          label="Mô tả"
          value={product?.description}
          readOnly={readOnly}
          className="col-span-2"
        />
      </div>
    </section>
  );
}

function Input({ label, readOnly, value }) {
  return (
    <div>
      <label className="text-sm text-gray-400">{label}</label>
      <input
        readOnly={readOnly}
        defaultValue={value}
        className="w-full mt-1 px-3 py-2 bg-[#1e1e1e] border border-gray-700 rounded"
      />
    </div>
  );
}

function Select({ label, disabled }) {
  return (
    <div>
      <label className="text-sm text-gray-400">{label}</label>
      <select
        disabled={disabled}
        className="w-full mt-1 px-3 py-2 bg-[#1e1e1e] border border-gray-700 rounded"
      />
    </div>
  );
}

function Textarea({ label, readOnly, value, className }) {
  return (
    <div className={className}>
      <label className="text-sm text-gray-400">{label}</label>
      <textarea
        readOnly={readOnly}
        defaultValue={value}
        rows={4}
        className="w-full mt-1 px-3 py-2 bg-[#1e1e1e] border border-gray-700 rounded"
      />
    </div>
  );
}