import test from "node:test";
import assert from "node:assert/strict";
import { addToCart, setQuantity, removeLine, cartRequest, toggleVoucher, cashSuggestions, money } from "../../../src/pages/admin/offlinePosLogic.js";

const shirt = { variantId: "v1", productName: "Áo", sku: "SHIRT-M", price: 300000, salePrice: null, stock: 3 };

test("scanning the same SKU again increases quantity but never above stock", () => {
  let { cart } = addToCart([], shirt);
  ({ cart } = addToCart(cart, shirt));
  assert.equal(cart.length, 1);
  assert.equal(cart[0].quantity, 2);
  const result = addToCart(cart, shirt, 5);
  assert.equal(result.cart[0].quantity, 3);
  assert.equal(result.capped, true);
});

test("quantity edits ignore invalid input and clamp to stock", () => {
  const { cart } = addToCart([], shirt);
  assert.equal(setQuantity(cart, "v1", "0")[0].quantity, 1);
  assert.equal(setQuantity(cart, "v1", "abc")[0].quantity, 1);
  assert.equal(setQuantity(cart, "v1", "10")[0].quantity, 3);
  assert.deepEqual(removeLine(cart, "v1"), []);
});

test("request carries only variant ids and quantities, never prices", () => {
  const { cart } = addToCart([], shirt);
  const body = cartRequest({ storeId: "s1", customer: { id: "c1" }, cart, promotionCodes: ["KM_A"], cashReceived: "500000", note: "  " });
  assert.deepEqual(body, { storeId: "s1", customerId: "c1", lines: [{ variantId: "v1", quantity: 1 }], promotionCodes: ["KM_A"], cashReceived: 500000, note: null });
  assert.equal(cartRequest({ cart: [], promotionCodes: [], cashReceived: "" }).cashReceived, null);
});

test("only one order-level voucher at a time, product vouchers stack", () => {
  const vouchers = [{ code: "O1", type: "ORDER" }, { code: "O2", type: "POINT_REDEMPTION" }, { code: "P1", type: "PRODUCT" }];
  let selected = toggleVoucher([], vouchers[0], vouchers);
  selected = toggleVoucher(selected, vouchers[2], vouchers);
  selected = toggleVoucher(selected, vouchers[1], vouchers);
  assert.deepEqual(selected, ["P1", "O2"]);
  assert.deepEqual(toggleVoucher(selected, vouchers[2], vouchers), ["O2"]);
});

test("cash suggestions start at the exact total and round up", () => {
  assert.deepEqual(cashSuggestions(938000), [938000, 940000, 950000, 1000000]);
  assert.deepEqual(cashSuggestions(0), []);
  assert.equal(money(1234567.4), "1.234.567đ");
});
