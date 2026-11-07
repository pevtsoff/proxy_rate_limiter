"""Logging setup shared by every proxy process.

The format carries module, function and line number; tracebacks are appended by
`logger.exception` / `exc_info`.
"""

import logging

LOG_FORMAT = "%(asctime)s %(levelname)s [%(name)s] %(module)s.%(funcName)s:%(lineno)d - %(message)s"


def configure_logging(level: str) -> None:
    """Configure the root logger once per process."""
    logging.basicConfig(level=level.upper(), format=LOG_FORMAT, force=True)
