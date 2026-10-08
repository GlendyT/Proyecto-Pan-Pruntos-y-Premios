"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

type ApiError = { mensaje?: string };
const tokenConsumptions = new Map<string, Promise<void>>();

function consumeTokenOnce(token: string): Promise<void> {
  const pending = tokenConsumptions.get(token);
  if (pending) return pending;

  const request = (async () => {
    const response = await fetch("/api/v1/auth/consume", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
    });
    if (!response.ok) {
      const body = (await response.json().catch(() => ({}))) as ApiError;
      throw new Error(body.mensaje ?? "El enlace es inválido o ya expiró.");
    }

    // Esta lectura inicia la cookie CSRF de Spring Security para el logout.
    const profileResponse = await fetch("/api/v1/me", {
      credentials: "same-origin",
      cache: "no-store",
    });
    if (!profileResponse.ok) throw new Error("Se validó el enlace, pero no se pudo abrir tu cuenta.");
  })();
  tokenConsumptions.set(token, request);
  return request;
}

export default function AuthCallbackPage() {
  const router = useRouter();
  const tokenCallbackRef = useRef<string | null>(null);
  const [mensaje, setMensaje] = useState("Validando tu enlace seguro…");

  useEffect(() => {
    let cancelado = false;
    const hash = window.location.hash;
    const tokenEnUrl = new URLSearchParams(hash.replace(/^#/, "")).get("token");
    if (tokenEnUrl) {
      tokenCallbackRef.current = tokenEnUrl;
    }
    // En modo desarrollo React puede repetir los efectos. El primer ciclo limpia
    // el fragmento de la URL; el segundo debe recuperar el token ya capturado.
    const token = tokenEnUrl ?? tokenCallbackRef.current;
    window.history.replaceState(null, "", window.location.pathname);

    if (!token) {
      void Promise.resolve().then(() => {
        if (!cancelado) setMensaje("El enlace no contiene un token válido. Solicita uno nuevo desde el portal.");
      });
      return;
    }

    consumeTokenOnce(token)
      .then(() => {
        if (!cancelado) {
          setMensaje("Acceso confirmado. Abriendo tus puntos…");
          router.replace("/");
        }
      })
      .catch((cause: unknown) => {
        if (!cancelado) {
          setMensaje(cause instanceof Error ? cause.message : "No pudimos validar el enlace.");
        }
      });
    return () => { cancelado = true; };
  }, [router]);

  return (
    <main className="callback-page">
      <div className="callback-card">
        <span className="brand-mark" aria-hidden="true">P</span>
        <p className="eyebrow">PAN, PUNTOS & PREMIOS</p>
        <h1>Un momento</h1>
        <p className="muted" role="status">{mensaje}</p>
        <Link className="text-link" href="/">Volver al portal</Link>
      </div>
    </main>
  );
}
