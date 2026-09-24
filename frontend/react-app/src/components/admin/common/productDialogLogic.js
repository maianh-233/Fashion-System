export function createProductViewDialog(product) {
  return product ? { mode: "view", product } : null;
}
