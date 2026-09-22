export function buildCatalogQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

export function toCatalogOptions(items = []) {
  return items.map((item) => ({
    value: item.id,
    label: item.slug ? `${item.name} · ${item.slug}` : item.name,
  }));
}
