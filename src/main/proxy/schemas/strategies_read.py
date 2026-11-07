from pydantic import BaseModel


class StrategiesRead(BaseModel):
    strategies: list[str]
    default: str
