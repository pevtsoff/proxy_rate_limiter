"""Base locust user: obtains a bearer token for its consumer and calls the target API
through the proxy. 429 responses are reported as failures named "rate limited" so the
limiter's work is visible in the statistics."""

import random

from locust import HttpUser, between

RESOURCES = ("orders", "users", "carts")
RATE_LIMITED = 429


class ProxiedApiUser(HttpUser):
    abstract = True
    wait_time = between(0.05, 0.2)
    consumer_ids: tuple[str, ...] = ()

    def on_start(self) -> None:
        consumer_id = random.choice(self.consumer_ids)
        response = self.client.post(
            "/admin/tokens", json={"consumer_id": consumer_id}, name="/admin/tokens"
        )
        response.raise_for_status()
        self.client.headers["Authorization"] = f"Bearer {response.json()['access_token']}"

    def call(self, method: str, path: str, name: str, json: dict | None = None) -> None:
        with self.client.request(
            method, path, json=json, name=name, catch_response=True
        ) as response:
            if response.status_code == RATE_LIMITED:
                response.failure("rate limited")
            elif response.status_code >= 400:
                response.failure(f"unexpected status {response.status_code}")
            else:
                response.success()

    @staticmethod
    def random_resource() -> str:
        return random.choice(RESOURCES)
