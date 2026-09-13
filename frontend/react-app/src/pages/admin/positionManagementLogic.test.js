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

function employeeScenario({ changes = {}, preview = impact, accepted = true, failures = [] } = {}) {
  const calls = [];
  const original = { id: "employee-1", departmentId: "dep-1", positionId: "position-1" };
  const form = { departmentId: "dep-1", positionId: "position-1", fullName: "An", ...changes };
  return { calls, form, run: () => positionLogic.updateEmployeeWithConfirmation({
    original, form,
    api: {
      hierarchyImpact: async (id, params) => { calls.push({ type: "preview", id, params }); return preview; },
      update: async (id, payload) => { calls.push({ type: "update", id, payload }); const error = failures.shift(); if (error) throw error; },
    },
    confirm: async (options) => { calls.push({ type: "confirm", options }); return accepted; },
  }) };
}

test("employee ordinary edits skip preview and never trust a supplied reset flag", async () => {
  const scenario = employeeScenario({ changes: { resetInvalidRelations: true } });
  assert.equal(await scenario.run(), true);
  assert.deepEqual(scenario.calls, [{ type: "update", id: "employee-1", payload: { departmentId: "dep-1", positionId: "position-1", fullName: "An" } }]);
});

for (const changes of [{ departmentId: "dep-2" }, { positionId: "position-2" }]) {
  test(`employee organization change ${Object.keys(changes)[0]} previews before saving without zero-impact consent`, async () => {
    const scenario = employeeScenario({ changes, preview: { affectedRelationCount: 0, affectedEmployeeCount: 0 } });
    assert.equal(await scenario.run(), true);
    assert.deepEqual(scenario.calls.map(call => call.type), ["preview", "update"]);
    assert.deepEqual(scenario.calls[0].params, { departmentId: "dep-1", positionId: "position-1", ...changes });
    assert.equal(Object.hasOwn(scenario.calls[1].payload, "resetInvalidRelations"), false);
  });
}

test("employee impact cancellation preserves form and sends no mutation", async () => {
  const scenario = employeeScenario({ changes: { positionId: "position-2" }, accepted: false });
  const before = structuredClone(scenario.form);
  assert.equal(await scenario.run(), false);
  assert.deepEqual(scenario.calls.map(call => call.type), ["preview", "confirm"]);
  assert.match(scenario.calls[1].options.message, /2 quan hệ.*3 nhân viên/);
  assert.deepEqual(scenario.form, before);
});

test("employee accepted impact sends reset only after consent", async () => {
  const scenario = employeeScenario({ changes: { positionId: "position-2" } });
  assert.equal(await scenario.run(), true);
  assert.deepEqual(scenario.calls.map(call => call.type), ["preview", "confirm", "update"]);
  assert.equal(scenario.calls[2].payload.resetInvalidRelations, true);
  assert.equal(Object.hasOwn(scenario.form, "resetInvalidRelations"), false);
});

test("employee raced conflict confirms current response counts and retries once", async () => {
  const scenario = employeeScenario({ changes: { positionId: "position-2" }, preview: { affectedRelationCount: 0 }, failures: [raceError] });
  assert.equal(await scenario.run(), true);
  assert.deepEqual(scenario.calls.map(call => call.type), ["preview", "update", "confirm", "update"]);
  assert.match(scenario.calls[2].options.message, /2 quan hệ.*3 nhân viên/);
  assert.equal(scenario.calls[3].payload.resetInvalidRelations, true);
});

test("employee raced cancellation sends no retry", async () => {
  const scenario = employeeScenario({ accepted: false, failures: [raceError] });
  assert.equal(await scenario.run(), false);
  assert.deepEqual(scenario.calls.map(call => call.type), ["update", "confirm"]);
});

test("employee second conflict and unrelated errors propagate without loops", async () => {
  const scenario = employeeScenario({ failures: [raceError, raceError] });
  await assert.rejects(scenario.run(), error => error === raceError);
  assert.deepEqual(scenario.calls.map(call => call.type), ["update", "confirm", "update"]);
  const failure = { status: 409, data: { code: "OTHER" } };
  const unrelated = employeeScenario({ failures: [failure] });
  await assert.rejects(unrelated.run(), error => error === failure);
  assert.deepEqual(unrelated.calls.map(call => call.type), ["update"]);
});

test("employee preview failure prevents mutation", async () => {
  const failure = new Error("Preview failed");
  await assert.rejects(positionLogic.updateEmployeeWithConfirmation({
    original: { id: "e", departmentId: "d", positionId: "p" }, form: { departmentId: "d", positionId: "new" },
    api: { hierarchyImpact: async () => { throw failure; }, update: async () => assert.fail("No update") },
    confirm: async () => assert.fail("No confirmation"),
  }), error => error === failure);
});

test("admin promotion with null organization uses transactional conflict consent because preview requires UUIDs", async () => {
  const scenario = employeeScenario({ changes: { departmentId: null, positionId: null, roleCodes: ["ADMIN"] }, failures: [raceError] });
  assert.equal(await scenario.run(), true);
  assert.deepEqual(scenario.calls.map(call => call.type), ["update", "confirm", "update"]);
  assert.equal(Object.hasOwn(scenario.calls[0].payload, "resetInvalidRelations"), false);
  assert.deepEqual(scenario.calls[2].payload, { departmentId: null, positionId: null, roleCodes: ["ADMIN"], fullName: "An", resetInvalidRelations: true });
});

test("employee refresh returns fresh detail, catalogs and both subordinate lists while reloading the employee table", async () => {
  const calls = [];
  const result = await positionLogic.refreshEmployeeData({ id: "e", reloadEmployees: async () => calls.push("employees"),
    api: { detail: async id => { assert.equal(id, "e"); return { id, employmentType: "FULL_TIME", positionId: "p", managerId: null }; },
      subordinates: async id => { assert.equal(id, "e"); calls.push("subordinates"); return []; },
      eligibleSubordinates: async id => { assert.equal(id, "e"); calls.push("eligible"); return [{ id: "released" }]; } },
    reloadCatalogs: async () => { calls.push("catalogs"); return { positions: [{ id: "p" }], departments: [] }; },
  });
  assert.deepEqual(calls.sort(), ["catalogs", "eligible", "employees", "subordinates"]);
  assert.deepEqual(result, { employee: { id: "e", employmentType: "FULL_TIME", positionId: "p", managerId: null }, catalogs: { positions: [{ id: "p" }], departments: [] }, subordinateData: { items: [], candidates: [{ id: "released" }] } });
});

test("ineligible managers and read-only viewers load current subordinates without requesting restricted candidates", async () => {
  for (const canAssign of [false]) {
    assert.deepEqual(await positionLogic.loadSubordinateLists({ id: "e", canAssign, api: {
      subordinates: async () => [{ id: "child" }], eligibleSubordinates: async () => assert.fail("No eligible request"),
    } }), { items: [{ id: "child" }], candidates: [] });
  }
});

for (const operation of ["add", "remove"]) {
  test(`subordinate ${operation} requires consent then reloads current and eligible lists`, async () => {
    const calls = [];
    const candidate = { id: "candidate", fullName: "Binh", email: "binh@example.com" };
    const result = await positionLogic.changeSubordinate({ operation, employeeId: "manager", candidate,
      confirm: async () => { calls.push("confirm"); return true; },
      api: {
        assignSubordinate: async (id, email) => { assert.equal(id, "manager"); assert.equal(email, "binh@example.com"); calls.push("add"); },
        removeSubordinate: async (id, childId) => { assert.equal(id, "manager"); assert.equal(childId, "candidate"); calls.push("remove"); },
      }, reload: async () => { calls.push("reload-both"); },
    });
    assert.equal(result, true);
    assert.deepEqual(calls, ["confirm", operation, "reload-both"]);
  });
}

test("cancelled subordinate assignment performs no mutation or refresh", async () => {
  assert.equal(await positionLogic.changeSubordinate({ operation: "add", employeeId: "e", candidate: { fullName: "Binh" },
    confirm: async () => false, api: { assignSubordinate: async () => assert.fail("No assignment") }, reload: async () => assert.fail("No refresh"),
  }), false);
});
