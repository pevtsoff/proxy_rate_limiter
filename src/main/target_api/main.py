"""ASGI entrypoint: `uvicorn target_api.main:app`."""

from target_api.app_factory import create_app

app = create_app()
