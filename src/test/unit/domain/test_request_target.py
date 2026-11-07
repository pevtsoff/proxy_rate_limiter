from proxy.domain.enums.http_method import HttpMethod
from proxy.domain.value_objects.consumer_id import ConsumerId
from proxy.domain.value_objects.request_target import RequestTarget


def test_resource_label_has_no_leading_slash() -> None:
    assert RequestTarget(HttpMethod.GET, "/orders/1/").resource_label == "orders/1"


def test_cache_key_combines_consumer_method_and_path() -> None:
    target = RequestTarget(HttpMethod.PUT, "orders/1")
    assert target.cache_key(ConsumerId("alice")) == "alice|PUT|/orders/1"
