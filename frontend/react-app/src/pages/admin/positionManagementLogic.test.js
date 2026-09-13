import test from "node:test";
import assert from "node:assert/strict";
import * as positionLogic from "./positionManagementLogic.js";
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

test("edit validation does not require immutable code, create still does", () => {
  assert.deepEqual(validatePositionForm({ ...validForm, code: "" }, { mode: "edit" }), {});
  assert.ok(validatePositionForm({ ...validForm, code: "" }).code);
});

test("zero salary bounds are valid and displayed explicitly", () => {
  assert.deepEqual(validatePositionForm({ ...validForm, minSalary: 0, maxSalary: 0 }), {});
  assert.equal(formatSalaryRange(0, 0), "0 ₫ – 0 ₫");
});

test("create conflicts become the shared duplicate-code notification message", () => {
  assert.equal(positionLogic.positionErrorMessage({ status: 409, message: "Conflict" }, { mode: "create" }), "Mã vị trí đã tồn tại");
  assert.equal(positionLogic.positionErrorMessage({ status: 403, message: "Không có quyền" }, { mode: "create" }), "Không có quyền");
  assert.equal(positionLogic.positionErrorMessage({ status: 409, message: "Other conflict" }, { mode: "edit" }), "Other conflict");
});

const impact = { affectedRelationCount: 2, affectedEmployeeCount: 3, summary: "Impact" };
const raceError = { status: 409, data: { code: "HIERARCHY_CONFIRMATION_REQUIRED", ...impact } };

function updateScenario({ changed = true, preview = impact, accepted = true, failures = [] } = {}) {
  const calls = [];
  const original = { ...validForm, id: "position-1" };
  const form = { ...validForm, hierarchyLevel: changed ? "4" : "3" };
  return {
    calls,
    original,
    form,
    run: () => positionLogic.updatePositionWithConfirmation({
      original, form,
      api: {
        hierarchyImpact: async (id, params) => { calls.push({ type: "preview", id, params }); return preview; },
        update: async (id, payload) => {
          calls.push({ type: "update", id, payload });
          const failure = failures.shift();
          if (failure) throw failure;
        },
      },
      confirm: async (options) => { calls.push({ type: "confirm", options }); return accepted; },
    }),
  };
}

test("unchanged hierarchy updates directly without preview or reset", async () => {
  const scenario = updateScenario({ changed: false });
  assert.equal(await scenario.run(), true);
  assert.equal(scenario.calls.length, 1);
  assert.equal(scenario.calls[0].type, "update");
  assert.equal(Object.hasOwn(scenario.calls[0].payload, "code"), false);
  assert.equal(Object.hasOwn(scenario.calls[0].payload, "resetInvalidRelations"), false);
});

test("changed hierarchy previews exact department and level before updating", async () => {
  const scenario = updateScenario({ preview: { affectedRelationCount: 0, affectedEmployeeCount: 0 } });
  assert.equal(await scenario.run(), true);
  assert.deepEqual(scenario.calls.map((call) => call.type), ["preview", "update"]);
  assert.deepEqual(scenario.calls[0], { type: "preview", id: "position-1", params: { departmentId: "dep-1", hierarchyLevel: 4 } });
  assert.equal(Object.hasOwn(scenario.calls[1].payload, "resetInvalidRelations"), false);
});

test("cancelled impact confirmation never updates or mutates the form", async () => {
  const scenario = updateScenario({ accepted: false });
  const before = structuredClone(scenario.form);
  assert.equal(await scenario.run(), false);
  assert.deepEqual(scenario.calls.map((call) => call.type), ["preview", "confirm"]);
  assert.deepEqual(scenario.form, before);
  assert.match(scenario.calls[1].options.message, /2 quan hệ/);
  assert.match(scenario.calls[1].options.message, /3 nhân viên/);
  assert.match(scenario.calls[1].options.message, /quản lý.*cấp dưới.*gỡ bỏ/);
});

test("accepted impact confirmation sets reset only on the outgoing payload", async () => {
  const scenario = updateScenario();
  assert.equal(await scenario.run(), true);
  assert.deepEqual(scenario.calls.map((call) => call.type), ["preview", "confirm", "update"]);
  assert.equal(scenario.calls[2].payload.resetInvalidRelations, true);
  assert.equal(Object.hasOwn(scenario.form, "resetInvalidRelations"), false);
});

test("raced conflict uses response counts then retries once with reset", async () => {
  const scenario = updateScenario({ preview: { affectedRelationCount: 0, affectedEmployeeCount: 0 }, failures: [raceError] });
  assert.equal(await scenario.run(), true);
  assert.deepEqual(scenario.calls.map((call) => call.type), ["preview", "update", "confirm", "update"]);
  assert.match(scenario.calls[2].options.message, /2 quan hệ/);
  assert.match(scenario.calls[2].options.message, /3 nhân viên/);
  assert.equal(scenario.calls[3].payload.resetInvalidRelations, true);
});

test("cancelling raced conflict leaves the retry unsent", async () => {
  const scenario = updateScenario({ changed: false, accepted: false, failures: [raceError] });
  assert.equal(await scenario.run(), false);
  assert.deepEqual(scenario.calls.map((call) => call.type), ["update", "confirm"]);
});

test("a repeated raced conflict is propagated without a second confirmation or retry", async () => {
  const scenario = updateScenario({ changed: false, failures: [raceError, raceError] });
  await assert.rejects(scenario.run(), (error) => error === raceError);
  assert.deepEqual(scenario.calls.map((call) => call.type), ["update", "confirm", "update"]);
});

test("unrelated update failures do not enter hierarchy confirmation", async () => {
  const failure = { status: 500, message: "Failed" };
  const scenario = updateScenario({ changed: false, failures: [failure] });
  await assert.rejects(scenario.run(), (error) => error === failure);
  assert.deepEqual(scenario.calls.map((call) => call.type), ["update"]);
});

test("a department-only edit also previews its proposed hierarchy", async () => {
  const scenario = updateScenario({ changed: false, accepted: false });
  scenario.form.departmentId = "dep-2";
  assert.equal(await scenario.run(), false);
  assert.deepEqual(scenario.calls[0].params, { departmentId: "dep-2", hierarchyLevel: 3 });
  assert.deepEqual(scenario.calls.map((call) => call.type), ["preview", "confirm"]);
});

test("failed preview cannot fall through to an update", async () => {
  const failure = new Error("Preview unavailable");
  await assert.rejects(positionLogic.updatePositionWithConfirmation({
    original: { ...validForm, id: "position-1" },
    form: { ...validForm, hierarchyLevel: 4 },
    api: {
      hierarchyImpact: async () => { throw failure; },
      update: async () => assert.fail("Must not update after a failed preview"),
    },
    confirm: async () => assert.fail("Must not confirm after a failed preview"),
  }), (error) => error === failure);
});
