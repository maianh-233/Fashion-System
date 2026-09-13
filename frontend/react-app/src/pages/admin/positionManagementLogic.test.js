import test from "node:test";
import assert from "node:assert/strict";
import {
  formatSalaryRange,
  hasHierarchyChange,
  isHierarchyConfirmationRequired,
  toCreatePositionPayload,
  toUpdatePositionPayload,
  validatePositionForm,
} from "./positionManagementLogic.js";

const validForm = {
  departmentId: "dep-1",
  code: "LEAD",
  name: "Lead",
  description: "Team lead",
  active: true,
  hierarchyLevel: "3",
  minSalary: "12000000",
  maxSalary: "18000000",
};

test("formats VND salary range", () => {
  assert.equal(formatSalaryRange(12000000, 18000000), "12.000.000 ₫ – 18.000.000 ₫");
});

test("validates hierarchy and salary values before submission", () => {
  assert.deepEqual(
    validatePositionForm({ ...validForm, hierarchyLevel: "2.5", minSalary: "-1", maxSalary: "4.2" }),
    {
      hierarchyLevel: "Cấp bậc phải là số nguyên dương.",
      minSalary: "Lương tối thiểu phải là số nguyên không âm.",
      maxSalary: "Lương tối đa phải là số nguyên không âm.",
    },
  );
  assert.deepEqual(
    validatePositionForm({ ...validForm, minSalary: "18000001", maxSalary: "18000000" }),
    { maxSalary: "Lương tối đa phải lớn hơn hoặc bằng lương tối thiểu." },
  );
});

test("create payload includes normalized position code and integer hierarchy salary values", () => {
  assert.deepEqual(
    toCreatePositionPayload({ ...validForm, code: " LEAD ", name: " Lead ", description: " " }),
    {
      departmentId: "dep-1",
      code: "LEAD",
      name: "Lead",
      description: null,
      active: true,
      hierarchyLevel: 3,
      minSalary: 12000000,
      maxSalary: 18000000,
    },
  );
});

test("update payload never sends position code", () => {
  const payload = toUpdatePositionPayload({
    code: "LEAD", departmentId: "dep-1", name: "Lead", active: true,
    hierarchyLevel: "3", minSalary: "12000000", maxSalary: "18000000",
  });
  assert.equal(Object.hasOwn(payload, "code"), false);
  assert.deepEqual(payload, {
    departmentId: "dep-1",
    name: "Lead",
    description: null,
    active: true,
    hierarchyLevel: 3,
    minSalary: 12000000,
    maxSalary: 18000000,
  });
});

test("detects a changed department or hierarchy level", () => {
  assert.equal(hasHierarchyChange(validForm, { ...validForm, hierarchyLevel: "4" }), true);
  assert.equal(hasHierarchyChange(validForm, { ...validForm, departmentId: "dep-2" }), true);
  assert.equal(hasHierarchyChange(validForm, { ...validForm, hierarchyLevel: 3 }), false);
});

test("recognizes hierarchy confirmation from structured conflict data only", () => {
  assert.equal(isHierarchyConfirmationRequired({ status: 409, data: { code: "HIERARCHY_CONFIRMATION_REQUIRED" } }), true);
  assert.equal(isHierarchyConfirmationRequired({ status: 409, code: "HIERARCHY_CONFIRMATION_REQUIRED" }), false);
  assert.equal(isHierarchyConfirmationRequired({ status: 400, data: { code: "HIERARCHY_CONFIRMATION_REQUIRED" } }), false);
});
