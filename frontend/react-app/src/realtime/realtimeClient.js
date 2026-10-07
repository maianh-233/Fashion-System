import { Client } from "@stomp/stompjs";
import { createReconnectPolicy } from "./reconnectPolicy.js";

const EVENTS_DESTINATION = "/user/queue/events";
const ACCOUNT_TYPES = new Set(["USER", "CUSTOMER"]);

function defaultBrokerUrl() {
  const configured = import.meta.env?.VITE_WS_URL;
  if (configured) return configured;
  const apiBase = import.meta.env?.VITE_API_BASE_URL || globalThis.location?.origin || "http://localhost:8080";
  const url = new URL(apiBase, globalThis.location?.origin || "http://localhost:8080");
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  url.pathname = `${url.pathname.replace(/\/$/, "")}/ws`;
  url.search = "";
  url.hash = "";
  return url.toString();
}

function parseEnvelope(body) {
  try {
    const event = JSON.parse(body);
    if (!event || typeof event !== "object"
      || !Number.isInteger(event.version) || event.version < 1
      || typeof event.eventId !== "string"
      || typeof event.eventType !== "string"
      || typeof event.channel !== "string"
      || !ACCOUNT_TYPES.has(event.recipientType)
      || typeof event.recipientId !== "string"
      || typeof event.timestamp !== "string"
      || !("payload" in event)) {
      return null;
    }
    return event;
  } catch {
    return null;
  }
}

export function createRealtimeClient({
  ClientClass = Client,
  brokerURL = defaultBrokerUrl(),
  reconnectPolicy = createReconnectPolicy(),
  setTimeoutFn = globalThis.setTimeout,
  clearTimeoutFn = globalThis.clearTimeout,
} = {}) {
  let stompClient = null;
  let activeToken = null;
  let generation = 0;
  let reconnectTimer = null;
  let hasConnected = false;
  const subscribers = new Set();
  const reconnectSubscribers = new Set();

  function cancelReconnect() {
    if (reconnectTimer !== null) {
      clearTimeoutFn(reconnectTimer);
      reconnectTimer = null;
    }
  }

  function dispatch(message) {
    const event = parseEnvelope(message.body);
    if (!event) return;
    subscribers.forEach(({ filter, handler }) => {
      if ((!filter.channel || filter.channel === event.channel)
        && (!filter.eventType || filter.eventType === event.eventType)) {
        handler(event);
      }
    });
  }

  function scheduleReconnect(expectedGeneration) {
    if (!activeToken || reconnectTimer !== null || expectedGeneration !== generation) return;
    reconnectTimer = setTimeoutFn(() => {
      reconnectTimer = null;
      if (activeToken && expectedGeneration === generation) {
        connect(activeToken, { force: true });
      }
    }, reconnectPolicy.nextDelay());
  }

  function connect(token, { force = false } = {}) {
    if (typeof token !== "string" || !token) {
      disconnect();
      return;
    }
    if (!force && stompClient && activeToken === token) return;

    cancelReconnect();
    const previous = stompClient;
    generation += 1;
    const clientGeneration = generation;
    activeToken = token;
    stompClient = null;
    previous?.deactivate();

    const nextClient = new ClientClass({
      brokerURL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 0,
      debug: () => {},
      onConnect: () => {
        if (clientGeneration !== generation || stompClient !== nextClient) return;
        nextClient.subscribe(EVENTS_DESTINATION, dispatch);
        reconnectPolicy.reset();
        if (hasConnected) reconnectSubscribers.forEach((handler) => handler());
        hasConnected = true;
      },
      onWebSocketClose: () => scheduleReconnect(clientGeneration),
      onStompError: () => scheduleReconnect(clientGeneration),
    });
    stompClient = nextClient;
    nextClient.activate();
  }

  function disconnect() {
    cancelReconnect();
    generation += 1;
    activeToken = null;
    hasConnected = false;
    reconnectPolicy.reset();
    const previous = stompClient;
    stompClient = null;
    previous?.deactivate();
  }

  function subscribe(filter = {}, handler) {
    if (typeof handler !== "function") throw new TypeError("Realtime handler must be a function");
    const subscription = { filter, handler };
    subscribers.add(subscription);
    return () => subscribers.delete(subscription);
  }

  function onReconnect(handler) {
    reconnectSubscribers.add(handler);
    return () => reconnectSubscribers.delete(handler);
  }

  return { connect, disconnect, subscribe, onReconnect };
}
