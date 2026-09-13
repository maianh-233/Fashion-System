import test from "node:test";
import assert from "node:assert/strict";
import {
  buildDepartmentUpdateBody,
} from "./departmentManagementLogic.js";

test("buildDepartmentUpdateBody keeps the original department code", () => {
  assert.deepEqual(
    buildDepartmentUpdateBody(
      { code: "HACKED", name: " Kinh doanh ", description: " ", active: true },
      "SALES",
    ),
    { code: "SALES", name: "Kinh doanh", description: null, active: true },
  );
});
