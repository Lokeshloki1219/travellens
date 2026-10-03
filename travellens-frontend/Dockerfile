# syntax=docker/dockerfile:1

# ---------- Stage 1: build the static bundle ----------
# Node and the 56 MB node_modules tree stay in this stage and are discarded.
FROM node:20-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- Stage 2: runtime ----------
# nginx serves the built assets and reverse-proxies the API, exactly as it did
# on the VM in Milestone 2 - but now it is part of the image.
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
  CMD wget -q --spider http://127.0.0.1/ || exit 1
