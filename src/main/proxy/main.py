"""ASGI entrypoint: `uvicorn proxy.main:app`."""

from proxy.app_factory import create_app

app = create_app()
