const BLOCKED_KEYS = new Set(["i", "j", "c"]);

/**
 * Adds a lightweight deterrent against opening browser developer tools.
 *
 * This cannot protect secrets or business logic shipped to the browser.
 * Sensitive logic and credentials must always stay on the server.
 *
 * @returns {() => void} removes all installed listeners
 */
export function installDevtoolsProtection() {
  const preventDevtoolsShortcuts = (event) => {
    const key = event.key.toLowerCase();
    const opensDevtools =
      event.key === "F12" ||
      ((event.ctrlKey || event.metaKey) && event.shiftKey && BLOCKED_KEYS.has(key)) ||
      ((event.ctrlKey || event.metaKey) && key === "u");

    if (opensDevtools) {
      event.preventDefault();
      event.stopPropagation();
    }
  };

  const preventContextMenu = (event) => event.preventDefault();

  window.addEventListener("keydown", preventDevtoolsShortcuts, true);
  window.addEventListener("contextmenu", preventContextMenu, true);

  return () => {
    window.removeEventListener("keydown", preventDevtoolsShortcuts, true);
    window.removeEventListener("contextmenu", preventContextMenu, true);
  };
}

