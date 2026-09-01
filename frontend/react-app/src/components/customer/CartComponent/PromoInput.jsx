import Button from "../../common/Button";
import { useState } from "react";
import { ArrowRight } from "lucide-react";

export default function PromoInput({ onApply, disabled }) {
  const [code, setCode] = useState("");
  const [error, setError] = useState("");

  const handleApply = () => {
    if (!code.trim()) {
      setError("Vui lòng nhập mã");
      return;
    }

    const applied = onApply(code.trim().toUpperCase());
    if (!applied) {
      setError("Mã không hợp lệ hoặc đã được sử dụng");
      return;
    }

    setError("");
    setCode("");
  };

  return (
    <>
      <div className="cart-promo-field">
        <input
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="Mã giảm giá"
          disabled={disabled}
          className="cart-promo-input"
          onKeyDown={(event) => {
            if (event.key === "Enter") handleApply();
          }}
        />
        <Button
          variant="unstyled"
          onClick={handleApply}
          disabled={disabled}
          className="cart-promo-apply"
        >
          <span>Áp dụng</span><ArrowRight size={15} />
        </Button>
      </div>

      {error && <p className="cart-promo-message is-error">{error}</p>}
      {disabled && (
        <p className="cart-promo-message">
          Tối đa 3 mã giảm giá
        </p>
      )}
    </>
  );
}
