# frontend/Dockerfile — Step 32 · ship the SPA as a container (multi-stage).
# Stage 1 needs the whole Node toolchain (~180 MB) to BUILD; stage 2 ships only nginx + ~150 kB of static
# files. The toolchain never reaches production — smaller image, smaller attack surface.
# Versions pinned per VERSIONS.md (never :latest).

# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM node:22.20.0-alpine AS build
WORKDIR /app

# Lockfile first → `npm ci` layer is cached until dependencies actually change (not on every source edit).
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# '' = same-origin relative fetches: the app calls /api/auth/…, /bank/… on WHATEVER origin serves it — the
# gateway. (Vite inlines this at BUILD time; it is not a runtime setting.)
ENV VITE_API_BASE_URL=""
RUN npm run build

# ── Stage 2: serve ──────────────────────────────────────────────────────────
FROM nginx:1.28.3-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
# The base image's CMD ["nginx", "-g", "daemon off;"] is what we want — no override needed.
