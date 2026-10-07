import assert from "node:assert/strict";
import test from "node:test";
import { createRealtimeClient } from "./realtimeClient.js";

class FakeStompClient {
  static instances = [];

  constructor(configuration) {
    this.configuration = configuration;
    this.active = false;
    this.deactivated = false;
    FakeStompClient.instances.push(this);
  }

  activate() {
    this.active = true;
  }

  deactivate() {
    this.active = false;
    this.deactivated = true;
    return Promise.resolve();
  }

  subscribe(destination, callback) {
    this.destination = destination;
    this.messageCallback = callback;
    return { unsubscribe() {} };
  }
}

function makeClient(overrides = {}) {
  FakeStompClient.instances = [];
  return createRealtimeClient({
    ClientClass: FakeStompClient,
    brokerURL: "ws://localhost/ws",
    setTimeoutFn: () => 1,
    clearTimeoutFn: () => {},
    ...overrides,
  });
}

test("keeps one active STOMP client and replaces it when token changes", () => {
  const client = makeClient();

  client.connect("token-a");
  client.connect("token-a");
  assert.equal(FakeStompClient.instances.length, 1);
  assert.equal(FakeStompClient.instances[0].configuration.connectHeaders.Authorization, "Bearer token-a");

  client.connect("token-b");
  assert.equal(FakeStompClient.instances.length, 2);
  assert.equal(FakeStompClient.instances[0].deactivated, true);
  assert.equal(FakeStompClient.instances[1].configuration.connectHeaders.Authorization, "Bearer token-b");
});

test("disconnect cancels reconnect and prevents stale client resurrection", () => {
  let scheduled;
  let cancelled;
  const client = makeClient({
    setTimeoutFn: (callback) => {
      scheduled = callback;
      return 7;
    },
    clearTimeoutFn: (id) => {
      cancelled = id;
    },
  });
  client.connect("token-a");
  const stomp = FakeStompClient.instances[0];
  stomp.configuration.onWebSocketClose();

  client.disconnect();
  scheduled?.();

  assert.equal(cancelled, 7);
  assert.equal(stomp.deactivated, true);
  assert.equal(FakeStompClient.instances.length, 1);
});

test("dispatches valid generic envelopes and ignores invalid payloads", () => {
  const client = makeClient();
  const received = [];
  client.subscribe({ channel: "NOTIFICATION", eventType: "NOTIFICATION_CREATED" }, (event) => {
    received.push(event);
  });
  client.connect("token-a");
  const stomp = FakeStompClient.instances[0];
  stomp.configuration.onConnect();

  stomp.messageCallback({ body: "not-json" });
  stomp.messageCallback({ body: JSON.stringify({ channel: "NOTIFICATION" }) });
  const event = {
    version: 1,
    eventId: "22d6c79d-d54d-4cc4-824c-88ebd34d601d",
    eventType: "NOTIFICATION_CREATED",
    channel: "NOTIFICATION",
    recipientType: "USER",
    recipientId: "2229914d-df6a-4414-bf4c-7e27a111ff9e",
    payload: { id: "notification-id" },
    timestamp: "2026-10-08T10:00:00Z",
  };
  stomp.messageCallback({ body: JSON.stringify(event) });

  assert.deepEqual(received, [event]);
  assert.equal(stomp.destination, "/user/queue/events");
});

test("signals reconnect only after a connection has previously succeeded", () => {
  const client = makeClient();
  let reconnects = 0;
  client.onReconnect(() => {
    reconnects += 1;
  });
  client.connect("token-a");
  FakeStompClient.instances[0].configuration.onConnect();
  FakeStompClient.instances[0].configuration.onWebSocketClose();
  client.connect("token-a", { force: true });
  FakeStompClient.instances[1].configuration.onConnect();

  assert.equal(reconnects, 1);
});
