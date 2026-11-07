"""initial schema

Revision ID: 0001
Revises:
Create Date: 2026-11-07 18:08:27.808725
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "0001"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:

    op.create_table(
        "consumer_groups",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column(
            "group_name",
            sa.String(length=32),
            nullable=False,
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_consumer_groups")),
        sa.UniqueConstraint("group_name", name=op.f("uq_consumer_groups_group_name")),
    )
    op.create_table(
        "policies",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column(
            "rate_limiting_strategy",
            sa.Enum(
                "fixed-window",
                "moving-window",
                "sliding-window-counter",
                name="rate_limiting_strategy",
                native_enum=False,
                create_constraint=True,
                length=32,
            ),
            server_default="moving-window",
            nullable=False,
        ),
        sa.Column("number_of_requests", sa.Integer(), nullable=False),
        sa.Column(
            "time_period",
            sa.Enum(
                "second",
                "minute",
                "hour",
                "day",
                "week",
                name="time_period",
                native_enum=False,
                create_constraint=True,
                length=32,
            ),
            nullable=False,
        ),
        sa.Column("is_default", sa.Boolean(), server_default=sa.false(), nullable=False),
        sa.CheckConstraint(
            "number_of_requests > 0", name=op.f("ck_policies_positive_number_of_requests")
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_policies")),
        sa.UniqueConstraint("name", name=op.f("uq_policies_name")),
    )
    op.create_index(
        "uq_policies_single_default",
        "policies",
        ["is_default"],
        unique=True,
        postgresql_where=sa.text("is_default"),
        sqlite_where=sa.text("is_default"),
    )
    op.create_table(
        "resource_groups",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("description", sa.String(length=500), nullable=True),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_resource_groups")),
        sa.UniqueConstraint("name", name=op.f("uq_resource_groups_name")),
    )
    op.create_table(
        "consumers",
        sa.Column("id", sa.String(length=128), nullable=False),
        sa.Column("consumer_group_id", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(
            ["consumer_group_id"],
            ["consumer_groups.id"],
            name=op.f("fk_consumers_consumer_group_id_consumer_groups"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_consumers")),
    )
    op.create_index(
        op.f("ix_consumers_consumer_group_id"), "consumers", ["consumer_group_id"], unique=False
    )
    op.create_table(
        "resources",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column(
            "method",
            sa.Enum(
                "GET",
                "HEAD",
                "OPTIONS",
                "TRACE",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "CONNECT",
                name="http_method",
                native_enum=False,
                create_constraint=True,
                length=32,
            ),
            nullable=False,
        ),
        sa.Column("resource_path", sa.String(length=500), nullable=False),
        sa.Column(
            "io_type",
            sa.Enum(
                "read",
                "write",
                name="io_type",
                native_enum=False,
                create_constraint=True,
                length=32,
            ),
            nullable=False,
        ),
        sa.Column("resource_group_id", sa.Integer(), nullable=True),
        sa.ForeignKeyConstraint(
            ["resource_group_id"],
            ["resource_groups.id"],
            name=op.f("fk_resources_resource_group_id_resource_groups"),
            ondelete="SET NULL",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_resources")),
        sa.UniqueConstraint("method", "resource_path", name=op.f("uq_resources_method")),
    )
    op.create_index(
        op.f("ix_resources_resource_group_id"), "resources", ["resource_group_id"], unique=False
    )
    op.create_table(
        "consumer_groups_policies",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("consumer_group_id", sa.Integer(), nullable=False),
        sa.Column(
            "relationship_type",
            sa.Enum(
                "resource",
                "resource_group",
                "all_resources",
                name="relationship_type",
                native_enum=False,
                create_constraint=True,
                length=32,
            ),
            nullable=False,
        ),
        sa.Column("policy_id", sa.Integer(), nullable=False),
        sa.Column("resource_id", sa.Integer(), nullable=True),
        sa.Column("resource_group_id", sa.Integer(), nullable=True),
        sa.CheckConstraint(
            "(relationship_type = 'resource' AND resource_id IS NOT NULL AND resource_group_id IS NULL) OR (relationship_type = 'resource_group' AND resource_group_id IS NOT NULL AND resource_id IS NULL) OR (relationship_type = 'all_resources' AND resource_id IS NULL AND resource_group_id IS NULL)",
            name=op.f("ck_consumer_groups_policies_target_matches_relationship"),
        ),
        sa.ForeignKeyConstraint(
            ["consumer_group_id"],
            ["consumer_groups.id"],
            name=op.f("fk_consumer_groups_policies_consumer_group_id_consumer_groups"),
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["policy_id"],
            ["policies.id"],
            name=op.f("fk_consumer_groups_policies_policy_id_policies"),
            ondelete="RESTRICT",
        ),
        sa.ForeignKeyConstraint(
            ["resource_group_id"],
            ["resource_groups.id"],
            name=op.f("fk_consumer_groups_policies_resource_group_id_resource_groups"),
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["resource_id"],
            ["resources.id"],
            name=op.f("fk_consumer_groups_policies_resource_id_resources"),
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id", name=op.f("pk_consumer_groups_policies")),
        sa.UniqueConstraint(
            "consumer_group_id", "resource_group_id", name="uq_cgp_group_resource_group"
        ),
        sa.UniqueConstraint("consumer_group_id", "resource_id", name="uq_cgp_group_resource"),
    )
    op.create_index(
        op.f("ix_consumer_groups_policies_consumer_group_id"),
        "consumer_groups_policies",
        ["consumer_group_id"],
        unique=False,
    )
    op.create_index(
        "uq_cgp_group_all_resources",
        "consumer_groups_policies",
        ["consumer_group_id"],
        unique=True,
        postgresql_where=sa.text("relationship_type = 'all_resources'"),
        sqlite_where=sa.text("relationship_type = 'all_resources'"),
    )


def downgrade() -> None:

    op.drop_index(
        "uq_cgp_group_all_resources",
        table_name="consumer_groups_policies",
        postgresql_where=sa.text("relationship_type = 'all_resources'"),
        sqlite_where=sa.text("relationship_type = 'all_resources'"),
    )
    op.drop_index(
        op.f("ix_consumer_groups_policies_consumer_group_id"), table_name="consumer_groups_policies"
    )
    op.drop_table("consumer_groups_policies")
    op.drop_index(op.f("ix_resources_resource_group_id"), table_name="resources")
    op.drop_table("resources")
    op.drop_index(op.f("ix_consumers_consumer_group_id"), table_name="consumers")
    op.drop_table("consumers")
    op.drop_table("resource_groups")
    op.drop_index(
        "uq_policies_single_default",
        table_name="policies",
        postgresql_where=sa.text("is_default"),
        sqlite_where=sa.text("is_default"),
    )
    op.drop_table("policies")
    op.drop_table("consumer_groups")
