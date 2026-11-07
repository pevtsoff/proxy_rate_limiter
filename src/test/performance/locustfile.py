"""Load test of the proxy rate limiter. Requires seeded data (`./seed.sh`).

Run: ./test.sh perf   (or `docker compose ... --profile perf up locust` for the web UI)
"""

from free_consumer_user import FreeConsumerUser
from premium_consumer_user import PremiumConsumerUser
from regular_consumer_user import RegularConsumerUser

__all__ = ["FreeConsumerUser", "PremiumConsumerUser", "RegularConsumerUser"]
