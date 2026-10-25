import test from "node:test";
import assert from "node:assert/strict";
import {
  statusGroups, shares, seriesLabel, monthLabel, changeVsPreviousPeriod, rateChange, rangeLabel, buildCsv,
  reportSections, paymentLabel,
} from "../../../src/pages/admin/statisticsLogic.js";

test("status counts collapse into pie groups and drop empty ones", () => {
  assert.deepEqual(statusGroups({ COMPLETED: 5, PENDING: 1, SHIPPING: 2, CANCELLED: 0 }), [
    { name: "Hoàn thành", color: "#f59e0b", value: 5 },
    { name: "Đang xử lý", color: "#38bdf8", value: 3 },
  ]);
  assert.deepEqual(statusGroups(undefined), []);
});

test("shares compute order percentages and labels", () => {
  const rows = shares([{ key: "CASH", orders: 3, revenue: "300" }, { key: "OTHER", orders: 1, revenue: 0 }], paymentLabel);
  assert.deepEqual(rows.map(row => [row.label, row.percent, row.revenue]), [["Tiền mặt", 75, 300], ["Khác", 25, 0]]);
  assert.equal(shares([{ key: "X", orders: 0 }])[0].percent, 0);
});

test("axis and month labels follow granularity", () => {
  assert.equal(seriesLabel("2026-10-05", "DAY"), "05/10");
  assert.equal(seriesLabel("2026-10", "MONTH"), "10/2026");
  assert.equal(monthLabel("2026-05"), "T5/2026");
});

test("period comparison wording and negative baselines", () => {
  assert.equal(changeVsPreviousPeriod(120, 100).text, "↑ 20% so với kỳ trước");
  assert.equal(changeVsPreviousPeriod(10, 0).text, "Kỳ trước chưa phát sinh");
  // Lợi nhuận kỳ trước âm: tăng lên vẫn là tăng.
  assert.equal(changeVsPreviousPeriod(50, -100).tone, "up");
});

test("retention change is shown in percentage points", () => {
  assert.deepEqual(rateChange(43.8, 40.4), { text: "+3,4 điểm % so với kỳ trước", tone: "up" });
  assert.equal(rateChange(null, 10), null);
  assert.equal(rateChange(10, 10).tone, "flat");
});

test("range label shows the last included day", () => {
  assert.equal(rangeLabel("2026-10-01", "2026-11-01"), "01/10/2026 – 31/10/2026");
  assert.equal(rangeLabel(null, "2026-11-01"), "");
});

test("csv has BOM, quotes special cells and separates sections", () => {
  const csv = buildCsv([
    { title: "A", headers: ["Tên", "Số"], rows: [['Áo "Silk", trắng', 1]] },
    { headers: ["X"], rows: [[null]] },
  ]);
  assert.equal(csv, String.fromCharCode(0xfeff) + 'A\r\nTên,Số\r\n"Áo ""Silk"", trắng",1\r\n\r\nX\r\n');
});

test("report sections cover every table of the page", () => {
  const report = {
    from: "2026-10-19", to: "2026-10-26", storeName: null, orderType: "ONLINE", granularity: "DAY",
    overview: { revenue: "10", previousRevenue: "5", orders: 1, previousOrders: 1, grossProfit: "3", previousGrossProfit: "1", newCustomers: 0, previousNewCustomers: 0 },
    retention: { rate: null, previousRate: 50 },
    revenueSeries: [{ label: "2026-10-25", revenue: "10", orders: 1 }],
    topProducts: [], lowStock: [], vipCustomers: [],
    inventory: { importedQuantity: 1, exportedQuantity: 2, stockQuantity: 3, stockValue: "4", unsoldVariants: 5 },
  };
  const sections = reportSections(report);
  assert.equal(sections[0].title, "Báo cáo thống kê 19/10/2026 – 25/10/2026 · Toàn hệ thống · Đơn online");
  assert.deepEqual(sections[0].rows[4], ["Tỷ lệ khách quay lại (%)", "", 50]);
  assert.deepEqual(sections.map(section => section.title).slice(1), [
    "Doanh thu theo thời gian", "Top sản phẩm bán chạy", "Sản phẩm sắp hết hàng", "Top khách VIP", "Kho",
  ]);
});
