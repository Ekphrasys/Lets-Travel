FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine

RUN apk add --no-cache curl

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY ssl/ /etc/nginx/ssl/
COPY --from=build /app/dist/travel-admin/browser /usr/share/nginx/html

EXPOSE 4200
HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=20s \
  CMD curl -sfk https://localhost:4200/ >/dev/null || exit 1
