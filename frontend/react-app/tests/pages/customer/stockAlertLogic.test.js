import test from "node:test";
import assert from "node:assert/strict";
import { loginRedirect, stockAlertStatus, variantText } from "../../../src/pages/customer/stockAlertLogic.js";

test("status: notified wins, then in stock, else waiting", () => {
  assert.equal(stockAlertStatus({ notifiedAt: "2026-10-28T10:00:00", inStock: true }).key, "notified");
  assert.equal(stockAlertStatus({ notifiedAt: null, inStock: true }).key, "available");
  assert.equal(stockAlertStatus({ notifiedAt: null, inStock: false }).key, "waiting");
  assert.equal(stockAlertStatus(null).key, "waiting");
});

test("variant text and login redirect back to the product", () => {
  assert.equal(variantText({ color: "Đen", size: "M" }), "Đen / M");
  assert.equal(variantText({ color: " ", size: "L" }), "L");
  assert.equal(loginRedirect("abc"), "/customerlogin?redirect=%2Fproductdetail%3Fid%3Dabc");
});
