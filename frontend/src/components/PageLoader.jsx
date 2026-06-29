import { useEffect, useState } from "react";

/**
 * PageLoader — full-screen brand splash shown on every page load/refresh.
 * Fades in quickly, holds briefly, then fades out and unmounts.
 */
export default function PageLoader({ onDone }) {
  const [isFading, setIsFading] = useState(false);

  useEffect(() => {
    // Phase 1: Hold for 1200ms, then trigger fade out state
    const fadeTimer = setTimeout(() => {
      setIsFading(true);
    }, 1200);

    // Phase 2: after fade-out completes (1200 + 800 = 2000ms), notify parent
    const doneTimer = setTimeout(() => {
      onDone?.();
    }, 2000);

    return () => {
      clearTimeout(fadeTimer);
      clearTimeout(doneTimer);
    };
  }, [onDone]);

  return (
    <div
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
        opacity: isFading ? 0 : 1,
        transform: isFading ? "scale(1.1)" : "scale(1)",
        transition: "opacity 0.8s cubic-bezier(0.4, 0, 0.2, 1), transform 0.8s cubic-bezier(0.4, 0, 0.2, 1)",
        willChange: "opacity, transform",
        pointerEvents: isFading ? "none" : "auto"
      }}
    >
      {/* Ambient glows */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: "radial-gradient(circle at 50% 50%, rgba(37,99,235,0.08) 0%, transparent 70%)",
          pointerEvents: "none"
        }}
      />
      <div 
        style={{
          position: "absolute",
          bottom: "-20%",
          left: "20%",
          width: "60%",
          height: "40%",
          background: "rgba(59, 130, 246, 0.15)",
          filter: "blur(100px)",
          borderRadius: "50%",
          animation: "pl-glow-pulse 1s ease-in-out infinite alternate"
        }}
      />

      {/* Massive Brand Name & Icon */}
      <div 
        style={{ 
          position: "relative", 
          display: "flex", 
          flexDirection: "column", 
          alignItems: "center",
          animation: "pl-fade-up 0.5s cubic-bezier(0.22, 1, 0.36, 1) both"
        }}
      >
        {/* Premium Book Icon */}
        <div
          style={{
            width: 64,
            height: 64,
            marginBottom: 20,
            borderRadius: "50%",
            background: "linear-gradient(135deg, rgba(30,58,138,0.8) 0%, rgba(29,78,216,0.8) 50%, rgba(59,130,246,0.8) 100%)",
            border: "1px solid rgba(96,165,250,0.3)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            boxShadow: "0 0 32px rgba(37,99,235,0.4), inset 0 0 16px rgba(255,255,255,0.1)",
            backdropFilter: "blur(8px)"
          }}
        >
          <BookOpenIcon />
        </div>

        <h2
          style={{
            fontFamily: "var(--f-display, 'Bebas Neue', sans-serif)",
            fontSize: "clamp(6rem, 18vw, 24rem)",
            letterSpacing: "0.02em",
            margin: 0,
            lineHeight: 1,
            background: "linear-gradient(to bottom, #ffffff 0%, rgba(255,255,255,0.1) 100%)",
            WebkitBackgroundClip: "text",
            WebkitTextFillColor: "transparent",
            backgroundClip: "text",
            color: "transparent",
            textShadow: "0 20px 60px rgba(37,99,235,0.15)"
          }}
        >
          AIVIRA
        </h2>
        
        {/* Scanning line over text */}
        <div 
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "linear-gradient(to bottom, transparent 0%, rgba(255,255,255,0.8) 50%, transparent 100%)",
            backgroundSize: "100% 200%",
            WebkitBackgroundClip: "text",
            WebkitTextFillColor: "transparent",
            backgroundClip: "text",
            opacity: 0.5,
            animation: "pl-scan 1s linear infinite"
          }}
        >
          AIVIRA
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
            animation: "pl-bar 0.9s cubic-bezier(0.22, 1, 0.36, 1) both"
          }}
        />
      </div>

      {/* Keyframes */}
      <style>{`
        @keyframes pl-fade-up {
          from { opacity: 0; transform: translateY(20px) scale(0.95); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }
        @keyframes pl-bar {
          0% { width: 0%; }
          40% { width: 60%; }
          100% { width: 100%; }
        }
        @keyframes pl-glow-pulse {
          from { opacity: 0.5; transform: scale(0.9); }
          to { opacity: 1; transform: scale(1.1); }
        }
        @keyframes pl-scan {
          0% { background-position: 0% -100%; }
          100% { background-position: 0% 200%; }
        }
      `}</style>
    </div>
  );
}

function BookOpenIcon() {
  return (
    <svg
      width="30"
      height="30"
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
