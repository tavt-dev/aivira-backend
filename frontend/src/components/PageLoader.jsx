import { useEffect, useRef } from "react";

/**
 * PageLoader — full-screen brand splash shown on every page load/refresh.
 * Fades in quickly, holds briefly, then fades out and unmounts.
 * Does NOT use sessionStorage — appears on every tab reload (different from IntroBook).
 */
export default function PageLoader({ onDone }) {
  const ref = useRef(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    // Phase 1: already visible (opacity:1 set in initial style)
    // Phase 2: after 480ms, start fade-out
    const fadeTimer = setTimeout(() => {
      el.style.transition = "opacity 0.38s ease, transform 0.38s ease";
      el.style.opacity = "0";
      el.style.transform = "scale(1.04)";
    }, 480);

    // Phase 3: after fade-out completes, notify parent
    const doneTimer = setTimeout(() => {
      onDone?.();
    }, 880);

    return () => {
      clearTimeout(fadeTimer);
      clearTimeout(doneTimer);
    };
  }, [onDone]);

  return (
    <div
      ref={ref}
      aria-hidden="true"
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 9998,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        background: "#020617",
        opacity: 1,
        transform: "scale(1)",
        willChange: "opacity, transform"
      }}
    >
      {/* Ambient glow */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          background:
            "radial-gradient(circle at 50% 50%, rgba(37,99,235,0.18) 0%, transparent 60%)",
          pointerEvents: "none"
        }}
      />

      {/* Logo mark */}
      <div style={{ position: "relative", display: "flex", flexDirection: "column", alignItems: "center", gap: 20 }}>
        {/* Pulse ring */}
        <div style={{ position: "relative", width: 72, height: 72, display: "flex", alignItems: "center", justifyContent: "center" }}>
          {/* Outer ring */}
          <div
            style={{
              position: "absolute",
              inset: -10,
              borderRadius: "50%",
              border: "1px solid rgba(59,130,246,0.3)",
              animation: "pl-ring-outer 1.4s ease-out infinite"
            }}
          />
          {/* Inner ring */}
          <div
            style={{
              position: "absolute",
              inset: 0,
              borderRadius: "50%",
              border: "1.5px solid rgba(59,130,246,0.55)",
              animation: "pl-ring-inner 1.4s ease-out infinite 0.2s"
            }}
          />
          {/* Book icon */}
          <div
            style={{
              width: 72,
              height: 72,
              borderRadius: "50%",
              background: "linear-gradient(135deg, #1e3a8a 0%, #1d4ed8 50%, #3b82f6 100%)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              boxShadow: "0 0 32px rgba(37,99,235,0.5), 0 0 64px rgba(37,99,235,0.2)"
            }}
          >
            <BookOpenIcon />
          </div>
        </div>

        {/* Brand name */}
        <div
          style={{
            fontFamily: "var(--f-display, 'Bebas Neue', sans-serif)",
            fontSize: "clamp(2rem, 5vw, 2.8rem)",
            letterSpacing: "0.28em",
            color: "#fff",
            textShadow: "0 0 30px rgba(96,165,250,0.45)",
            lineHeight: 1,
            animation: "pl-fade-up 0.4s ease-out both"
          }}
        >
          AIVIRA
        </div>

        {/* Tagline */}
        <div
          style={{
            fontFamily: "var(--f-body, 'Outfit', sans-serif)",
            fontSize: "0.7rem",
            fontWeight: 600,
            letterSpacing: "0.22em",
            textTransform: "uppercase",
            color: "rgba(148,163,184,0.65)",
            animation: "pl-fade-up 0.5s ease-out 0.1s both"
          }}
        >
          Bookstore
        </div>
      </div>

      {/* Loading bar at bottom */}
      <div
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          right: 0,
          height: 2,
          background: "rgba(255,255,255,0.05)"
        }}
      >
        <div
          style={{
            height: "100%",
            background: "linear-gradient(90deg, #1d4ed8, #3b82f6, #60a5fa)",
            animation: "pl-bar 0.85s ease-in-out both"
          }}
        />
      </div>

      {/* Keyframes */}
      <style>{`
        @keyframes pl-ring-outer {
          0% { transform: scale(1); opacity: 0.5; }
          50% { transform: scale(1.18); opacity: 0.2; }
          100% { transform: scale(1); opacity: 0.5; }
        }
        @keyframes pl-ring-inner {
          0% { transform: scale(1); opacity: 0.8; }
          50% { transform: scale(1.08); opacity: 0.4; }
          100% { transform: scale(1); opacity: 0.8; }
        }
        @keyframes pl-fade-up {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes pl-bar {
          0% { width: 0%; }
          60% { width: 75%; }
          100% { width: 100%; }
        }
      `}</style>
    </div>
  );
}

function BookOpenIcon() {
  return (
    <svg
      width="34"
      height="34"
      viewBox="0 0 24 24"
      fill="none"
      stroke="rgba(255,255,255,0.9)"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
      <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
    </svg>
  );
}
