package com.example.ratelimiter.adapter.in.ui.view;

import com.example.ratelimiter.adapter.in.ui.MainLayout;
import com.example.ratelimiter.adapter.in.ui.component.CrudToolbar;
import com.example.ratelimiter.adapter.in.ui.component.FormDialog;
import com.example.ratelimiter.adapter.in.ui.component.PagedGrid;
import com.example.ratelimiter.adapter.in.ui.component.UiActions;
import com.example.ratelimiter.application.port.in.ManageResourceGroupsUseCase;
import com.example.ratelimiter.application.port.in.command.ResourceGroupCommand;
import com.example.ratelimiter.domain.model.ResourceGroup;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** MUC6: CRUD of resource groups. */
@Route(value = "resource-groups", layout = MainLayout.class)
@PageTitle("Resource groups | Rate Limiter Admin")
public class ResourceGroupsView extends VerticalLayout {

    private final ManageResourceGroupsUseCase resourceGroups;
    private final PagedGrid<ResourceGroup> pagedGrid;

    public ResourceGroupsView(ManageResourceGroupsUseCase resourceGroups) {
        this.resourceGroups = resourceGroups;
        this.pagedGrid = new PagedGrid<>(resourceGroups::list);
        pagedGrid.grid().addColumn(ResourceGroup::id).setHeader("ID").setAutoWidth(true);
        pagedGrid.grid().addColumn(ResourceGroup::name).setHeader("Name");
        CrudToolbar<ResourceGroup> toolbar = new CrudToolbar<>(pagedGrid.grid(),
                () -> openForm(null), this::openForm, this::delete);
        setSizeFull();
        add(new H2("Resource groups"), toolbar, pagedGrid);
        pagedGrid.refresh();
    }

    private void openForm(ResourceGroup existing) {
        TextField name = new TextField("Name");
        if (existing != null) {
            name.setValue(existing.name());
        }
        Runnable save = () -> {
            ResourceGroupCommand command = new ResourceGroupCommand(name.getValue());
            if (existing == null) {
                resourceGroups.create(command);
            } else {
                resourceGroups.update(existing.id(), command);
            }
        };
        new FormDialog(existing == null ? "New resource group" : "Edit resource group", save,
                pagedGrid::refresh).withFields(name).open();
    }

    private void delete(ResourceGroup group) {
        if (UiActions.run(() -> resourceGroups.delete(group.id()), "Resource group deleted")) {
            pagedGrid.refresh();
        }
    }
}
