function text(value) {
  return String(value ?? "").trim();
}

function isInteger(value) {
  return text(value) !== "" && Number.isSafeInteger(Number(value));
}

function isPositiveInteger(value) {
  return isInteger(value) && Number(value) > 0;
}

function isNonNegativeInteger(value) {
  return isInteger(value) && Number(value) >= 0;
}

function normalizedPositionFields(form) {
  return {
    departmentId: form.departmentId,
    name: text(form.name),
    description: text(form.description) || null,
    active: Boolean(form.active),
    hierarchyLevel: Number(form.hierarchyLevel),
    minSalary: Number(form.minSalary),
    maxSalary: Number(form.maxSalary),
  };
}

export function validatePositionForm(form, { mode = "create" } = {}) {
  const errors = {};
  if (!text(form.departmentId)) errors.departmentId = "Vui lòng chọn phòng ban.";
  if (mode === "create" && !text(form.code)) errors.code = "Mã vị trí là bắt buộc.";
  if (!text(form.name)) errors.name = "Tên vị trí là bắt buộc.";
  if (!isPositiveInteger(form.hierarchyLevel)) errors.hierarchyLevel = "Cấp bậc phải là số nguyên dương.";
  if (!isNonNegativeInteger(form.minSalary)) errors.minSalary = "Lương tối thiểu phải là số nguyên không âm.";
  if (!isNonNegativeInteger(form.maxSalary)) errors.maxSalary = "Lương tối đa phải là số nguyên không âm.";
  if (!errors.minSalary && !errors.maxSalary && Number(form.maxSalary) < Number(form.minSalary)) {
    errors.maxSalary = "Lương tối đa phải lớn hơn hoặc bằng lương tối thiểu.";
  }
  return errors;
}

export function toCreatePositionPayload(form) {
  return {
    ...normalizedPositionFields(form),
    code: text(form.code),
  };
}

export function toUpdatePositionPayload(form) {
  const payload = normalizedPositionFields(form);
  if (form.resetInvalidRelations !== undefined) payload.resetInvalidRelations = Boolean(form.resetInvalidRelations);
  return payload;
}

export function hasHierarchyChange(current, proposed) {
  return current.departmentId !== proposed.departmentId
    || Number(current.hierarchyLevel) !== Number(proposed.hierarchyLevel);
}

export function formatSalaryRange(minSalary, maxSalary) {
  const format = new Intl.NumberFormat("vi-VN");
  return `${format.format(minSalary)} ₫ – ${format.format(maxSalary)} ₫`;
}

export function isHierarchyConfirmationRequired(error) {
  return error?.status === 409 && error.data?.code === "HIERARCHY_CONFIRMATION_REQUIRED";
}

export function positionErrorMessage(error, { mode } = {}) {
  if (mode === "create" && error?.status === 409 && !isHierarchyConfirmationRequired(error)) {
    return "Mã vị trí đã tồn tại";
  }
  return error?.message || "Không thể hoàn tất thao tác vị trí.";
}

function hierarchyConfirmation(impact) {
  return {
    title: "Xác nhận thay đổi phân cấp",
    message: `Thay đổi ảnh hưởng đến ${impact.affectedRelationCount} quan hệ và ${impact.affectedEmployeeCount} nhân viên. Các liên kết quản lý/cấp dưới không hợp lệ sẽ được gỡ bỏ. Bạn có muốn tiếp tục?`,
    confirmText: "Gỡ liên kết và lưu",
    cancelText: "Hủy",
    destructive: true,
  };
}

export async function updatePositionWithConfirmation({ original, form, api, confirm }) {
  const payload = toUpdatePositionPayload(form);
  if (hasHierarchyChange(original, form)) {
    const impact = await api.hierarchyImpact(original.id, {
      departmentId: payload.departmentId,
      hierarchyLevel: payload.hierarchyLevel,
    });
    if (impact.affectedRelationCount > 0) {
      if (!await confirm(hierarchyConfirmation(impact))) return false;
      payload.resetInvalidRelations = true;
    }
  }
  try {
    await api.update(original.id, payload);
  } catch (error) {
    if (!isHierarchyConfirmationRequired(error)) throw error;
    if (!await confirm(hierarchyConfirmation(error.data))) return false;
    // A second conflict propagates to the caller; never loop confirmations.
    await api.update(original.id, { ...payload, resetInvalidRelations: true });
  }
  return true;
}
