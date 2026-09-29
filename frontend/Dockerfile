# ---------- build ----------
FROM node:22-alpine AS build

WORKDIR /build

# dependencias numa camada propria: o cache so invalida quando o lock mudar
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- runtime ----------
FROM nginx:1.27-alpine

# o Angular 21 gera os estaticos em dist/<projeto>/browser
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
