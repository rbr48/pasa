(() => {
  "use strict";

  /* ---------- Theme ---------- */
  const root = document.documentElement;
  const THEME_KEY = "pasa-theme";
  const ICONS = {
    sun: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>',
    moon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 12.8A9 9 0 1111.2 3 7 7 0 0021 12.8z"/></svg>'
  };
  function applyTheme(t) {
    root.setAttribute("data-theme", t);
    document.querySelectorAll("[data-theme-icon]").forEach((el) => {
      el.innerHTML = t === "light" ? ICONS.moon : ICONS.sun;
    });
  }
  try {
    const saved = localStorage.getItem(THEME_KEY);
    applyTheme(saved === "light" ? "light" : "dark");
  } catch (_) { applyTheme("dark"); }

  window.toggleTheme = function () {
    const next = root.getAttribute("data-theme") === "light" ? "dark" : "light";
    applyTheme(next);
    try { localStorage.setItem(THEME_KEY, next); } catch (_) {}
  };

  /* ---------- Nav scroll state + scroll progress ---------- */
  const nav = document.getElementById("nav");
  const progressBar = document.getElementById("scrollProgress");
  function onScroll() {
    if (window.scrollY > 8) nav.classList.add("scrolled");
    else nav.classList.remove("scrolled");
    if (progressBar) {
      const h = document.documentElement;
      const max = h.scrollHeight - h.clientHeight;
      progressBar.style.width = max > 0 ? (window.scrollY / max) * 100 + "%" : "0%";
    }
  }
  document.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  /* ---------- Cursor spotlight ---------- */
  const spotlight = document.getElementById("spotlight");
  if (spotlight && window.matchMedia("(hover: hover)").matches) {
    document.addEventListener(
      "mousemove",
      (e) => {
        spotlight.style.setProperty("--mx", e.clientX + "px");
        spotlight.style.setProperty("--my", e.clientY + "px");
      },
      { passive: true }
    );
  }

  /* ---------- Magnetic buttons ---------- */
  if (window.matchMedia("(hover: hover)").matches) {
    document.querySelectorAll(".magnetic").forEach((el) => {
      el.addEventListener("mousemove", (e) => {
        const r = el.getBoundingClientRect();
        const x = e.clientX - r.left - r.width / 2;
        const y = e.clientY - r.top - r.height / 2;
        el.style.transform = `translate(${x * 0.18}px, ${y * 0.35}px)`;
      });
      el.addEventListener("mouseleave", () => { el.style.transform = ""; });
    });
  }

  /* ---------- Scroll-spy rail ---------- */
  const railItems = document.querySelectorAll(".rail-item");
  const railTargets = Array.from(railItems)
    .map((item) => document.getElementById(item.dataset.rail))
    .filter(Boolean);
  if ("IntersectionObserver" in window && railTargets.length) {
    const railIo = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            railItems.forEach((item) => item.classList.toggle("active", item.dataset.rail === e.target.id));
          }
        });
      },
      { rootMargin: "-45% 0px -45% 0px", threshold: 0 }
    );
    railTargets.forEach((el) => railIo.observe(el));
  }

  /* ---------- Feature gallery ---------- */
  window.scrollGallery = function (dir) {
    const el = document.getElementById("featureGallery");
    if (!el) return;
    const card = el.querySelector(".gallery-card");
    const step = card ? card.getBoundingClientRect().width + 18 : 320;
    el.scrollBy({ left: dir * step, behavior: "smooth" });
  };

  /* ---------- Mobile menu ---------- */
  window.toggleMobileMenu = function () {
    document.getElementById("mobileMenu").classList.toggle("open");
  };
  document.querySelectorAll(".mobile-menu a").forEach((a) =>
    a.addEventListener("click", () => document.getElementById("mobileMenu").classList.remove("open"))
  );

  /* ---------- Scroll reveal ---------- */
  const revealEls = document.querySelectorAll(".reveal");
  if ("IntersectionObserver" in window) {
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            e.target.classList.add("in");
            io.unobserve(e.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -60px 0px" }
    );
    revealEls.forEach((el) => io.observe(el));
  } else {
    revealEls.forEach((el) => el.classList.add("in"));
  }

  /* ---------- Animated stat counters ---------- */
  function animateCount(el) {
    const target = parseFloat(el.dataset.count);
    const suffix = el.dataset.suffix || "";
    const decimals = el.dataset.decimals ? parseInt(el.dataset.decimals, 10) : 0;
    const dur = 1400;
    const start = performance.now();
    function tick(now) {
      const p = Math.min(1, (now - start) / dur);
      const eased = 1 - Math.pow(1 - p, 3);
      const val = target * eased;
      el.textContent = (decimals ? val.toFixed(decimals) : Math.round(val).toLocaleString()) + suffix;
      if (p < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }
  const counters = document.querySelectorAll("[data-count]");
  if ("IntersectionObserver" in window && counters.length) {
    const cio = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            animateCount(e.target);
            cio.unobserve(e.target);
          }
        });
      },
      { threshold: 0.5 }
    );
    counters.forEach((el) => cio.observe(el));
  }

  /* ---------- Scenario showcase ---------- */
  const SCENARIOS = {
    snatch: {
      tabId: "tabSnatch",
      status: "Snatch Detected",
      sub: "4 hardware traps engaged",
      shieldClass: "danger",
      sensors: [
        ["Kinetic Sensor", "TRIGGERED · 2.85G"],
        ["Screen Lock", "Device Owner Enforced"],
        ["Siren", "100dB Dispatched"],
        ["Camera", "Forensic Capture"],
      ],
      logTitle: "Incident Intercept — Kinetic Snatch",
      log1: "ALARM: 2.85G delta detected at 14:02:44. Screen locked via Device Owner policy.",
      log2: "Siren dispatched at 100% SPL. Covert selfie captured. GPS pin pushed to Telegram.",
    },
    sms: {
      tabId: "tabSms",
      status: "Air-Gap SMS Active",
      sub: "Mobile data disabled — GSM fallback live",
      shieldClass: "accent",
      sensors: [
        ["Mobile Data", "Disabled by thief"],
        ["SMS Channel", "TOTP-signed · Live"],
        ["GPS Fix", "Cached every 30s"],
        ["Commands", "Locate · Lock · Wipe"],
      ],
      logTitle: "Air-Gap SMS — Command Channel",
      log1: "Mobile data severed at 14:03:02. Falling back to raw GSM PDU channel.",
      log2: "TOTP-signed LOCATE command delivered over SMS. Coordinates returned in 6.2s.",
    },
    shutdown: {
      tabId: "tabShutdown",
      status: "Fake Shutdown Engaged",
      sub: "Screen dark — daemon still running",
      shieldClass: "neutral",
      sensors: [
        ["Display", "Spoofed power-off"],
        ["GPS Daemon", "Still streaming"],
        ["Microphone", "Ambient capture"],
        ["Relay", "Telegram channel open"],
      ],
      logTitle: "Stealth Trap — Fake Power-Down",
      log1: "Power button held. Boot animation spoofed; display renders black.",
      log2: "Background daemon persists — GPS, mic and Telegram relay continue uninterrupted.",
    },
    camera: {
      tabId: "tabCamera",
      status: "Forensic Camera Fired",
      sub: "Silent Camera2 capture · 1080p",
      shieldClass: "purple",
      sensors: [
        ["Shutter Sound", "Suppressed"],
        ["Screen Wake", "None"],
        ["Resolution", "1080p Camera2"],
        ["Signature", "Ed25519 signed"],
      ],
      logTitle: "Silent Forensic Camera — Triggered",
      log1: "Camera2 API invoked without UI wake or shutter sound at 14:04:11.",
      log2: "Forensic mugshot signed (Ed25519) and relayed to operator's Telegram endpoint.",
    },
  };

  window.setScenario = function (key) {
    const s = SCENARIOS[key];
    if (!s) return;
    document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
    const btn = document.getElementById(s.tabId);
    if (btn) btn.classList.add("active");

    const shieldColors = {
      danger: ["rgba(248,113,122,0.28)", "rgba(248,113,122,0.5)", "var(--danger)"],
      accent: ["rgba(59,130,246,0.25)", "rgba(59,130,246,0.4)", "var(--accent-2)"],
      neutral: ["rgba(255,255,255,0.08)", "rgba(255,255,255,0.18)", "var(--text-dim)"],
      purple: ["rgba(139,92,246,0.25)", "rgba(139,92,246,0.4)", "var(--accent-3)"],
    };
    const ring = document.getElementById("shieldRing");
    const [bg, border, color] = shieldColors[s.shieldClass];
    if (ring) {
      ring.style.background = `radial-gradient(circle, ${bg}, transparent 70%)`;
      ring.style.borderColor = border;
      ring.style.color = color;
    }
    setText("deviceStatusTitle", s.status);
    setText("deviceStatusSub", s.sub);

    const grid = document.getElementById("sensorGrid");
    if (grid) {
      grid.innerHTML = s.sensors
        .map(([k, v]) => `<div class="sensor-cell"><div class="k">${k}</div><div class="v">${v}</div></div>`)
        .join("");
    }
    setText("feedTitle", s.logTitle);
    setHtml("feedLog1", `<span class="lbl">&gt;</span> ${s.log1}`);
    setHtml("feedLog2", `<span class="lbl dim">&gt;</span> ${s.log2}`);
  };
  function setText(id, text) { const el = document.getElementById(id); if (el) el.textContent = text; }
  function setHtml(id, html) { const el = document.getElementById(id); if (el) el.innerHTML = html; }

  /* ---------- Live clock in device mockup ---------- */
  function tickClock() {
    const el = document.getElementById("deviceClock");
    if (el) {
      const d = new Date();
      el.textContent = d.toTimeString().slice(0, 5);
    }
  }
  tickClock();
  setInterval(tickClock, 30000);

  /* ---------- FAQ accordion ---------- */
  window.toggleFaq = function (el) {
    const wasOpen = el.classList.contains("open");
    el.parentElement.querySelectorAll(".faq-item").forEach((i) => i.classList.remove("open"));
    if (!wasOpen) el.classList.add("open");
  };

  /* ---------- Currency toggle (pricing) ---------- */
  const RATES = {
    trial: { bdt: "Free", usd: "Free" },
    pro: { bdt: "৳3,490", usd: "$29" },
    fleet: { bdt: "৳9,990", usd: "$84" },
  };
  window.switchCurrency = function (cur) {
    document.getElementById("curBdtBtn").classList.toggle("active", cur === "bdt");
    document.getElementById("curUsdBtn").classList.toggle("active", cur === "usd");
    document.querySelectorAll("[data-price-primary]").forEach((el) => {
      const plan = el.dataset.pricePrimary;
      el.textContent = RATES[plan][cur];
    });
    document.querySelectorAll("[data-price-secondary]").forEach((el) => {
      const plan = el.dataset.priceSecondary;
      const other = cur === "bdt" ? "usd" : "bdt";
      el.textContent = plan === "trial" ? "" : "/ " + RATES[plan][other];
    });
  };

  /* ---------- Checkout modal ---------- */
  const PLAN_PRICE = {
    PRO_LIFETIME: { bdt: 3490, label: "৳3,490 / $29 USD", short: "Pro Lifetime License" },
    PRO_ENTERPRISE: { bdt: 9990, label: "৳9,990 / $84 USD", short: "Fleet / Enterprise License" },
  };
  window.openCheckout = function (tier) {
    const plan = PLAN_PRICE[tier] || PLAN_PRICE.PRO_LIFETIME;
    document.getElementById("selectedTier").value = tier;
    document.getElementById("selectedPrice").value = plan.bdt;
    setText("checkoutPlanTitle", plan.short);
    setText("checkoutPriceLine", plan.label);
    setText("checkoutTotal", plan.label);
    setText("bkashAmount", "৳" + plan.bdt.toLocaleString("en-BD"));
    setText("binanceAmount", plan.label);
    document.getElementById("checkoutForm").classList.remove("hidden-step");
    document.getElementById("checkoutSuccess").classList.add("hidden-step");
    toggleModal("checkoutModal", true);
  };
  window.closeCheckout = function () { toggleModal("checkoutModal", false); };

  window.selectPayment = function (provider) {
    document.getElementById("paymentProvider").value = provider;
    document.querySelectorAll(".pm-btn").forEach((b) => b.classList.remove("active"));
    document.getElementById("pm-" + provider).classList.add("active");
    document.querySelectorAll(".pay-panel").forEach((p) => p.classList.remove("active"));
    document.getElementById("panel-" + provider).classList.add("active");
  };

  window.copyText = function (text, feedbackId) {
    const done = () => {
      const el = document.getElementById(feedbackId);
      if (el) {
        const original = el.textContent;
        el.textContent = "Copied!";
        setTimeout(() => (el.textContent = original), 1500);
      }
    };
    if (navigator.clipboard) navigator.clipboard.writeText(text).then(done).catch(done);
    else done();
  };

  window.handlePurchaseSubmit = async function (evt) {
    evt.preventDefault();
    const btn = document.getElementById("submitOrderBtn");
    const originalLabel = btn.textContent;
    btn.textContent = "Processing…";
    btn.disabled = true;
    try {
      const provider = document.getElementById("paymentProvider").value;
      const body = {
        email: document.getElementById("customerEmail").value.trim(),
        tier: document.getElementById("selectedTier").value,
        provider,
        price: document.getElementById("selectedPrice").value,
        txId: provider === "bkash" ? document.getElementById("bkashTxId").value.trim() : undefined,
        binanceTxId: provider === "binance" ? document.getElementById("binanceTxId").value.trim() : undefined,
      };
      const res = await fetch("/api/license/purchase", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok && data.ok) {
        document.getElementById("checkoutForm").classList.add("hidden-step");
        document.getElementById("checkoutSuccess").classList.remove("hidden-step");
        setText("generatedKeyDisplay", (data.license && data.license.key) || data.key || "Issued — check your email");
        setText("successSubtext", data.pending
          ? "Your payment is being verified. Your key will be emailed shortly."
          : "Activate this key in Telegram or inside the Android app:");
      } else {
        alert((data && data.description) || "Could not process order. Please verify your transaction ID and try again, or contact support on WhatsApp.");
      }
    } catch (err) {
      alert("Network error while submitting your order. Please try again or reach us on WhatsApp.");
    } finally {
      btn.textContent = originalLabel;
      btn.disabled = false;
    }
  };

  window.copyGeneratedKey = function () {
    const code = document.getElementById("generatedKeyDisplay");
    if (code) window.copyText(code.textContent, "generatedKeyDisplay");
  };

  /* ---------- License lookup ---------- */
  window.handleLookupSubmit = async function (evt) {
    evt.preventDefault();
    const q = document.getElementById("lookupQuery").value.trim();
    const resultEl = document.getElementById("lookupResult");
    if (!q) return;
    resultEl.classList.remove("hidden-step");
    resultEl.innerHTML = '<p class="mono" style="font-size:12px;color:var(--text-faint)">Looking up license…</p>';
    try {
      const res = await fetch("/api/license/lookup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query: q }),
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok && data.ok && data.license) {
        const l = data.license;
        resultEl.innerHTML = `<div class="pay-instructions" style="color:var(--success)"><b>Active license found.</b><br>Tier: ${l.tier || "—"} &middot; Devices: ${l.maxDevices || l.devices || "—"} &middot; Status: ${l.status || "active"}</div>`;
      } else {
        resultEl.innerHTML = '<div class="pay-instructions" style="color:var(--danger)">No active license found matching that email or key.</div>';
      }
    } catch (_) {
      resultEl.innerHTML = '<div class="pay-instructions" style="color:var(--danger)">Lookup failed — please try again.</div>';
    }
  };

  /* ---------- CISO / enterprise inquiry ---------- */
  window.handleCisoSubmit = async function (evt) {
    evt.preventDefault();
    const btn = document.getElementById("cisoSubmitBtn");
    const original = btn.textContent;
    btn.textContent = "Sending…";
    btn.disabled = true;
    try {
      const body = {
        name: document.getElementById("cisoName").value.trim(),
        email: document.getElementById("cisoEmail").value.trim(),
        org: document.getElementById("cisoOrg").value.trim(),
        scope: document.getElementById("cisoScope").value,
        notes: document.getElementById("cisoNotes").value.trim(),
      };
      const res = await fetch("/api/ciso-inquiry", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok && data.ok !== false) {
        document.getElementById("cisoForm").classList.add("hidden-step");
        document.getElementById("cisoSuccess").classList.remove("hidden-step");
      } else {
        alert((data && data.description) || "Could not submit inquiry. Please email us directly or use WhatsApp.");
      }
    } catch (_) {
      alert("Network error. Please try again or reach us on WhatsApp.");
    } finally {
      btn.textContent = original;
      btn.disabled = false;
    }
  };

  /* ---------- Generic modal helpers ---------- */
  function toggleModal(id, open) {
    const el = document.getElementById(id);
    if (!el) return;
    el.classList.toggle("open", open);
    document.body.style.overflow = open ? "hidden" : "";
  }
  window.openModal = (id) => toggleModal(id, true);
  window.closeModal = (id) => toggleModal(id, false);
  document.querySelectorAll(".modal-veil").forEach((veil) => {
    veil.addEventListener("click", (e) => {
      if (e.target === veil) toggleModal(veil.id, false);
    });
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") document.querySelectorAll(".modal-veil.open").forEach((v) => toggleModal(v.id, false));
  });

  /* ---------- Visitor counter ---------- */
  fetch("/api/stats/visitors")
    .then((r) => r.json())
    .then((d) => {
      if (d && d.ok) setText("visitorCount", Number(d.count).toLocaleString());
    })
    .catch(() => {});

  /* ---------- Init default scenario ---------- */
  window.setScenario("snatch");
})();
