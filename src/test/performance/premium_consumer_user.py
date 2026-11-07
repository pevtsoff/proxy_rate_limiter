from locust import task
from proxied_api_user import ProxiedApiUser


class PremiumConsumerUser(ProxiedApiUser):
    """premium tier: 500/min overall, but GET /orders/{item_id} is capped at 3/second."""

    weight = 1
    consumer_ids = ("premium-user-1",)

    @task(5)
    def read_order(self) -> None:
        self.call("GET", "/orders/1", "GET /orders/{item_id} [premium]")

    @task(2)
    def replace_user(self) -> None:
        self.call("PUT", "/users/1", "PUT /users/{item_id} [premium]", json={"name": "z"})
