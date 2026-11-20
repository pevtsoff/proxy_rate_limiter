package com.example.ratelimiter.adapter.in.ui.view;

import com.example.ratelimiter.adapter.in.ui.MainLayout;
import com.example.ratelimiter.adapter.in.ui.component.CodedEnumFields;
import com.example.ratelimiter.adapter.in.ui.component.CrudToolbar;
import com.example.ratelimiter.adapter.in.ui.component.FormDialog;
import com.example.ratelimiter.adapter.in.ui.component.PagedGrid;
import com.example.ratelimiter.adapter.in.ui.component.UiActions;
import com.example.ratelimiter.application.port.in.ManagePoliciesUseCase;
import com.example.ratelimiter.application.port.in.command.PolicyCommand;
import com.example.ratelimiter.domain.model.Policy;
import com.example.ratelimiter.domain.model.PolicyFilter;
import com.example.ratelimiter.domain.model.RateLimitingStrategy;
import com.example.ratelimiter.domain.model.TimePeriod;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

/** MUC4 / MUC6: CRUD of rate-limiting policies. */
@Route(value = "policies", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Policies | Rate Limiter Admin")
public class PoliciesView extends VerticalLayout {

    private final ManagePoliciesUseCase policies;
    private final TextField nameFilter = new TextField("Name contains");
    private final ComboBox<RateLimitingStrategy> strategyFilter =
            CodedEnumFields.comboBox("Strategy", RateLimitingStrategy.class);
    private final PagedGrid<Policy> pagedGrid;

    public PoliciesView(ManagePoliciesUseCase policies) {
        this.policies = policies;
        this.pagedGrid = new PagedGrid<>(page -> policies.list(currentFilter(), page));
        configureColumns(pagedGrid.grid());
        configureFilters();
        CrudToolbar<Policy> toolbar = new CrudToolbar<>(pagedGrid.grid(),
                () -> openForm(null), this::openForm, this::delete);
        setSizeFull();
        add(new H2("Rate-limiting policies"), new HorizontalLayout(nameFilter, strategyFilter),
                toolbar, pagedGrid);
        pagedGrid.refresh();
    }

    private PolicyFilter currentFilter() {
        return new PolicyFilter(nameFilter.getValue(), strategyFilter.getValue());
    }

    private void configureFilters() {
        nameFilter.setValueChangeMode(ValueChangeMode.LAZY);
        nameFilter.addValueChangeListener(event -> pagedGrid.firstPage());
        strategyFilter.addValueChangeListener(event -> pagedGrid.firstPage());
    }

    private static void configureColumns(Grid<Policy> grid) {
        grid.addColumn(Policy::id).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Policy::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(policy -> policy.strategy().value()).setHeader("Strategy");
        grid.addColumn(Policy::numberOfRequests).setHeader("Requests");
        grid.addColumn(policy -> policy.timePeriod().value()).setHeader("Period");
        grid.addColumn(policy -> policy.defaultPolicy() ? "yes" : "").setHeader("Default");
    }

    private void openForm(Policy existing) {
        TextField name = new TextField("Name");
        ComboBox<RateLimitingStrategy> strategy =
                CodedEnumFields.comboBox("Strategy", RateLimitingStrategy.class);
        IntegerField requests = new IntegerField("Number of requests");
        requests.setMin(1);
        ComboBox<TimePeriod> period = CodedEnumFields.comboBox("Time period", TimePeriod.class);
        strategy.setValue(existing == null ? RateLimitingStrategy.DEFAULT : existing.strategy());
        if (existing != null) {
            name.setValue(existing.name());
            requests.setValue(existing.numberOfRequests());
            period.setValue(existing.timePeriod());
        }
        Runnable save = () -> save(existing, new PolicyCommand(name.getValue(), strategy.getValue(),
                requests.getValue() == null ? 0 : requests.getValue(), period.getValue()));
        new FormDialog(existing == null ? "New policy" : "Edit policy", save, pagedGrid::refresh)
                .withFields(name, strategy, requests, period)
                .open();
    }

    private void save(Policy existing, PolicyCommand command) {
        if (existing == null) {
            policies.create(command);
        } else {
            policies.update(existing.id(), command);
        }
    }

    private void delete(Policy policy) {
        if (UiActions.run(() -> policies.delete(policy.id()), "Policy deleted")) {
            pagedGrid.refresh();
        }
    }
}
