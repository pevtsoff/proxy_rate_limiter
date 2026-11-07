"""Create the initial application schema."""

from collections.abc import Sequence

from alembic import op

from app.models.base import Base
from app.models import entities  # noqa: F401

revision: str = "20261107_000000"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Create missing tables, adopting databases initialized by the old startup flow."""
    bind = op.get_bind()
    Base.metadata.create_all(bind=bind, checkfirst=True)


def downgrade() -> None:
    """Drop the application schema."""
    bind = op.get_bind()
    Base.metadata.drop_all(bind=bind, checkfirst=True)
