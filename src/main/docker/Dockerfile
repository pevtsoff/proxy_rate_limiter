# syntax=docker/dockerfile:1.7
# One image for both services (proxy and target API); the `test` stage adds dev tools.
FROM python:3.13-slim AS base

RUN python -m pip install --no-cache-dir uv==0.5.11

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    UV_PROJECT_ENVIRONMENT=/opt/venv \
    PATH="/opt/venv/bin:${PATH}" \
    PYTHONPATH=/app/src/main

WORKDIR /app
COPY pyproject.toml uv.lock* ./


FROM base AS runtime
RUN --mount=type=cache,target=/root/.cache/uv uv sync --no-dev --no-install-project
RUN useradd --create-home --uid 10001 app
USER app
EXPOSE 8000


FROM base AS test
RUN --mount=type=cache,target=/root/.cache/uv uv sync --no-install-project
