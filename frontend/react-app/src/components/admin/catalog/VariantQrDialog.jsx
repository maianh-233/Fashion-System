import { useEffect, useState } from "react";
import QRCode from "qrcode";
import Button from "../../common/Button";

export default function VariantQrDialog({ variant, onClose }) {
  const [image, setImage] = useState("");
  const [error, setError] = useState("");
  useEffect(() => {
    QRCode.toDataURL(variant.barcode || variant.sku, {
      errorCorrectionLevel: "H", width: 512, margin: 2,
    }).then(setImage).catch(() => setError("Không thể tạo mã QR."));
  }, [variant.barcode, variant.sku]);

  return <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true">
    <style>{`@media print { body * { visibility: hidden !important; } .variant-qr-label, .variant-qr-label * { visibility: visible !important; } .variant-qr-label { position: fixed; left: 0; top: 0; width: 50mm; padding: 3mm; color: black !important; background: white !important; } .variant-qr-controls { display: none !important; } }`}</style>
    <section className="w-full max-w-sm rounded-2xl bg-white p-6 text-zinc-900">
      <div className="variant-qr-label text-center">
        <h2 className="text-base font-semibold">{variant.productName || "Sản phẩm"}</h2>
        <p className="text-sm">{variant.productCode || ""} · {variant.color || "—"} / {variant.size || "—"}</p>
        {image && <img src={image} alt={`QR ${variant.sku}`} className="mx-auto my-3 h-48 w-48 object-contain" />}
        {error && <p className="text-red-600">{error}</p>}
        <p className="font-mono text-sm font-bold">{variant.sku}</p>
        <p className="text-sm">{Number(variant.salePrice ?? variant.price).toLocaleString("vi-VN")} ₫</p>
      </div>
      <div className="variant-qr-controls mt-5 flex justify-end gap-3">
        <Button type="button" onClick={onClose}>Đóng</Button>
        <Button type="button" disabled={!image} onClick={() => window.print()}>In QR</Button>
      </div>
    </section>
  </div>;
}
