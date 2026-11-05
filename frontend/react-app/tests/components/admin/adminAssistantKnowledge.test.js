import test from "node:test";
import assert from "node:assert/strict";
import { answerQuestion, findTopic, normalize, topics, welcomeMessage } from "../../../src/components/admin/assistant/adminAssistantKnowledge.js";

test("normalize strips Vietnamese accents and punctuation", () => {
  assert.equal(normalize("  Đổi TRẢ hàng?! "), "doi tra hang");
});

test("free-text questions map to the matching system topic", () => {
  assert.equal(findTopic("Làm sao để bán hàng tại quầy?").id, "pos");
  assert.equal(findTopic("hủy đơn đã thanh toán momo được không").id, "cancel");
  assert.equal(findTopic("Tạo phiếu điều chuyển hàng giữa cửa hàng").id, "transfers");
  assert.equal(findTopic("tỷ lệ đổi trả theo size ở đâu").id, "return-report");
  assert.equal(findTopic("điểm thưởng hết hạn khi nào").id, "loyalty");
  assert.equal(findTopic("nhập bảng size cho danh mục").id, "size-chart");
  assert.equal(findTopic("đổi mật khẩu").id, "profile");
});

test("whole-phrase matching avoids false hits inside other words", () => {
  assert.equal(findTopic("không biết"), null);
});

test("questions outside the system are declined with suggestions", () => {
  const reply = answerQuestion("Thời tiết Hà Nội hôm nay thế nào?");
  assert.equal(reply.outOfScope, true);
  assert.match(reply.text, /chỉ trả lời các câu hỏi liên quan đến hệ thống/);
  assert.ok(reply.suggestions.length > 0);
});

test("greetings, thanks and help requests get canned replies", () => {
  assert.equal(answerQuestion("Xin chào").text, welcomeMessage().text);
  assert.match(answerQuestion("cảm ơn nhé").text, /Rất vui/);
  assert.equal(answerQuestion("bạn làm được gì").suggestions.length, topics.length);
});

test("link is hidden when the account lacks the page permission", () => {
  const allowed = answerQuestion("flash sale", { hasPermission: () => true });
  assert.deepEqual(allowed.link, { path: "/admin/flash-sales", label: "Mở Flash sale" });
  const denied = answerQuestion("flash sale", { hasPermission: () => false });
  assert.equal(denied.link, null);
  assert.match(denied.text, /chưa có quyền/);
});

test("clicking a suggestion answers that topic directly", () => {
  assert.equal(answerQuestion("Hạng khách hàng", { topicId: "tiers" }).topicId, "tiers");
});

test("every topic has a unique id and at least one normalized keyword", () => {
  assert.equal(new Set(topics.map((topic) => topic.id)).size, topics.length);
  for (const topic of topics) {
    assert.ok(topic.keywords.length > 0, topic.id);
    for (const keyword of topic.keywords) assert.equal(normalize(keyword), keyword, `${topic.id}: ${keyword}`);
  }
});
