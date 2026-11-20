package com.example.ratelimiter.adapter.in.ui.view;

import com.example.ratelimiter.adapter.in.ui.MainLayout;
import com.example.ratelimiter.adapter.in.ui.component.CodedEnumFields;
import com.example.ratelimiter.adapter.in.ui.component.CrudToolbar;
import com.example.ratelimiter.adapter.in.ui.component.FormDialog;
import com.example.ratelimiter.adapter.in.ui.component.PagedGrid;
import com.example.ratelimiter.adapter.in.ui.component.UiActions;
import com.example.ratelimiter.application.port.in.ManagePolicyAssignmentsUseCase;
import com.example.ratelimiter.application.port.in.command.PolicyAssignmentCommand;
import com.example.ratelimiter.domain.model.AssignmentFilter;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyAssignment;
import com.example.ratelimiter.domain.model.RelationshipType;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** MUC1.5 / MUC2 / MUC6: assign policies to consumer groups per resource or resource group. */
@Route(value = "policy-assignments", layout = MainLayout.class)
@PageTitle("Policy assignments | Rate Limiter Admin")
public class PolicyAssignmentsView extends VerticalLayout {

    private final ManagePolicyAssignmentsUseCase assignments;
    private final AssignmentLookups lookups;
    private final ComboBox<ConsumerGroup> groupFilter = new ComboBox<>("Consumer group");
    private final ComboBox<RelationshipType> typeFilter =
            CodedEnumFields.comboBox("Relationship type", RelationshipType.class);
    private final PagedGrid<PolicyAssignment> pagedGrid;

    public PolicyAssignmentsView(
            ManagePolicyAssignmentsUseCase assignments, AssignmentLookups lookups) {
        this.assignments = assignments;
        this.lookups = lookups;
        this.pagedGrid = new PagedGrid<>(page -> assignments.list(currentFilter(), page));
        lookups.reload();
        configureColumns();
        configureFilters();
        CrudToolbar<PolicyAssignment> toolbar = new CrudToolbar<>(pagedGrid.grid(),
                () -> openForm(null), this::openForm, this::delete);
        setSizeFull();
        add(new H2("Policy assignments (consumer_groups_policies)"),
                new HorizontalLayout(groupFilter, typeFilter), toolbar, pagedGrid);
        pagedGrid.refresh();
    }

    private AssignmentFilter currentFilter() {
        ConsumerGroup group = groupFilter.getValue();
        return new AssignmentFilter(group == null ? null : group.id(), typeFilter.getValue(), null);
    }

    private void configureColumns() {
        pagedGrid.grid().addColumn(PolicyAssignment::id).setHeader("ID").setAutoWidth(true);
        pagedGrid.grid().addColumn(a -> lookups.groupLabel(a.consumerGroupId()))
                .setHeader("Consumer group");
        pagedGrid.grid().addColumn(a -> a.relationshipType().value()).setHeader("Type");
        pagedGrid.grid().addColumn(lookups::targetLabel).setHeader("Target").setAutoWidth(true);
        pagedGrid.grid().addColumn(a -> lookups.policyLabel(a.policyId())).setHeader("Policy");
    }

    private void configureFilters() {
        groupFilter.setItems(lookups.groups());
        groupFilter.setItemLabelGenerator(group -> group.name().value());
        groupFilter.setClearButtonVisible(true);
        groupFilter.addValueChangeListener(event -> pagedGrid.firstPage());
        typeFilter.addValueChangeListener(event -> pagedGrid.firstPage());
    }

    private void openForm(PolicyAssignment existing) {
        lookups.reload();
        AssignmentForm form = new AssignmentForm(lookups, existing);
        Runnable save = () -> save(existing, form.toCommand());
        new FormDialog(existing == null ? "New assignment" : "Edit assignment", save,
                pagedGrid::refresh).withFields(form.fields()).open();
    }

    private void save(PolicyAssignment existing, PolicyAssignmentCommand command) {
        if (existing == null) {
            assignments.create(command);
        } else {
            assignments.update(existing.id(), command);
        }
    }

    private void delete(PolicyAssignment assignment) {
        if (UiActions.run(() -> assignments.delete(assignment.id()), "Assignment deleted")) {
            pagedGrid.refresh();
        }
    }

    /** Fields of the assignment dialog; the target list follows the relationship type. */
    private static final class AssignmentForm {

        private final ComboBox<ConsumerGroup> group = new ComboBox<>("Consumer group");
        private final ComboBox<RelationshipType> type =
                CodedEnumFields.comboBox("Relationship type", RelationshipType.class);
        private final ComboBox<AssignmentLookups.Target> target = new ComboBox<>("Target");
        private final ComboBox<Policy> policy = new ComboBox<>("Policy");

        private AssignmentForm(AssignmentLookups lookups, PolicyAssignment existing) {
            group.setItems(lookups.groups());
            group.setItemLabelGenerator(item -> item.name().value());
            policy.setItems(lookups.policies());
            policy.setItemLabelGenerator(Policy::name);
            target.setItemLabelGenerator(AssignmentLookups.Target::label);
            type.addValueChangeListener(
                    event -> target.setItems(lookups.targets(event.getValue())));
            type.setValue(existing == null
                    ? RelationshipType.RESOURCE : existing.relationshipType());
            if (existing != null) {
                group.setValue(lookups.group(existing.consumerGroupId()));
                policy.setValue(lookups.policy(existing.policyId()));
                target.setValue(lookups.target(existing.relationshipType(), existing.targetId()));
            }
        }

        private ComboBox<?>[] fields() {
            return new ComboBox<?>[] {group, type, target, policy};
        }

        private PolicyAssignmentCommand toCommand() {
            return new PolicyAssignmentCommand(
                    group.getValue() == null ? null : group.getValue().id(),
                    type.getValue(),
                    policy.getValue() == null ? null : policy.getValue().id(),
                    target.getValue() == null ? null : target.getValue().id());
        }
    }
}
