import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Pan, Puntos y Premios",
  description: "Consulta tu saldo e historial de puntos.",
};

export const viewport: Viewport = {
  themeColor: "#f8f6f1",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es-MX">
      <body>{children}</body>
    </html>
  );
}
