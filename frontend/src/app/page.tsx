"use client";

import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import Link from "next/link";

type ApiError = { mensaje?: string };
type Perfil = { idCliente: number; nombre: string };
type Saldo = { saldoPuntos: number };
type Movimiento = {
  idMovimiento: number;
  fecha: string;
  tipo: "ACUMULACION" | "CANJE" | string;
  puntos: number;
  referencia: string | null;
};
type PaginaMovimientos = {
  movimientos: Movimiento[];
  siguienteCursor: string | null;
  hayMas: boolean;
};

async function leerError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ApiError;
    return body.mensaje ?? "No pudimos completar la solicitud. Inténtalo de nuevo.";
  } catch {
    return "No pudimos conectar con el servicio. Inténtalo de nuevo.";
  }
}

function fechaLegible(fecha: string): string {
  return new Intl.DateTimeFormat("es-MX", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(fecha));
}

export default function Home() {
  const [correo, setCorreo] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const [perfil, setPerfil] = useState<Perfil | null>(null);
  const [saldo, setSaldo] = useState<Saldo | null>(null);
  const [movimientos, setMovimientos] = useState<Movimiento[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hayMas, setHayMas] = useState(false);
  const [cargandoMas, setCargandoMas] = useState(false);

  const cargarCuenta = useCallback(async () => {
    const response = await fetch("/api/v1/me", {
      credentials: "same-origin",
      cache: "no-store",
    });
    if (response.status === 401) {
      setPerfil(null);
      return;
    }
    if (!response.ok) throw new Error(await leerError(response));

    const datosPerfil = (await response.json()) as Perfil;
    const [respuestaSaldo, respuestaMovimientos] = await Promise.all([
      fetch("/api/v1/me/points", { credentials: "same-origin", cache: "no-store" }),
      fetch("/api/v1/me/points/movements?limit=10", {
        credentials: "same-origin",
        cache: "no-store",
      }),
    ]);
    if (!respuestaSaldo.ok) throw new Error(await leerError(respuestaSaldo));
    if (!respuestaMovimientos.ok) throw new Error(await leerError(respuestaMovimientos));

    setPerfil(datosPerfil);
    setSaldo((await respuestaSaldo.json()) as Saldo);
    const pagina = (await respuestaMovimientos.json()) as PaginaMovimientos;
    setMovimientos(pagina.movimientos);
    setCursor(pagina.siguienteCursor);
    setHayMas(pagina.hayMas);
  }, []);

  useEffect(() => {
    void Promise.resolve()
      .then(cargarCuenta)
      .catch((cause: unknown) => {
        setError(cause instanceof Error ? cause.message : "No pudimos cargar tu cuenta.");
      });
  }, [cargarCuenta]);

  async function solicitarEnlace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setMensaje("");
    setOcupado(true);
    try {
      const response = await fetch("/api/v1/auth/access-requests", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: correo.trim() }),
      });
      if (!response.ok) throw new Error(await leerError(response));
      const body = (await response.json()) as ApiError;
      setMensaje(body.mensaje ?? "Si el correo corresponde a una cuenta activa, recibirás un enlace de acceso.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No pudimos solicitar el enlace.");
    } finally {
      setOcupado(false);
    }
  }

  async function cargarMas() {
    if (!cursor || cargandoMas) return;
    setCargandoMas(true);
    setError("");
    try {
      const query = new URLSearchParams({ limit: "10", before: cursor });
      const response = await fetch(`/api/v1/me/points/movements?${query}`, {
        credentials: "same-origin",
        cache: "no-store",
      });
      if (!response.ok) throw new Error(await leerError(response));
      const pagina = (await response.json()) as PaginaMovimientos;
      setMovimientos((actuales) => [...actuales, ...pagina.movimientos]);
      setCursor(pagina.siguienteCursor);
      setHayMas(pagina.hayMas);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No pudimos cargar más movimientos.");
    } finally {
      setCargandoMas(false);
    }
  }

  async function cerrarSesion() {
    setOcupado(true);
    setError("");
    try {
      // La respuesta de /me establece la cookie XSRF-TOKEN del backend si aún no existe.
      await fetch("/api/v1/me", { credentials: "same-origin", cache: "no-store" });
      const cookie = document.cookie
        .split("; ")
        .find((item) => item.startsWith("XSRF-TOKEN="))
        ?.split("=")[1];
      if (!cookie) throw new Error("No se pudo inicializar la protección de la sesión. Recarga la página.");

      const response = await fetch("/api/v1/auth/logout", {
        method: "POST",
        credentials: "same-origin",
        headers: { "X-XSRF-TOKEN": decodeURIComponent(cookie) },
      });
      if (!response.ok) throw new Error(await leerError(response));
      setPerfil(null);
      setSaldo(null);
      setMovimientos([]);
      setMensaje("Sesión cerrada.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No pudimos cerrar la sesión.");
    } finally {
      setOcupado(false);
    }
  }

  return (
    <main className="page-shell">
      <header className="topbar">
        <Link className="brand" href="/" aria-label="Pan, Puntos y Premios, inicio">
          <span className="brand-mark" aria-hidden="true">P</span>
          <span>Pan, Puntos <span className="brand-amp">&</span> Premios</span>
        </Link>
        {perfil && (
          <button className="button button-quiet" type="button" onClick={cerrarSesion} disabled={ocupado}>
            Cerrar sesión
          </button>
        )}
      </header>

      {perfil && saldo ? (
        <section className="dashboard" aria-labelledby="welcome-title">
          <div className="welcome-row">
            <div>
              <p className="eyebrow">TU CUENTA</p>
              <h1 id="welcome-title">Hola, {perfil.nombre}</h1>
              <p className="muted">Aquí puedes revisar tus puntos y sus movimientos.</p>
            </div>
            <div className="points-card">
              <span className="points-label">Saldo disponible</span>
              <strong>{saldo.saldoPuntos.toLocaleString("es-MX")}</strong>
              <span className="points-unit">puntos</span>
            </div>
          </div>

          <section className="history-card" aria-labelledby="history-title">
            <div className="section-heading">
              <div>
                <p className="eyebrow">ACTIVIDAD</p>
                <h2 id="history-title">Historial de puntos</h2>
              </div>
              <span className="history-count">{movimientos.length} movimientos</span>
            </div>
            {movimientos.length === 0 ? (
              <div className="empty-state">Todavía no tienes movimientos de puntos.</div>
            ) : (
              <div className="movement-list">
                {movimientos.map((movimiento) => {
                  const acumulacion = movimiento.tipo === "ACUMULACION";
                  return (
                    <article className="movement" key={movimiento.idMovimiento}>
                      <span className={`movement-icon ${acumulacion ? "is-earned" : "is-redeemed"}`} aria-hidden="true">
                        {acumulacion ? "+" : "−"}
                      </span>
                      <div className="movement-copy">
                        <strong>{acumulacion ? "Puntos acumulados" : "Canje de puntos"}</strong>
                        <span>{fechaLegible(movimiento.fecha)}</span>
                      </div>
                      <strong className={`movement-points ${acumulacion ? "is-earned-text" : "is-redeemed-text"}`}>
                        {acumulacion ? "+" : "−"}{movimiento.puntos.toLocaleString("es-MX")}
                      </strong>
                    </article>
                  );
                })}
              </div>
            )}
            {hayMas && (
              <button className="button button-outline load-more" type="button" onClick={cargarMas} disabled={cargandoMas}>
                {cargandoMas ? "Cargando…" : "Ver movimientos anteriores"}
              </button>
            )}
          </section>
        </section>
      ) : (
        <section className="login-layout" aria-labelledby="login-title">
          <div className="intro-copy">
            <p className="eyebrow">TU PANADERÍA, TUS BENEFICIOS</p>
            <h1 id="login-title">Cada visita<br />suma algo bueno.</h1>
            <p className="intro-description">Consulta tus puntos acumulados y descubre cómo avanzan tus recompensas.</p>
            <div className="intro-note"><span aria-hidden="true">✦</span> Tus puntos, siempre a la mano.</div>
          </div>

          <section className="login-card" aria-label="Acceso de clientes">
            <div className="card-icon" aria-hidden="true">✳</div>
            <p className="eyebrow">ACCESO DE CLIENTE</p>
            <h2>Consulta tus puntos</h2>
            <p className="muted login-hint">Te enviaremos un enlace seguro a tu correo registrado. No necesitas crear una contraseña.</p>
            <form className="login-form" onSubmit={solicitarEnlace}>
              <label htmlFor="correo">Correo electrónico</label>
              <input
                id="correo"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="tu@correo.com"
                value={correo}
                onChange={(event) => setCorreo(event.target.value)}
                required
                maxLength={100}
              />
              <button className="button button-primary" type="submit" disabled={ocupado}>
                {ocupado ? "Enviando…" : "Enviarme un enlace"}
                {!ocupado && <span aria-hidden="true">→</span>}
              </button>
            </form>
            {mensaje && <p className="notice notice-success" role="status">{mensaje}</p>}
            <p className="privacy-note">Por seguridad, el enlace solo funciona una vez y caduca en 15 minutos.</p>
          </section>
        </section>
      )}

      {error && <p className="notice notice-error" role="alert">{error}</p>}
      <footer className="footer">Pan, Puntos y Premios <span>·</span> Tus compras tienen recompensa.</footer>
    </main>
  );
}
