import assert from "node:assert/strict";
import test from "node:test";
import { dueGroup, emptyTaskForm, groupByDue, taskPayload, toDateTimeInput, validateTaskForm } from "../src/pages/admin/task/taskLogic.js";

const now = new Date(2026, 10, 9, 10, 0);

test("dueGroup sorts a task by its deadline", () => {
  assert.equal(dueGroup({ dueAt: null }, now), "NONE");
  assert.equal(dueGroup({ dueAt: "2026-11-08T10:00:00", overdue: true }, now), "OVERDUE");
  assert.equal(dueGroup({ dueAt: "2026-11-09T18:00:00", overdue: false }, now), "TODAY");
  assert.equal(dueGroup({ dueAt: "2026-11-12T08:00:00", overdue: false }, now), "UPCOMING");
});

test("groupByDue keeps group order and drops empty groups", () => {
  const groups = groupByDue([
    { id: 1, dueAt: null },
    { id: 2, dueAt: "2026-11-08T10:00:00", overdue: true },
    { id: 3, dueAt: null },
  ], now);
  assert.deepEqual(groups.map((g) => g.key), ["OVERDUE", "NONE"]);
  assert.deepEqual(groups[1].tasks.map((t) => t.id), [1, 3]);
});

test("validateTaskForm requires assignees only when creating", () => {
  const form = { ...emptyTaskForm(), title: "Kiểm kho" };
  assert.match(validateTaskForm(form, { creating: true }), /người nhận/);
  assert.equal(validateTaskForm(form, { creating: false }), "");
  assert.match(validateTaskForm({ ...form, title: "  " }, { creating: false }), /tên công việc/);
  assert.match(validateTaskForm({ ...form, checklist: [{ content: " " }] }, { creating: false }), /Việc con/);
});

test("taskPayload trims text and only sends assignees when creating", () => {
  const form = { ...emptyTaskForm(), assigneeIds: ["a"], title: " Việc ", description: " ", dueAt: "", checklist: [{ id: "x", content: " Bước " }, { content: "Mới" }] };
  assert.deepEqual(taskPayload(form, { creating: true }), {
    title: "Việc", description: null, priority: "NORMAL", dueAt: null, assigneeIds: ["a"],
    checklist: [{ id: "x", content: "Bước" }, { id: null, content: "Mới" }],
  });
  assert.equal("assigneeIds" in taskPayload(form, { creating: false }), false);
});

test("toDateTimeInput drops seconds for the datetime input", () => {
  assert.equal(toDateTimeInput("2026-11-10T17:30:00"), "2026-11-10T17:30");
  assert.equal(toDateTimeInput(null), "");
});
