import assert from "node:assert/strict";
import test from "node:test";
import {
  budgetUsage, campaignPayload, campaignToForm, describeAudience, dispatchPayload, dispatchProgress, dispatchToForm,
  emptyCampaignForm, emptyDispatchForm, emptyExpenseForm, expensePayload, formatMoney, isCustomerPath, offerLabel,
  validateCampaignForm, validateDispatchForm, validateExpenseForm, defaultCalendarRange, formatPercent, targetProgress,
  timelineBar, weekTicks,
} from "../src/pages/admin/marketing/marketingLogic.js";

test("targetProgress and formatPercent handle missing targets", () => {
  assert.equal(targetProgress(700, 1000), 70);
  assert.equal(targetProgress(1500, 1000), 150);
  assert.equal(targetProgress(10, null), null);
  assert.equal(targetProgress(10, 0), null);
  assert.equal(formatPercent(1, 4), "25%");
  assert.equal(formatPercent(1, 0), "—");
});

test("timelineBar clips to the range and keeps a minimum width", () => {
  // Khoảng 10 ngày: 01 → 10/12.
  assert.deepEqual(timelineBar("2026-12-01T00:00:00", "2026-12-06T00:00:00", "2026-12-01", "2026-12-10"), { left: 0, width: 50 });
  const clipped = timelineBar("2026-11-20T00:00:00", "2027-01-05T00:00:00", "2026-12-01", "2026-12-10");
  assert.deepEqual(clipped, { left: 0, width: 100 });
  const point = timelineBar("2026-12-10T23:00:00", "2026-12-10T23:00:00", "2026-12-01", "2026-12-10");
  assert.equal(point.width, 1);
  assert.ok(point.left + point.width <= 100);
});

test("calendar range and week ticks", () => {
  assert.deepEqual(defaultCalendarRange(new Date(2026, 10, 10)), { from: "2026-11-01", to: "2026-12-31" });
  assert.deepEqual(weekTicks("2026-11-01", "2026-11-17"), ["2026-11-02", "2026-11-09", "2026-11-16"]);
});

const now = new Date(2026, 11, 1, 9, 0);
const validDispatch = () => ({ ...emptyDispatchForm(), title: "Sale đã mở", message: "Giảm 20%", scheduledAt: "2026-12-02T09:00" });

test("validateDispatchForm checks content, link, inactive days and time", () => {
  assert.equal(validateDispatchForm(validDispatch(), now), "");
  assert.match(validateDispatchForm({ ...validDispatch(), title: "ab" }, now), /Tiêu đề/);
  assert.match(validateDispatchForm({ ...validDispatch(), message: " " }, now), /nội dung/);
  assert.match(validateDispatchForm({ ...validDispatch(), actionUrl: "/admin/orders" }, now), /đường dẫn nội bộ/);
  assert.match(validateDispatchForm({ ...validDispatch(), actionUrl: "https://evil.example" }, now), /đường dẫn nội bộ/);
  assert.match(validateDispatchForm({ ...validDispatch(), inactiveDays: "0" }, now), /1 đến 1095/);
  assert.match(validateDispatchForm({ ...validDispatch(), scheduledAt: "2026-11-30T09:00" }, now), /sau thời điểm hiện tại/);
});

test("dispatch payload and round trip keep criteria", () => {
  const body = dispatchPayload({ ...validDispatch(), tierIds: ["t1"], inactiveDays: "90", actionUrl: " /promotions " });
  assert.deepEqual(body.criteria, { tierIds: ["t1"], inactiveDays: 90, categoryId: null });
  assert.equal(body.actionUrl, "/promotions");
  assert.equal(body.channel, "IN_APP");
  const form = dispatchToForm({ title: "A", message: "B", actionUrl: null, scheduledAt: "2026-12-02T09:00:00",
    audience: { tierIds: ["t1"], inactiveDays: null, categoryId: "c1" } });
  assert.deepEqual([form.tierIds, form.inactiveDays, form.categoryId, form.scheduledAt], [["t1"], "", "c1", "2026-12-02T09:00"]);
});

test("describeAudience, progress and customer paths", () => {
  assert.equal(describeAudience({ tierNames: [], inactiveDays: null, categoryName: null }), "Mọi khách đang hoạt động");
  assert.equal(describeAudience({ tierNames: ["Vàng", "Bạc"], inactiveDays: 90, categoryName: "Áo" }), "Hạng Vàng, Bạc · chưa mua 90 ngày · từng mua Áo");
  assert.equal(dispatchProgress({ audienceCount: null }), null);
  assert.equal(dispatchProgress({ audienceCount: 4, sent: 2, skipped: 1, failed: 0 }), 75);
  assert.equal(isCustomerPath(""), true);
  assert.equal(isCustomerPath("//evil"), false);
  assert.equal(isCustomerPath("/vouchers?tab=mine"), true);
});

const validForm = () => ({ ...emptyCampaignForm(), name: "Sale 12.12", startsAt: "2026-12-10T08:00", endsAt: "2026-12-13T22:00" });

test("validateCampaignForm checks name, window, length and numbers", () => {
  assert.equal(validateCampaignForm(validForm()), "");
  assert.match(validateCampaignForm({ ...validForm(), name: "ab" }), /ít nhất 3 ký tự/);
  assert.match(validateCampaignForm({ ...validForm(), endsAt: "2026-12-09T08:00" }), /kết thúc phải sau/);
  assert.match(validateCampaignForm({ ...validForm(), endsAt: "2028-01-10T08:00" }), /tối đa 366 ngày/);
  assert.match(validateCampaignForm({ ...validForm(), endsAt: "" }), /thời gian bắt đầu và kết thúc/);
  assert.match(validateCampaignForm({ ...validForm(), plannedBudget: "-1" }), /không được âm/);
  assert.match(validateCampaignForm({ ...validForm(), targetOrders: "1.5" }), /số nguyên/);
});

test("campaignPayload trims text and turns empty numbers into null", () => {
  const body = campaignPayload({ ...validForm(), name: "  Sale  ", description: "  ", plannedBudget: "5000000", targetOrders: "" });
  assert.equal(body.name, "Sale");
  assert.equal(body.description, null);
  assert.equal(body.ownerId, null);
  assert.equal(body.plannedBudget, 5000000);
  assert.equal(body.targetOrders, null);
});

test("campaignToForm round-trips an API campaign", () => {
  const form = campaignToForm({
    name: "A", objective: "RETENTION", description: null, startsAt: "2026-12-10T08:00:00", endsAt: "2026-12-13T22:00:00",
    ownerId: "u1", plannedBudget: 1000, targetRevenue: null, targetOrders: 0, targetBuyers: null,
  });
  assert.equal(form.startsAt, "2026-12-10T08:00");
  assert.equal(form.plannedBudget, "1000");
  assert.equal(form.targetRevenue, "");
  assert.equal(form.targetOrders, "0");
});

test("expense form needs a positive amount and a date", () => {
  const form = { ...emptyExpenseForm(), amount: "250000" };
  assert.equal(validateExpenseForm(form), "");
  assert.match(validateExpenseForm({ ...form, amount: "0" }), /lớn hơn 0/);
  assert.match(validateExpenseForm({ ...form, category: "TIKTOK" }), /loại chi phí/);
  assert.match(validateExpenseForm({ ...form, spentOn: "" }), /ngày chi/);
  assert.deepEqual(expensePayload({ ...form, vendor: " Meta ", note: "" }), {
    category: "ADS", amount: 250000, spentOn: form.spentOn, vendor: "Meta", note: null,
  });
});

test("budgetUsage and labels", () => {
  assert.equal(budgetUsage(6_000_000, 5_000_000), 120);
  assert.equal(budgetUsage(0, null), null);
  assert.equal(budgetUsage(100, 0), null);
  assert.equal(formatMoney(null), "—");
  assert.equal(offerLabel({ code: "KM10", name: "Giảm 10%" }), "KM10 – Giảm 10%");
  assert.equal(offerLabel({ code: null, name: "Flash 12h" }), "Flash 12h");
});

test("phase 4: channel, holdout and store scope in forms", () => {
  assert.match(validateDispatchForm({ ...validDispatch(), holdoutPercent: "60" }, now), /0 đến 50%/);
  assert.match(validateDispatchForm({ ...validDispatch(), channel: "SMS" }, now), /kênh gửi/);
  const body = dispatchPayload({ ...validDispatch(), channel: "EMAIL", holdoutPercent: "20" });
  assert.equal(body.channel, "EMAIL");
  assert.equal(body.holdoutPercent, 20);
  assert.equal(dispatchToForm({ title: "A", message: "B", scheduledAt: "2026-12-02T09:00:00", channel: "EMAIL", holdoutPercent: 10, audience: {} }).holdoutPercent, "10");
  assert.equal(campaignPayload({ ...validForm(), storeId: "" }).storeId, null);
  assert.equal(campaignPayload({ ...validForm(), storeId: "s1" }).storeId, "s1");
  assert.equal(campaignToForm({ name: "A", objective: "REVENUE", startsAt: "2026-12-10T08:00:00", endsAt: "2026-12-13T22:00:00", storeId: "s1" }).storeId, "s1");
  assert.equal(dispatchProgress({ audienceCount: 10, sent: 4, skipped: 1, failed: 0, holdout: 5 }), 100);
});
