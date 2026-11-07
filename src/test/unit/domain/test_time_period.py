import pytest

from proxy.domain.enums.time_period import TimePeriod


@pytest.mark.parametrize(
    ("period", "window"),
    [
        (TimePeriod.SECOND, (1, "second")),
        (TimePeriod.MINUTE, (1, "minute")),
        (TimePeriod.HOUR, (1, "hour")),
        (TimePeriod.DAY, (1, "day")),
        (TimePeriod.WEEK, (7, "day")),
    ],
)
def test_period_maps_to_limits_window(period: TimePeriod, window: tuple[int, str]) -> None:
    assert period.to_limits_window() == window


def test_spec_periods_are_exactly_second_minute_hour_day_week() -> None:
    assert [p.value for p in TimePeriod] == ["second", "minute", "hour", "day", "week"]
