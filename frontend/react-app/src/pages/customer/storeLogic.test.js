import test from "node:test";
import assert from "node:assert/strict";
import {
  STORE_FALLBACK_IMAGE, directionsUrl, escapeHtml, filterStores, safeImageUrl, sortByDistance, storeArea, telHref, toMapStores,
} from "./storeLogic.js";

const stores = toMapStores([
  { id: "a", name: "Fashion Store Quận 7", address: "Nguyễn Thị Thập, Quận 7, TP. Hồ Chí Minh", latitude: 10.738, longitude: 106.701 },
  { id: "b", name: "Fashion Store Cầu Giấy", address: "Cầu Giấy, Hà Nội", latitude: 21.0362, longitude: 105.7906 },
  { id: "c", name: "Không tọa độ", address: "Đà Nẵng", latitude: null, longitude: null },
]);

test("map stores need coordinates and get an area", () => {
  assert.deepEqual(stores.map((s) => s.id), ["a", "b"]);
  assert.deepEqual(stores[0].coordinates, [10.738, 106.701]);
  assert.equal(stores[1].area, "Hà Nội");
  assert.equal(storeArea(""), "");
});

test("search ignores accents", () => {
  assert.deepEqual(filterStores(stores, "ha noi").map((s) => s.id), ["b"]);
  assert.deepEqual(filterStores(stores, "  quan 7").map((s) => s.id), ["a"]);
  assert.equal(filterStores(stores, "").length, 2);
});

test("nearest first when location is known", () => {
  const sorted = sortByDistance(stores, [21.03, 105.85]);
  assert.equal(sorted[0].id, "b");
  assert.ok(sorted[0].distance < sorted[1].distance);
  assert.equal(sortByDistance(stores, null), stores);
});

test("popup content is escaped and links are safe", () => {
  assert.equal(escapeHtml(`<img src=x onerror="a">&'`), "&lt;img src=x onerror=&quot;a&quot;&gt;&amp;&#39;");
  assert.equal(safeImageUrl("javascript:alert(1)"), STORE_FALLBACK_IMAGE);
  assert.equal(safeImageUrl("https://res.cloudinary.com/x.jpg"), "https://res.cloudinary.com/x.jpg");
  assert.equal(directionsUrl("10.5", 106), "https://www.google.com/maps/dir/?api=1&destination=10.5,106");
  assert.equal(telHref("028 3822-8899"), "tel:02838228899");
});
