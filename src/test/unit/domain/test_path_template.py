import pytest

from proxy.domain.value_objects.path_template import PathTemplate, normalize_path


@pytest.mark.parametrize(
    ("template", "path", "expected"),
    [
        ("/orders", "/orders", True),
        ("/orders", "/orders/", True),
        ("/orders/{item_id}", "/orders/1", True),
        ("/orders/{item_id}", "/orders/abc", True),
        ("/orders/{item_id}", "/orders", False),
        ("/orders/{item_id}", "/orders/1/lines", False),
        ("/orders/{item_id}/lines/{line}", "/orders/1/lines/2", True),
        ("/orders", "/users", False),
        ("/orders.json", "/ordersXjson", False),
    ],
)
def test_template_matching(template: str, path: str, expected: bool) -> None:
    assert PathTemplate(template).matches(path) is expected


def test_parameter_count() -> None:
    assert PathTemplate("/a/{x}/b/{y}").parameter_count == 2
    assert PathTemplate("/a").parameter_count == 0


@pytest.mark.parametrize(("raw", "normalized"), [("orders/", "/orders"), ("/", "/"), ("", "/")])
def test_normalize_path(raw: str, normalized: str) -> None:
    assert normalize_path(raw) == normalized
