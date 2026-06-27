import "@testing-library/jest-dom/vitest";
import { afterAll, afterEach, beforeAll, vi } from "vitest";
import { server } from "./src/test/server.js";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  server.resetHandlers();
  localStorage.clear();
  sessionStorage.clear();
});
afterAll(() => server.close());

Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
});

window.scrollTo = vi.fn();

class ResizeObserverMock {
  constructor(callback) {
    this.callback = callback;
  }

  observe(element) {
    this.callback?.([{
      target: element,
      contentRect: { width: 1024, height: 320 },
      borderBoxSize: [{ inlineSize: 1024, blockSize: 320 }],
      contentBoxSize: [{ inlineSize: 1024, blockSize: 320 }]
    }], this);
  }

  unobserve() {}
  disconnect() {}
}

window.ResizeObserver = ResizeObserverMock;
globalThis.ResizeObserver = ResizeObserverMock;

Element.prototype.getBoundingClientRect = vi.fn(() => ({
  bottom: 320,
  height: 320,
  left: 0,
  right: 1024,
  top: 0,
  width: 1024,
  x: 0,
  y: 0,
  toJSON: () => {}
}));

class IntersectionObserverMock {
  constructor(callback) {
    this.callback = callback;
  }

  observe(element) {
    this.callback([{ isIntersecting: true, target: element }], this);
  }

  unobserve() {}
  disconnect() {}
  takeRecords() { return []; }
}

window.IntersectionObserver = IntersectionObserverMock;
globalThis.IntersectionObserver = IntersectionObserverMock;
