import test from "node:test";
import assert from "node:assert/strict";
import {
  compactMoney, changeVsPrevious, dayLabel, longDate, greeting, yearSummary, variantLabel,
} from "../../../src/pages/admin/dashboardLogic.js";

test("compact money switches unit at a million and a billion", () => {
  assert.equal(compactMoney(950000), "950.000");
  assert.equal(compactMoney(685400000), "685,4 tr");
  assert.equal(compactMoney(8_240_000_000), "8,2 tỷ");
  assert.equal(compactMoney(null), "0");
});

test("change line compares with the same period last month", () => {
  assert.deepEqual(changeVsPrevious(118.2, 100), { text: "↑ 18,2% so với cùng kỳ tháng trước", tone: "up" });
  assert.deepEqual(changeVsPrevious(90, 100), { text: "↓ 10% so với cùng kỳ tháng trước", tone: "down" });
  assert.equal(changeVsPrevious(0, 0).tone, "flat");
  assert.equal(changeVsPrevious("5", 0).text, "Tháng trước chưa phát sinh");
});

test("dates are read as calendar days, not shifted by time zone", () => {
  assert.equal(dayLabel("2026-10-25"), "CN 25/10");
  assert.equal(dayLabel("2026-10-19"), "T2 19/10");
  assert.equal(longDate("2026-10-05"), "05 tháng 10, 2026");
  assert.equal(longDate(undefined), "");
});

test("greeting follows the hour", () => {
  assert.equal(greeting(8), "Chào buổi sáng");
  assert.equal(greeting(12), "Chào buổi trưa");
  assert.equal(greeting(15), "Chào buổi chiều");
  assert.equal(greeting(21), "Chào buổi tối");
});

test("year summary reports growth and best year, without dividing by zero", () => {
  const summary = yearSummary([
    { label: "2024", revenue: 0 }, { label: "2025", revenue: "200" }, { label: "2026", revenue: "150" },
  ]);
  assert.deepEqual(summary, { year: "2026", revenue: 150, growth: -25, bestYear: "2025" });
  assert.equal(yearSummary([{ label: "2025", revenue: 0 }, { label: "2026", revenue: 10 }]).growth, null);
  assert.equal(yearSummary([{ label: "2026", revenue: 0 }]).bestYear, null);
  assert.equal(yearSummary([]), null);
});

test("variant label skips missing options", () => {
  assert.equal(variantLabel({ productName: "Áo", color: "Trắng", size: "M" }), "Áo · Trắng / M");
  assert.equal(variantLabel({ productName: "Túi", color: null, size: "" }), "Túi");
});
