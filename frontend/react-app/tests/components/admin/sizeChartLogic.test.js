import test from "node:test";
import assert from "node:assert/strict";
import { SIZE_CHART_PRESET, chartPayload, emptyChartRow, rowsFromChart, validateChartRows } from "../../../src/components/admin/catalog/sizeChartLogic.js";

const row = (size, minH, maxH, minW = "", maxW = "") => ({ ...emptyChartRow(), size, minHeightCm: minH, maxHeightCm: maxH, minWeightKg: minW, maxWeightKg: maxW });

test("payload trims sizes and turns blanks into null", () => {
  assert.deepEqual(chartPayload([row(" M ", "160", "", "", "55,5")]).entries,
    [{ size: "M", minHeightCm: 160, maxHeightCm: null, minWeightKg: null, maxWeightKg: 55.5 }]);
  assert.deepEqual(rowsFromChart({ entries: [{ size: "S", minHeightCm: 150, maxHeightCm: null, minWeightKg: 40, maxWeightKg: null }] })
    .map((row) => { const copy = { ...row }; delete copy.key; return copy; }), [{ size: "S", minHeightCm: "150", maxHeightCm: "", minWeightKg: "40", maxWeightKg: "" }]);
});

test("validation catches duplicates, empty ranges and inverted ranges", () => {
  assert.equal(validateChartRows(SIZE_CHART_PRESET), null);
  assert.equal(validateChartRows([]), null);
  assert.match(validateChartRows([row("", "160", "170")]), /tên size/);
  assert.match(validateChartRows([row("M", "160", "170"), row("m", "170", "180")]), /trùng/);
  assert.match(validateChartRows([row("M", "", "")]), /ít nhất một khoảng/);
  assert.match(validateChartRows([row("M", "170", "160")]), /nhỏ hơn hoặc bằng/);
  assert.match(validateChartRows([row("M", "60", "170")]), /80 đến 250/);
});
