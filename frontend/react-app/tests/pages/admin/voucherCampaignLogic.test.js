import test from "node:test";
import assert from "node:assert/strict";
import {
  audienceText, benefitText, campaignPayload, emptyCampaignForm, usageText, validateCampaignForm,
} from "../../../src/pages/admin/voucherCampaignLogic.js";

test("payload sends criteria only for chosen filters and schedule only in schedule mode", () => {
  const form = { ...emptyCampaignForm(), name: " Quay lại nhé ", tierIds: ["t1"], categoryId: "", maxDiscount: "50000" };
  const payload = campaignPayload(form);
  assert.deepEqual(payload.criteria, { tierIds: ["t1"], inactiveDays: null, categoryId: null, skipRecent: true });
  assert.equal(payload.name, "Quay lại nhé");
  assert.equal(payload.maxDiscount, 50000);
  assert.equal(payload.scheduledAt, null);
  const scheduled = campaignPayload({ ...form, useInactive: true, inactiveDays: "90", sendMode: "schedule", scheduledAt: "2026-11-10T09:00", discountType: "FIXED" });
  assert.equal(scheduled.criteria.inactiveDays, 90);
  assert.equal(scheduled.scheduledAt, "2026-11-10T09:00");
  assert.equal(scheduled.maxDiscount, null);
});

test("validation catches missing name, bad discount, days and past schedule", () => {
  const ok = { ...emptyCampaignForm(), name: "A" };
  assert.equal(validateCampaignForm(ok), "");
  assert.match(validateCampaignForm({ ...ok, name: " " }), /tên/);
  assert.match(validateCampaignForm({ ...ok, discountValue: "120" }), /100/);
  assert.match(validateCampaignForm({ ...ok, validDays: "0" }), /1 đến 365/);
  const now = new Date(2026, 10, 2, 12, 0);
  assert.match(validateCampaignForm({ ...ok, sendMode: "schedule", scheduledAt: "2026-11-01T09:00" }, now), /tương lai/);
});

test("campaign texts describe benefit, audience and usage", () => {
  const campaign = { discountType: "PERCENT", discountValue: 15, maxDiscount: 100000, minOrderValue: 300000, validDays: 14,
    tierNames: ["Vàng"], inactiveDays: 90, categoryName: "Áo", skipRecentDays: 14, recipientCount: 40, usedCount: 12 };
  assert.equal(benefitText(campaign), `Giảm 15% (tối đa ${(100000).toLocaleString("vi-VN")}đ) · đơn từ ${(300000).toLocaleString("vi-VN")}đ · dùng 14 ngày`);
  assert.equal(audienceText(campaign), "Hạng Vàng · 90 ngày chưa mua · Từng mua Áo · bỏ qua khách nhận voucher trong 14 ngày");
  assert.equal(usageText(campaign), "12 / 40 (30%)");
  assert.equal(usageText({ recipientCount: null }), "—");
  assert.equal(audienceText({}), "Mọi khách");
});
