from locust import task
from proxied_api_user import ProxiedApiUser


class RegularConsumerUser(ProxiedApiUser):
    """regular tier: 100/min per resource group, 5/min for POST /carts, default elsewhere."""

    weight = 2
    consumer_ids = ("regular-user-1", "regular-user-2")

    @task(3)
    def read_order(self) -> None:
        self.call("GET", "/orders/1", "GET /orders/{item_id} [regular]")

    @task(1)
    def add_to_cart(self) -> None:
        self.call("POST", "/carts", "POST /carts [regular]", json={"name": "x"})

    @task(1)
    def update_user(self) -> None:
        self.call("PATCH", "/users/1", "PATCH /users/{item_id} [regular]", json={"name": "y"})
