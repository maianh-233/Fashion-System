export function buildDepartmentUpdateBody(form, originalCode) {
  return {
    code: originalCode,
    name: form.name.trim(),
    description: form.description?.trim() || null,
    active: Boolean(form.active),
  };
}
