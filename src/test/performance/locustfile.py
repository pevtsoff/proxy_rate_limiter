"""Load test of the Proxy Rate Limiter (run with ./test.sh perf or ./test.sh perf-ui).

Each simulated user is one demo consumer from db/seed/seed.sql. It obtains a bearer token from
the admin token endpoint on start and then calls the target endpoints through the proxy.
HTTP 429 is an *expected* outcome (the limiter doing its job), so it is reported as success
under a separate "[429]" name; anything else non-2xx is a failure.
"""
import os
import random

from locust import HttpUser, between, task

ADMIN_AUTH = (os.environ.get("ADMIN_USERNAME", "admin"), os.environ.get("ADMIN_PASSWORD", "admin"))
RESOURCES = ("orders", "users", "carts")


class ProxyConsumer(HttpUser):
    abstract = True
    consumer_id = ""
    wait_time = between(0.05, 0.2)

    def on_start(self):
        response = self.client.post("/admin/api/tokens", json={"consumer_id": self.consumer_id},
                                    auth=ADMIN_AUTH, name="admin: issue token")
        response.raise_for_status()
        self.client.headers["Authorization"] = "Bearer " + response.json()["access_token"]

    def _call(self, method, path, name, **kwargs):
        with self.client.request(method, path, name=name, catch_response=True, **kwargs) as resp:
            if resp.status_code == 429:
                resp.request_meta["name"] = name + " [429]"
                resp.success()
            elif resp.status_code >= 400:
                resp.failure(f"unexpected HTTP {resp.status_code}")

    @task(6)
    def read_item(self):
        resource = random.choice(RESOURCES)
        self._call("GET", f"/{resource}/{random.randint(1, 1000)}", f"GET /{resource}/{{id}}")

    @task(2)
    def list_items(self):
        resource = random.choice(RESOURCES)
        self._call("GET", f"/{resource}", f"GET /{resource}")

    @task(1)
    def create_item(self):
        resource = random.choice(RESOURCES)
        self._call("POST", f"/{resource}", f"POST /{resource}", json={"source": "locust"})

    @task(1)
    def patch_item(self):
        resource = random.choice(RESOURCES)
        self._call("PATCH", f"/{resource}/{random.randint(1, 1000)}",
                   f"PATCH /{resource}/{{id}}", json={"qty": 1})


class PremiumConsumer(ProxyConsumer):
    weight = 1
    consumer_id = "premium-user-1"


class RegularConsumer(ProxyConsumer):
    weight = 3
    consumer_id = "regular-user-1"


class FreeConsumer(ProxyConsumer):
    weight = 5
    consumer_id = "free-user-1"


class InternalService(ProxyConsumer):
    weight = 1
    consumer_id = "internal-service-1"
