package com.example.ratelimiter.adapter.in.ui.view;

import com.example.ratelimiter.adapter.in.ui.MainLayout;
import com.example.ratelimiter.adapter.in.ui.component.CrudToolbar;
import com.example.ratelimiter.adapter.in.ui.component.FormDialog;
import com.example.ratelimiter.adapter.in.ui.component.Lookups;
import com.example.ratelimiter.adapter.in.ui.component.PagedGrid;
import com.example.ratelimiter.adapter.in.ui.component.UiActions;
import com.example.ratelimiter.application.port.in.ManageConsumerGroupsUseCase;
import com.example.ratelimiter.application.port.in.ManageConsumersUseCase;
import com.example.ratelimiter.application.port.in.command.ConsumerCommand;
import com.example.ratelimiter.domain.model.Consumer;
import com.example.ratelimiter.domain.model.ConsumerFilter;
import com.example.ratelimiter.domain.model.ConsumerGroup;
import com.example.ratelimiter.domain.model.PageQuery;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** MUC1 / MUC6: CRUD of consumers with filtering by id and consumer group. */
@Route(value = "consumers", layout = MainLayout.class)
@PageTitle("Consumers | Rate Limiter Admin")
public class ConsumersView extends VerticalLayout {

    private final ManageConsumersUseCase consumers;
    private final ManageConsumerGroupsUseCase groups;
    private final TextField idFilter = new TextField("Consumer id contains");
    private final ComboBox<ConsumerGroup> groupFilter = new ComboBox<>("Consumer group");
    private final PagedGrid<Consumer> pagedGrid;
    private Map<Long, ConsumerGroup> groupsById = Map.of();

    public ConsumersView(ManageConsumersUseCase consumers, ManageConsumerGroupsUseCase groups) {
        this.consumers = consumers;
        this.groups = groups;
        this.pagedGrid = new PagedGrid<>(page -> consumers.list(currentFilter(), page));
        configureColumns();
        configureFilters();
        CrudToolbar<Consumer> toolbar = new CrudToolbar<>(pagedGrid.grid(),
                () -> openForm(null), this::openForm, this::delete);
        setSizeFull();
        add(new H2("Consumers"), new HorizontalLayout(idFilter, groupFilter), toolbar, pagedGrid);
        reloadGroups();
        pagedGrid.refresh();
    }

    private ConsumerFilter currentFilter() {
        ConsumerGroup group = groupFilter.getValue();
        return new ConsumerFilter(idFilter.getValue(), group == null ? null : group.id());
    }

    private void configureColumns() {
        pagedGrid.grid().addColumn(Consumer::id).setHeader("ID").setAutoWidth(true);
        pagedGrid.grid().addColumn(Consumer::consumerId).setHeader("Consumer id");
        pagedGrid.grid().addColumn(consumer -> Lookups.label(groupsById,
                consumer.consumerGroupId(), group -> group.name().value())).setHeader("Group");
    }

    private void configureFilters() {
        idFilter.setValueChangeMode(ValueChangeMode.LAZY);
        idFilter.addValueChangeListener(event -> pagedGrid.firstPage());
        groupFilter.setItemLabelGenerator(group -> group.name().value());
        groupFilter.setClearButtonVisible(true);
        groupFilter.addValueChangeListener(event -> pagedGrid.firstPage());
    }

    private void reloadGroups() {
        groupsById = groups.list(PageQuery.all()).content().stream()
                .collect(Collectors.toMap(ConsumerGroup::id, Function.identity()));
        groupFilter.setItems(groupsById.values());
    }

    private void openForm(Consumer existing) {
        TextField consumerId = new TextField("Consumer id");
        ComboBox<ConsumerGroup> group = new ComboBox<>("Consumer group", groupsById.values());
        group.setItemLabelGenerator(item -> item.name().value());
        if (existing != null) {
            consumerId.setValue(existing.consumerId());
            group.setValue(groupsById.get(existing.consumerGroupId()));
        }
        Runnable save = () -> save(existing, new ConsumerCommand(consumerId.getValue(),
                group.getValue() == null ? null : group.getValue().id()));
        new FormDialog(existing == null ? "New consumer" : "Edit consumer", save,
                pagedGrid::refresh)
                .withFields(consumerId, group)
                .open();
    }

    private void save(Consumer existing, ConsumerCommand command) {
        if (existing == null) {
            consumers.create(command);
        } else {
            consumers.update(existing.id(), command);
        }
    }

    private void delete(Consumer consumer) {
        if (UiActions.run(() -> consumers.delete(consumer.id()), "Consumer deleted")) {
            pagedGrid.refresh();
        }
    }
}
