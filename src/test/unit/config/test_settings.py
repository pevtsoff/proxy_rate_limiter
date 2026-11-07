import pytest

from proxy.config.settings import Settings


def test_lru_cache_defaults_are_capacity_1000_and_ttl_3600() -> None:
    settings = Settings(_env_file=None)
    assert settings.lru_cache_capacity == 1000
    assert settings.lru_cache_ttl == 3600


def test_lru_cache_is_configured_from_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("LRU_CACHE_CAPACITY", "5")
    monkeypatch.setenv("LRU_CACHE_TTL", "60")
    settings = Settings(_env_file=None)
    assert (settings.lru_cache_capacity, settings.lru_cache_ttl) == (5, 60)


def test_default_policy_uses_moving_window() -> None:
    assert Settings(_env_file=None).default_policy_strategy.value == "moving-window"
