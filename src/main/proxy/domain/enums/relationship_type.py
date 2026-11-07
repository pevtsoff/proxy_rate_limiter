from enum import StrEnum


class RelationshipType(StrEnum):
    """What a consumer group policy assignment targets.

    `all_resources` is an addition to the spec: it lets one policy cover every
    target resource of a consumer group (use case "one policy for ALL resources")
    without forcing every resource into a single resource group.
    """

    RESOURCE = "resource"
    RESOURCE_GROUP = "resource_group"
    ALL_RESOURCES = "all_resources"
