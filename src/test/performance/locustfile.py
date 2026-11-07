"""Simple load profile for the proxied example API."""

from locust import HttpUser, task


class ProxyConsumer(HttpUser):
    """Generate authenticated calls through the proxy."""

    @task
    def get_orders(self) -> None:
        self.client.get("/orders/1", headers={"Authorization": "Bearer demo-consumer"})
