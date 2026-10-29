import test from "node:test";
import assert from "node:assert/strict";
import { isChartMatch, measurementQuery, parseMeasurements, rangeLabel, recommendationLine } from "../../../src/pages/customer/sizeGuideLogic.js";

test("measurement query skips empty values", () => {
  assert.equal(measurementQuery(null, "", "?"), "");
  assert.equal(measurementQuery(165, null, "?"), "?heightCm=165");
  assert.equal(measurementQuery(165, 55.5, "&"), "&heightCm=165&weightKg=55.5");
});

test("parses measurements with comma decimals and range checks", () => {
  assert.deepEqual(parseMeasurements("165", "55,25"), { heightCm: 165, weightKg: 55.3, error: "" });
  assert.deepEqual(parseMeasurements("", ""), { heightCm: null, weightKg: null, error: "" });
  assert.match(parseMeasurements("165.5", "").error, /Chiều cao/);
  assert.match(parseMeasurements("300", "").error, /Chiều cao/);
  assert.match(parseMeasurements("", "abc").error, /Cân nặng/);
});

test("range labels cover open ends", () => {
  assert.equal(rangeLabel(160, 170, "cm"), "160 – 170 cm");
  assert.equal(rangeLabel(60, null, "kg"), "từ 60 kg");
  assert.equal(rangeLabel(null, 50.5, "kg"), "đến 50,5 kg");
  assert.equal(rangeLabel(null, null, "kg"), "—");
});

test("recommendation line names its source", () => {
  assert.equal(recommendationLine(null), null);
  assert.equal(recommendationLine({ recommendedSize: null }), null);
  assert.match(recommendationLine({ recommendedSize: "L", source: "HISTORY" }), /size L \(size bạn thường mua/);
  assert.match(recommendationLine({ recommendedSize: "M", source: "CHART" }), /theo số đo/);
  assert.equal(isChartMatch({ size: " m " }, { chartSize: "M" }), true);
  assert.equal(isChartMatch({ size: "M" }, { chartSize: null }), false);
});
