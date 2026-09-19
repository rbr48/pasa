# PASA VPS Hardening Checklist (personal deployment)

A short, practical checklist for running the PASA control plane safely on your own
VPS. It assumes the current setup: Node/Express on `127.0.0.1:8160`, fronted by
nginx with Certbot TLS (`pasa.nginx.conf`), storing device data, GPS history and
uploaded photos/audio/video as plain files under `pasa-server/data/` and
`pasa-server/uploads/`.

Ordered roughly by impact. Items marked **[done]** already exist in the repo.

## 1. Secrets
- [ ] Set a strong `ADMIN_SECRET` in `.env` (32+ random chars). If unset, the server
      auto-generates one into `data/admin_secret.json` on first boot — fine, but read
      it once and store it in a password manager, then keep the file private.
- [ ] Keep the Telegram `BOT_TOKEN` in `.env` only. Never commit `.env`
      (already covered by `.gitignore`).
- [ ] `chmod 600 .env data/admin_secret.json data/server_signing_key.json` so only
      your user can read them.

## 2. Transport & exposure
- **[done]** TLS via Certbot; HTTP redirects to HTTPS; HSTS enabled.
- **[done]** Node binds behind nginx; app port `8160` is proxied, not public.
- [ ] Confirm the OS firewall only exposes 22, 80, 443:
      `sudo ufw allow 22,80,443/tcp && sudo ufw enable`.
      Make sure `8160` and `8165` are **not** open to the internet.
- [ ] Restrict `/admin` and `/api/admin/*` to your IP (or put them behind a VPN /
      nginx `allow`/`deny`). These endpoints expose device locations and media.

## 3. Data at rest (your personal photos, audio, GPS)
- [ ] The `data/` and `uploads/` dirs hold your own captures in the clear. At minimum
      `chmod 700` them so only your user can read.
- [ ] Prefer a VPS with full-disk encryption, or keep `uploads/` on an encrypted
      volume (LUKS). If the host is seized or a snapshot leaks, this is what protects
      your media.
- [ ] Add a retention job (cron) that deletes uploads older than N days — you rarely
      need old intruder photos, and less stored data = less to lose.
- [ ] Back up `data/*.json` (devices, signing key, licenses) somewhere private; losing
      `server_signing_key.json` breaks command signing for enrolled devices.

## 4. Application hardening (nice-to-have for a single-user server)
- **[done]** Ed25519 command signing; ES256 device-proof auth on `/api/device/*`.
- [ ] Tighten CORS. `server.js` uses `app.use(cors())` (allow-all). For a personal
      deployment you can restrict it to your own origin, e.g.
      `cors({ origin: 'https://pasa.izhaanintellect.fun' })`, or drop it entirely if
      only the app and your admin page call the API.
- [ ] Add basic rate limiting on `/api/admin/verify` and `/api/device/register`
      (e.g. `express-rate-limit`) to blunt brute-force against the admin secret.
- [ ] Consider `helmet()` on the Express app as defense-in-depth (nginx already adds
      headers, but this covers any direct-to-Node path).
- [ ] If you're the only user, disable/remove the public `/api/license/purchase`
      free-key path — it isn't needed for personal use and shouldn't be reachable.

## 5. Operations
- [ ] Run Node under a non-root user via systemd or `pm2`, with auto-restart.
- [ ] Keep the OS patched: `unattended-upgrades` (Debian/Ubuntu) or equivalent.
- [ ] Ship logs off-box or rotate them; `logSecurityEvent` writes to
      `data/security_logs.json`, so include it in log review.
- [ ] Use SSH keys only, disable password login (`PasswordAuthentication no`),
      and disable root SSH (`PermitRootLogin no`).

## Quick wins (do these first)
1. Strong `ADMIN_SECRET` + `chmod 600` the secret files.
2. Firewall to 22/80/443 only; lock `/admin` to your IP.
3. `chmod 700 data/ uploads/` and put `uploads/` on an encrypted volume.
4. Restrict CORS and add rate limiting on the admin/register endpoints.
