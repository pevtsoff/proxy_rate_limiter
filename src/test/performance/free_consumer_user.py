from locust import task
from proxied_api_user import ProxiedApiUser


class FreeConsumerUser(ProxiedApiUser):
    """free tier: 10 requests/minute shared across all resources."""

    weight = 3
    consumer_ids = ("free-user-1", "free-user-2")

    @task(3)
    def read_item(self) -> None:
        resource = self.random_resource()
        self.call("GET", f"/{resource}/1", f"GET /{resource}/{{item_id}} [free]")

    @task(1)
    def create_item(self) -> None:
        resource = self.random_resource()
        self.call("POST", f"/{resource}", f"POST /{resource} [free]", json={"name": "x"})
