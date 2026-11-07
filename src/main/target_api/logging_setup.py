import logging

LOG_FORMAT = "%(asctime)s %(levelname)s [%(name)s] %(module)s.%(funcName)s:%(lineno)d - %(message)s"


def configure_logging(level: str) -> None:
    logging.basicConfig(level=level.upper(), format=LOG_FORMAT, force=True)
