from enum import StrEnum

_DAYS_PER_WEEK = 7


class TimePeriod(StrEnum):
    """Window length of a rate limiting policy."""

    SECOND = "second"
    MINUTE = "minute"
    HOUR = "hour"
    DAY = "day"
    WEEK = "week"

    def to_limits_window(self) -> tuple[int, str]:
        """Return `(multiples, granularity)` understood by the `limits` library.

        `limits` has no weekly granularity, so a week is expressed as 7 days.
        """
        if self is TimePeriod.WEEK:
            return _DAYS_PER_WEEK, TimePeriod.DAY.value
        return 1, self.value
