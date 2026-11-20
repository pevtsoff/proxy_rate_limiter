package com.example.ratelimiter.adapter.in.ui.view;

import com.example.ratelimiter.adapter.in.ui.MainLayout;
import com.example.ratelimiter.adapter.in.ui.component.CodedEnumFields;
import com.example.ratelimiter.adapter.in.ui.component.CrudToolbar;
import com.example.ratelimiter.adapter.in.ui.component.FormDialog;
import com.example.ratelimiter.adapter.in.ui.component.PagedGrid;
import com.example.ratelimiter.adapter.in.ui.component.UiActions;
import com.example.ratelimiter.application.port.in.ManageConsumerGroupsUseCase;
import com.example.ratelimiter.application.port.in.command.CreateConsumerGroupCommand;
import com.example.ratelimiter.application.port.in.command.UpdateConsumerGroupCommand;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.ConsumerGroupName;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Arrays;
import java.util.List;

/** MUC1 / MUC6: CRUD of consumer groups; new groups are created with their first consumers. */
@Route(value = "consumer-groups", layout = MainLayout.class)
@PageTitle("Consumer groups | Rate Limiter Admin")
public class ConsumerGroupsView extends VerticalLayout {

    private final ManageConsumerGroupsUseCase groups;
    private final PagedGrid<ConsumerGroup> pagedGrid;

    public ConsumerGroupsView(ManageConsumerGroupsUseCase groups) {
        this.groups = groups;
        this.pagedGrid = new PagedGrid<>(groups::list);
        pagedGrid.grid().addColumn(ConsumerGroup::id).setHeader("ID").setAutoWidth(true);
        pagedGrid.grid().addColumn(group -> group.name().value()).setHeader("Group name");
        CrudToolbar<ConsumerGroup> toolbar = new CrudToolbar<>(pagedGrid.grid(),
                this::openCreateForm, this::openEditForm, this::delete);
        setSizeFull();
        add(new H2("Consumer groups"), toolbar, pagedGrid);
        pagedGrid.refresh();
    }

    private void openCreateForm() {
        ComboBox<ConsumerGroupName> name =
                CodedEnumFields.comboBox("Group name", ConsumerGroupName.class);
        TextArea consumerIds = new TextArea("Initial consumer ids (comma or newline separated)");
        Runnable save = () -> groups.create(
                new CreateConsumerGroupCommand(name.getValue(), parseIds(consumerIds.getValue())));
        new FormDialog("New consumer group", save, pagedGrid::refresh)
                .withFields(name, consumerIds)
                .open();
    }

    private void openEditForm(ConsumerGroup existing) {
        ComboBox<ConsumerGroupName> name =
                CodedEnumFields.comboBox("Group name", ConsumerGroupName.class);
        name.setValue(existing.name());
        Runnable save = () -> groups.update(existing.id(),
                new UpdateConsumerGroupCommand(name.getValue()));
        new FormDialog("Edit consumer group", save, pagedGrid::refresh).withFields(name).open();
    }

    private void delete(ConsumerGroup group) {
        if (UiActions.run(() -> groups.delete(group.id()),
                "Consumer group deleted with its consumers and assignments")) {
            pagedGrid.refresh();
        }
    }

    private static List<String> parseIds(String text) {
        return Arrays.stream(text.split("[,\\s]+"))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
    }
}
