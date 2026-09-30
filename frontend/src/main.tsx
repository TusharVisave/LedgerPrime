import React from "react";
import ReactDOM from "react-dom/client";
import { Toaster } from "react-hot-toast";
import { registerSW } from "virtual:pwa-register";
import { AuthProvider } from "./context/AuthContext";
import App from "./App";
import "./index.css";

// Register the PWA service worker (generateSW precaches the app shell).
registerSW({ immediate: true });

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: "#18181b",
            color: "#f4f4f5",
            border: "1px solid #27272a",
            borderRadius: "12px",
            fontSize: "14px",
          },
          success: { iconTheme: { primary: "#34d399", secondary: "#09090b" } },
          error: { iconTheme: { primary: "#fb7185", secondary: "#09090b" } },
        }}
      />
    </AuthProvider>
  </React.StrictMode>,
);
