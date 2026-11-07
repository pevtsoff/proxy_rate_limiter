"""Separate consumer group names from consumer types."""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20261107_000001"
down_revision: str | None = "20261107_000000"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

GROUP_TYPE_CONSTRAINT = "ck_consumer_groups_group_type"
VALID_GROUP_TYPES = ("premium", "regular", "free", "internal")


def upgrade() -> None:
    """Add a reusable type field while preserving existing groups."""
    inspector = sa.inspect(op.get_bind())
    columns = {column["name"] for column in inspector.get_columns("consumer_groups")}
    if "group_type" in columns:
        return
    add_group_type_column()
    copy_existing_types()
    drop_old_group_name_constraint(inspector)
    add_group_type_constraint()
    op.alter_column("consumer_groups", "group_type", nullable=False)
    op.alter_column("consumer_groups", "group_name", type_=sa.String(100))


def add_group_type_column() -> None:
    op.add_column(
        "consumer_groups",
        sa.Column("group_type", sa.String(20), nullable=True),
    )


def copy_existing_types() -> None:
    op.execute("UPDATE consumer_groups SET group_type = group_name")


def drop_old_group_name_constraint(inspector: sa.Inspector) -> None:
    for constraint in inspector.get_check_constraints("consumer_groups"):
        if "group_name" in (constraint.get("sqltext") or ""):
            op.drop_constraint(constraint["name"], "consumer_groups", type_="check")


def add_group_type_constraint() -> None:
    values = ", ".join(f"'{group_type}'" for group_type in VALID_GROUP_TYPES)
    op.create_check_constraint(
        GROUP_TYPE_CONSTRAINT,
        "consumer_groups",
        f"group_type IN ({values})",
    )


def downgrade() -> None:
    """Restore the original four-value group-name constraint."""
    op.drop_constraint(GROUP_TYPE_CONSTRAINT, "consumer_groups", type_="check")
    op.drop_column("consumer_groups", "group_type")
    op.alter_column("consumer_groups", "group_name", type_=sa.String(20))
    op.create_check_constraint(
        "consumer_groups_group_name_check",
        "consumer_groups",
        "group_name IN ('premium', 'regular', 'free', 'internal')",
    )
