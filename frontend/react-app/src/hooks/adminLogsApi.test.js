import { describe, expect, test } from "vitest";
import { normalizeAdminLogsPage, queryString } from "./adminLogsApi";

describe("admin logs API helpers", () => {
  test("serializes only populated filters", () => {
    expect(queryString({ page: 0, size: 25, category: "", action: "LOGIN_FAILED" }))
      .toBe("?page=0&size=25&action=LOGIN_FAILED");
  });

  test("normalizes a Spring Page response", () => {
    expect(normalizeAdminLogsPage({
      content: [{ id: "1", category: "AUTH" }],
      number: 1,
      totalPages: 3,
      totalElements: 51,
    })).toEqual({
      rows: [{ id: "1", category: "AUTH" }],
      page: 2,
      totalPages: 3,
      totalElements: 51,
    });
  });
});
