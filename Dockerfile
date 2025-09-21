FROM oven/bun:1.2-alpine AS build

WORKDIR /usr/src/app
COPY . /usr/src/app
RUN bun install --frozen-lockfile
RUN bun run build

FROM alpine:latest

WORKDIR /var/www/localhost/htdocs
RUN apk update && apk add lighttpd curl

COPY --from=build /usr/src/app/dist .

EXPOSE 80
HEALTHCHECK --interval=5s --timeout=3s --retries=3 CMD curl --fail http://localhost || exit 1
CMD ["lighttpd", "-D", "-f", "/etc/lighttpd/lighttpd.conf"]