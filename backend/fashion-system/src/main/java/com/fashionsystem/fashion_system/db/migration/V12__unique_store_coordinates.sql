-- Mỗi cửa hàng vật lý phải có một cặp tọa độ riêng.
-- Kiểm tra dữ liệu trùng trước khi tạo constraint để lỗi triển khai dễ hiểu hơn.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM stores
        WHERE code IS NULL OR phone IS NULL OR latitude IS NULL OR longitude IS NULL
    ) THEN
        RAISE EXCEPTION 'Không thể siết dữ liệu stores: code, phone hoặc tọa độ đang để trống';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM stores
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        GROUP BY latitude, longitude
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Không thể tạo uq_stores_coordinates: dữ liệu cửa hàng đang trùng tọa độ';
    END IF;
END $$;

ALTER TABLE stores ALTER COLUMN code SET NOT NULL;
ALTER TABLE stores ALTER COLUMN phone SET NOT NULL;
ALTER TABLE stores ALTER COLUMN latitude SET NOT NULL;
ALTER TABLE stores ALTER COLUMN longitude SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_stores_coordinates') THEN
        ALTER TABLE stores
            ADD CONSTRAINT uq_stores_coordinates UNIQUE (latitude, longitude);
    END IF;
END $$;
