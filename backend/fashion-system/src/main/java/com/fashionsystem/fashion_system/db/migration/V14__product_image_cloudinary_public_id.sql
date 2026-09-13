-- Store the provider-owned identifier needed for safe Cloudinary replacement and deletion.
ALTER TABLE product_images
    ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_product_images_cloudinary_public_id
    ON product_images(cloudinary_public_id);
