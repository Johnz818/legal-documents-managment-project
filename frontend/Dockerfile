FROM node:22-alpine AS builder

ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH

WORKDIR /workspace

RUN corepack enable \
    && corepack prepare pnpm@10.15.1 --activate

COPY package.json pnpm-lock.yaml ./
RUN --mount=type=cache,id=frontend-pnpm,target=/pnpm/store \
    pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM nginxinc/nginx-unprivileged:1.28-alpine

ENV LEGAL_API_UPSTREAM=http://host.docker.internal:8080
ENV NGINX_ENVSUBST_FILTER=LEGAL_API_UPSTREAM

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=builder /workspace/dist /usr/share/nginx/html

EXPOSE 8080
