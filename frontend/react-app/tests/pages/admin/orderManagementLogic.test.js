import test from "node:test";
import assert from "node:assert/strict";
import { EMPTY_FILTERS, customerLabel, listParams, nextSort, statusOptions, validateDates } from "../../../src/pages/admin/orderManagementLogic.js";

test("list params drop empty filters and convert to zero-based page", () => {
  const params = listParams({ storeId: "", type: "OFFLINE", hidden: false, filters: { ...EMPTY_FILTERS, keyword: " LB-26 " }, page: 3, size: 10, sort: { field: "createdAt", direction: "desc" } });
  assert.deepEqual(params, { storeId: undefined, type: "OFFLINE", hidden: undefined, keyword: "LB-26", status: undefined, fromDate: undefined, toDate: undefined, handlerId: undefined, paymentMethod: undefined, page: 2, size: 10, sort: "createdAt,desc" });
  assert.equal(listParams({ type: "ONLINE", hidden: true, filters: EMPTY_FILTERS, page: 1, size: 10, sort: { field: "totalAmount", direction: "asc" } }).hidden, "true");
});

test("sorting toggles the active column and starts new columns descending", () => {
  assert.deepEqual(nextSort({ field: "createdAt", direction: "desc" }, "createdAt"), { field: "createdAt", direction: "asc" });
  assert.deepEqual(nextSort({ field: "createdAt", direction: "asc" }, "totalAmount"), { field: "totalAmount", direction: "desc" });
});

test("offline orders only offer finished statuses", () => {
  assert.deepEqual(statusOptions("OFFLINE"), ["COMPLETED", "RETURNED", "CANCELLED"]);
  assert.equal(statusOptions("ONLINE").length, 8);
});

test("customer column falls back to receiver then walk-in", () => {
  assert.deepEqual(customerLabel({ customerName: "An", customerPhone: "09" }), { name: "An", phone: "09" });
  assert.deepEqual(customerLabel({ receiverName: "Bình", receiverPhone: "08" }), { name: "Bình", phone: "08" });
  assert.equal(customerLabel({}).name, "Khách vãng lai");
});

test("date range must not be reversed", () => {
  assert.equal(validateDates({ fromDate: "2026-10-14", toDate: "2026-10-01" }) !== "", true);
  assert.equal(validateDates({ fromDate: "2026-10-01", toDate: "2026-10-01" }), "");
});

test("handover button switches to retry after a failed delivery", async () => {
  const { handoverLabel } = await import("../../../src/pages/admin/orderManagementLogic.js");
  assert.equal(handoverLabel([]), "Giao cho vận chuyển");
  assert.equal(handoverLabel([{ status: "FAILED" }]), "Gửi lại yêu cầu giao hàng");
});
