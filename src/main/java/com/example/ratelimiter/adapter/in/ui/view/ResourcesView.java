package com.example.ratelimiter.adapter.in.ui.view;

import com.example.ratelimiter.adapter.in.ui.MainLayout;
import com.example.ratelimiter.adapter.in.ui.component.CodedEnumFields;
import com.example.ratelimiter.adapter.in.ui.component.CrudToolbar;
import com.example.ratelimiter.adapter.in.ui.component.FormDialog;
import com.example.ratelimiter.adapter.in.ui.component.Lookups;
import com.example.ratelimiter.adapter.in.ui.component.PagedGrid;
import com.example.ratelimiter.adapter.in.ui.component.UiActions;
import com.example.ratelimiter.application.port.in.ManageResourceGroupsUseCase;
import com.example.ratelimiter.application.port.in.ManageResourcesUseCase;
import com.example.ratelimiter.application.port.in.SyncResourcesUseCase;
import com.example.ratelimiter.application.port.in.command.CreateResourceCommand;
import com.example.ratelimiter.application.port.in.command.UpdateResourceCommand;
import com.example.ratelimiter.domain.model.HttpMethod;
import com.example.ratelimiter.domain.model.IoType;
import com.example.ratelimiter.domain.model.PageQuery;
import com.example.ratelimiter.domain.model.Resource;
import com.example.ratelimiter.domain.model.ResourceFilter;
import com.example.ratelimiter.domain.model.ResourceGroup;
import com.vaadin.flow.component.button.Button;
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

/** MUC1.4 / MUC6: target resources, with the "sync from OpenAPI" action. */
@Route(value = "resources", layout = MainLayout.class)
@PageTitle("Resources | Rate Limiter Admin")
public class ResourcesView extends VerticalLayout {

    private final ManageResourcesUseCase resources;
    private final ManageResourceGroupsUseCase resourceGroups;
    private final SyncResourcesUseCase sync;
    private final TextField pathFilter = new TextField("Path contains");
    private final ComboBox<HttpMethod> methodFilter =
            CodedEnumFields.comboBox("Method", HttpMethod.class);
    private final ComboBox<IoType> ioTypeFilter = CodedEnumFields.comboBox("IO type", IoType.class);
    private final PagedGrid<Resource> pagedGrid;
    private Map<Long, ResourceGroup> groupsById = Map.of();

    public ResourcesView(ManageResourcesUseCase resources,
            ManageResourceGroupsUseCase resourceGroups, SyncResourcesUseCase sync) {
        this.resources = resources;
        this.resourceGroups = resourceGroups;
        this.sync = sync;
        this.pagedGrid = new PagedGrid<>(page -> resources.list(currentFilter(), page));
        configureColumns();
        configureFilters();
        CrudToolbar<Resource> toolbar = new CrudToolbar<>(pagedGrid.grid(),
                this::openCreateForm, this::openEditForm, this::delete);
        toolbar.addAction(new Button("Sync from target OpenAPI", event -> synchronize()));
        setSizeFull();
        add(new H2("Target resources"),
                new HorizontalLayout(pathFilter, methodFilter, ioTypeFilter), toolbar, pagedGrid);
        reloadGroups();
        pagedGrid.refresh();
    }

    private ResourceFilter currentFilter() {
        return new ResourceFilter(pathFilter.getValue(), methodFilter.getValue(),
                ioTypeFilter.getValue(), null);
    }

    private void configureColumns() {
        pagedGrid.grid().addColumn(Resource::id).setHeader("ID").setAutoWidth(true);
        pagedGrid.grid().addColumn(resource -> resource.method().value()).setHeader("Method");
        pagedGrid.grid().addColumn(Resource::path).setHeader("Path").setAutoWidth(true);
        pagedGrid.grid().addColumn(resource -> resource.ioType().value()).setHeader("IO type");
        pagedGrid.grid().addColumn(resource -> Lookups.label(groupsById,
                resource.resourceGroupId(), ResourceGroup::name)).setHeader("Resource group");
    }

    private void configureFilters() {
        pathFilter.setValueChangeMode(ValueChangeMode.LAZY);
        pathFilter.addValueChangeListener(event -> pagedGrid.firstPage());
        methodFilter.addValueChangeListener(event -> pagedGrid.firstPage());
        ioTypeFilter.addValueChangeListener(event -> pagedGrid.firstPage());
    }

    private void reloadGroups() {
        groupsById = resourceGroups.list(PageQuery.all()).content().stream()
                .collect(Collectors.toMap(ResourceGroup::id, Function.identity()));
    }

    private void synchronize() {
        if (UiActions.run(sync::synchronize, "Resources synchronised from OpenAPI")) {
            reloadGroups();
            pagedGrid.refresh();
        }
    }

    private ComboBox<ResourceGroup> groupField(Long selectedId) {
        ComboBox<ResourceGroup> group = new ComboBox<>("Resource group", groupsById.values());
        group.setItemLabelGenerator(ResourceGroup::name);
        group.setClearButtonVisible(true);
        group.setValue(selectedId == null ? null : groupsById.get(selectedId));
        return group;
    }

    private void openCreateForm() {
        ComboBox<HttpMethod> method = CodedEnumFields.comboBox("Method", HttpMethod.class);
        TextField path = new TextField("Path (OpenAPI template, e.g. /orders/{id})");
        ComboBox<IoType> ioType = CodedEnumFields.comboBox("IO type (optional)", IoType.class);
        ComboBox<ResourceGroup> group = groupField(null);
        Runnable save = () -> resources.create(new CreateResourceCommand(method.getValue(),
                path.getValue(), ioType.getValue(), idOf(group.getValue())));
        new FormDialog("New resource", save, pagedGrid::refresh)
                .withFields(method, path, ioType, group)
                .open();
    }

    private void openEditForm(Resource existing) {
        ComboBox<IoType> ioType = CodedEnumFields.comboBox("IO type", IoType.class);
        ioType.setValue(existing.ioType());
        ComboBox<ResourceGroup> group = groupField(existing.resourceGroupId());
        Runnable save = () -> resources.update(existing.id(),
                new UpdateResourceCommand(ioType.getValue(), idOf(group.getValue())));
        new FormDialog("Edit " + existing.label(), save, pagedGrid::refresh)
                .withFields(ioType, group)
                .open();
    }

    private void delete(Resource resource) {
        if (UiActions.run(() -> resources.delete(resource.id()), "Resource deleted")) {
            pagedGrid.refresh();
        }
    }

    private static Long idOf(ResourceGroup group) {
        return group == null ? null : group.id();
    }
}
