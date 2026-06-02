const REVEAL_SELECTOR = "[data-reveal]";
const TILT_SELECTOR = "[data-tilt]";

export function initMotionEffects(root = document) {
  const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches;
  if (reduceMotion) return () => {};

  const revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add("is-visible");
        revealObserver.unobserve(entry.target);
      });
    },
    { threshold: 0.14, rootMargin: "0px 0px -8% 0px" }
  );

  root.querySelectorAll(REVEAL_SELECTOR).forEach((element, index) => {
    element.classList.add("scroll-reveal");
    element.style.setProperty("--reveal-delay", `${Math.min(index % 6, 5) * 42}ms`);
    revealObserver.observe(element);
  });

  const cleanups = [];
  root.querySelectorAll(TILT_SELECTOR).forEach((element) => {
    element.classList.add("tilt-card");
    let targetX = 0;
    let targetY = 0;
    let currentX = 0;
    let currentY = 0;
    let frame = 0;

    const render = () => {
      currentX += (targetX - currentX) * 0.16;
      currentY += (targetY - currentY) * 0.16;
      element.style.setProperty("--tilt-x", `${currentX.toFixed(2)}deg`);
      element.style.setProperty("--tilt-y", `${currentY.toFixed(2)}deg`);

      if (Math.abs(targetX - currentX) > 0.01 || Math.abs(targetY - currentY) > 0.01) {
        frame = requestAnimationFrame(render);
      } else {
        frame = 0;
      }
    };

    const start = () => {
      if (!frame) frame = requestAnimationFrame(render);
    };

    const onMove = (event) => {
      const rect = element.getBoundingClientRect();
      const x = (event.clientX - rect.left) / rect.width - 0.5;
      const y = (event.clientY - rect.top) / rect.height - 0.5;
      targetX = -y * 4.5;
      targetY = x * 5.5;
      element.style.setProperty("--tilt-glow-x", `${((x + 0.5) * 100).toFixed(1)}%`);
      element.style.setProperty("--tilt-glow-y", `${((y + 0.5) * 100).toFixed(1)}%`);
      start();
    };

    const onLeave = () => {
      targetX = 0;
      targetY = 0;
      element.style.setProperty("--tilt-glow-x", "50%");
      element.style.setProperty("--tilt-glow-y", "50%");
      start();
    };

    element.addEventListener("mousemove", onMove);
    element.addEventListener("mouseleave", onLeave);
    cleanups.push(() => {
      if (frame) cancelAnimationFrame(frame);
      element.removeEventListener("mousemove", onMove);
      element.removeEventListener("mouseleave", onLeave);
    });
  });

  return () => {
    revealObserver.disconnect();
    cleanups.forEach((cleanup) => cleanup());
  };
}
