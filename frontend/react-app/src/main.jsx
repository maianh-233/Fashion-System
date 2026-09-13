import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css";
import { ThemeProvider } from "./contexts/ThemeContext";
import { SystemNotificationProvider } from "./components/common/SystemNotification";
import { installDevtoolsProtection } from "./utils/devtoolsProtection";

import "@fortawesome/fontawesome-free/css/all.min.css";

if (import.meta.env.PROD) {
  installDevtoolsProtection();
}

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <ThemeProvider>
      <SystemNotificationProvider>
        <App />
      </SystemNotificationProvider>
    </ThemeProvider>
  </React.StrictMode>
);
