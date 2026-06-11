import { useLayoutEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { gsap } from "gsap";
import "./IntroBook.css";

export const INTRO_SESSION_KEY = "aivira_intro_seen";

export function hasSeenIntro() {
  try {
    const params = new URLSearchParams(window.location.search);
    if (params.get("intro") === "1" || params.get("intro") === "true") {
      sessionStorage.removeItem(INTRO_SESSION_KEY);
      return false;
    }
    return sessionStorage.getItem(INTRO_SESSION_KEY) === "true";
  } catch {
    return false;
  }
}

export default function IntroBook({ onFinish }) {
  const { t } = useTranslation();
  const overlayRef = useRef(null);
  const bookContainerRef = useRef(null);
  const bookRef = useRef(null);
  const frontWrapperRef = useRef(null);
  const copyRef = useRef(null);
  const controlsRef = useRef(null);
  const floatAnimRef = useRef(null);

  const [playing, setPlaying] = useState(false);
  const playingRef = useRef(playing);
  playingRef.current = playing;

  const particles = useMemo(
    () =>
      Array.from({ length: 40 }, (_, index) => ({
        id: index,
        x: `${4 + ((index * 41) % 92)}%`,
        y: `${6 + ((index * 59) % 88)}%`,
        s: `${0.6 + ((index * 13) % 10) / 10}`,
        d: `${2.2 + ((index * 19) % 30) / 10}s`,
      })),
    []
  );

  const shootingStars = useMemo(
    () =>
      Array.from({ length: 8 }, (_, index) => ({
        id: index,
        top: `${-10 + Math.random() * 60}%`,
        left: `${20 + Math.random() * 80}%`,
        delay: `${Math.random() * 6}s`,
        duration: `${4 + Math.random() * 3}s`,
      })),
    []
  );

  useLayoutEffect(() => {
    let ctx = gsap.context(() => {
      const particleEls = gsap.utils.toArray(".aiv-intro-particle");

      gsap.set(overlayRef.current, { autoAlpha: 1 });
      
      gsap.set(bookRef.current, {
        rotateX: 18,
        rotateY: -22,
        rotateZ: -4,
        y: -30,
        scale: 0.9,
      });

      gsap.set(frontWrapperRef.current, {
        rotateY: 0,
      });

      gsap.set([copyRef.current, controlsRef.current], {
        autoAlpha: 0,
        y: 20,
        filter: "blur(10px)",
      });

      gsap.set(particleEls, {
        autoAlpha: 0,
        scale: 0,
      });

      const introTl = gsap.timeline({ defaults: { ease: "power3.out" } });

      introTl
        .to(particleEls, {
          autoAlpha: "random(0.4, 0.9)",
          scale: 1,
          duration: 1.2,
          stagger: 0.015,
        })
        .to(
          bookRef.current,
          {
            y: 0,
            scale: 1,
            duration: 1.6,
            ease: "expo.out",
          },
          "-=1.0"
        )
        .to(
          copyRef.current,
          {
            autoAlpha: 1,
            y: 0,
            filter: "blur(0px)",
            duration: 1.0,
          },
          "-=0.8"
        )
        .to(
          controlsRef.current,
          {
            autoAlpha: 1,
            y: 0,
            filter: "blur(0px)",
            duration: 0.8,
          },
          "-=0.6"
        );

      floatAnimRef.current = gsap.to(bookRef.current, {
        y: 15,
        rotateX: 12,
        rotateY: -16,
        rotateZ: 0,
        duration: 3,
        repeat: -1,
        yoyo: true,
        ease: "sine.inOut",
        delay: 0.5,
      });

      gsap.to(particleEls, {
        y: "random(-40, 40)",
        x: "random(-20, 20)",
        autoAlpha: "random(0.3, 1)",
        duration: "random(3, 6)",
        repeat: -1,
        yoyo: true,
        ease: "sine.inOut",
        stagger: 0.1,
      });

      // Mouse tracking for tilt and hover effect
      const xTo = gsap.quickTo(bookContainerRef.current, "rotationY", { duration: 1.2, ease: "power3.out" });
      const yTo = gsap.quickTo(bookContainerRef.current, "rotationX", { duration: 1.2, ease: "power3.out" });
      const scaleTo = gsap.quickTo(bookContainerRef.current, "scale", { duration: 0.6, ease: "power2.out" });

      const handleMouseMove = (e) => {
        if (playingRef.current) return;
        const { innerWidth, innerHeight } = window;
        const x = (e.clientX / innerWidth - 0.5) * 2;
        const y = (e.clientY / innerHeight - 0.5) * 2;
        
        xTo(x * 25); // Mouse X rotates Y axis
        yTo(y * -25); // Mouse Y rotates X axis
      };

      const handleMouseEnter = () => {
        if (playingRef.current) return;
        scaleTo(1.05); // Enlarge deeply on hover
      };

      const handleMouseLeaveWindow = () => {
        if (playingRef.current) return;
        xTo(0);
        yTo(0);
      };

      const handleMouseLeave = () => {
        if (playingRef.current) return;
        scaleTo(1);
      };

      window.addEventListener("mousemove", handleMouseMove);
      window.addEventListener("mouseleave", handleMouseLeaveWindow);
      
      const bookEl = bookContainerRef.current;
      if (bookEl) {
        bookEl.addEventListener("mouseenter", handleMouseEnter);
        bookEl.addEventListener("mouseleave", handleMouseLeave);
      }

      return () => {
        window.removeEventListener("mousemove", handleMouseMove);
        window.removeEventListener("mouseleave", handleMouseLeaveWindow);
        if (bookEl) {
          bookEl.removeEventListener("mouseenter", handleMouseEnter);
          bookEl.removeEventListener("mouseleave", handleMouseLeave);
        }
      };

    }, overlayRef);

    return () => {
      ctx.revert();
    };
  }, []);

  function completeIntro() {
    try {
      sessionStorage.setItem(INTRO_SESSION_KEY, "true");
    } catch {
      // Session storage can be unavailable in private browsing modes.
    }
    onFinish?.();
  }

  function skipIntro() {
    if (playing) return;
    floatAnimRef.current?.kill();
    gsap.to(overlayRef.current, {
      autoAlpha: 0,
      scale: 1.05,
      filter: "blur(10px)",
      duration: 0.3,
      ease: "power2.out",
      onComplete: completeIntro,
    });
  }

  function openBook() {
    if (playing) return;
    setPlaying(true);
    
    // Smoothly stop the floating animation instead of harsh kill
    floatAnimRef.current?.kill();

    const particleEls = overlayRef.current?.querySelectorAll(".aiv-intro-particle");
    
    const isMobile = window.innerWidth <= 680;

    const tl = gsap.timeline({
      defaults: { ease: "power4.inOut" },
    });

    tl
      .to(
        [controlsRef.current, copyRef.current],
        {
          autoAlpha: 0,
          y: 20,
          filter: "blur(10px)",
          duration: 0.3,
          ease: "power2.in",
        },
        0
      )
      .to(
        bookContainerRef.current,
        {
          rotationX: 0,
          rotationY: 0,
          rotationZ: 0,
          duration: 0.8,
          ease: "power2.inOut",
        },
        0
      )
      .to(
        bookRef.current,
        {
          rotateX: 10,
          rotateY: 0,
          rotateZ: 0,
          x: isMobile ? "40%" : "25%", // Center open book appropriately
          y: -15,
          scale: isMobile ? 0.85 : 1.15, // Scale down on mobile to prevent overflow
          duration: 1.0,
        },
        0.1
      )
      .to(
        frontWrapperRef.current,
        {
          rotateY: -178, // Open from right to left
          duration: 1.2,
          ease: "back.inOut(0.8)", // Slight bounce effect when fully opened
        },
        0.1
      )
      .to(
        particleEls,
        {
          autoAlpha: 1,
          scale: 2.5,
          x: "random(-200, 200)",
          y: "random(-200, 200)",
          duration: 0.8,
          stagger: 0.002,
          ease: "expo.out",
        },
        0.6
      )
      .to(
        overlayRef.current,
        {
          autoAlpha: 0,
          scale: 1.25,
          filter: "blur(6px)",
          duration: 0.45,
          ease: "power2.inOut",
          onComplete: completeIntro,
        },
        1.0 // Super fast transition right as it finishes opening
      );
  }

  return (
    <section className="aiv-intro" ref={overlayRef} aria-label="Aivira intro">
      <div className="aiv-intro-bg" />
      <div className="aiv-intro-noise" />
      <div className="aiv-intro-vignette" />

      <div className="aiv-intro-particles" aria-hidden="true">
        {particles.map((particle) => (
          <span
            key={particle.id}
            className="aiv-intro-particle"
            style={{
              "--x": particle.x,
              "--y": particle.y,
              "--s": particle.s,
              "--d": particle.d,
            }}
          />
        ))}
      </div>

      <div className="aiv-shooting-stars" aria-hidden="true">
        {shootingStars.map((star) => (
          <span
            key={star.id}
            className="aiv-shooting-star"
            style={{
              "--top": star.top,
              "--left": star.left,
              "--del": star.delay,
              "--dur": star.duration,
            }}
          />
        ))}
      </div>

      <button className="aiv-intro-skip" type="button" onClick={skipIntro} disabled={playing}>
        {t("intro.skip")}
      </button>

      <div className="aiv-intro-stage">
        <div className="aiv-intro-book-container" ref={bookContainerRef}>
          <button className="aiv-intro-book" ref={bookRef} type="button" onClick={openBook}>
            <div className="aiv-book-back" />
            <div className="aiv-book-pages-right">
              <div className="aiv-right-page-content">
                <div className="aiv-page-border"></div>
                <div className="aiv-page-header">
                  <span className="aiv-page-num">01</span>
                  <span className="aiv-page-chap">{t("intro.chapter")}</span>
                </div>
                <h2 className="aiv-page-heading">{t("intro.pageHeading1")}<br/>{t("intro.pageHeading2")}</h2>
                <div className="aiv-page-divider"></div>
                <p className="aiv-page-text">
                   {t("intro.pageText")}
                </p>
                <div className="aiv-page-graphic">
                  <div className="aiv-pg-circle"></div>
                  <div className="aiv-pg-circle-inner"></div>
                </div>
              </div>
            </div>
            
            <div className="aiv-book-front-wrapper" ref={frontWrapperRef}>
              <div className="aiv-book-inside-cover">
                 <div className="aiv-inside-border"></div>
                 <span className="aiv-inside-cover-brand">{t("intro.insideBrand")}</span>
                 <div className="aiv-inside-illustration">
                   <div className="aiv-ill-star">✦</div>
                 </div>
              </div>
              <div className="aiv-book-front">
                <span className="aiv-book-cover-line" />
                <span className="aiv-book-cover-shine" />
                <span className="aiv-book-mark">A</span>
                <span className="aiv-book-title">AIVIRA</span>
                <span className="aiv-book-subtitle">{t("common.bookstore")}</span>
              </div>
            </div>
            <div className="aiv-book-shadow" />
          </button>
        </div>

        <div className="aiv-intro-copy" ref={copyRef}>
          <p className="aiv-intro-kicker">AIVIRA {t("common.bookstore")}</p>
          <h1>AIVIRA</h1>
          <p>{t("intro.copy")}</p>
        </div>

        <div className="aiv-intro-actions" ref={controlsRef}>
          <button className="aiv-intro-open" type="button" onClick={openBook} disabled={playing}>
            {t("intro.open")}
          </button>
        </div>
      </div>
    </section>
  );
}
