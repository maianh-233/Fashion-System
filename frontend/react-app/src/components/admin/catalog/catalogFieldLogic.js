export const MAX_CATALOG_IMAGE_BYTES = 25 * 1024 * 1024;

const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];

export function validateCatalogImage(file) {
  if (!file) return "";
  return ALLOWED_IMAGE_TYPES.includes(file.type) && file.size <= MAX_CATALOG_IMAGE_BYTES
    ? ""
    : "Ảnh phải là JPEG, PNG, GIF hoặc WebP và không quá 25 MB.";
}

export function shouldClampNote(value) {
  const note = String(value || "");
  return note.length > 500 || note.split(/\r?\n/).length > 6;
}
