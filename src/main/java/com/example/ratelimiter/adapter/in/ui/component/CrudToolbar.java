package com.example.ratelimiter.adapter.in.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.util.function.Consumer;

/** New / Edit / Delete buttons bound to the selection of a grid. */
public class CrudToolbar<T> extends HorizontalLayout {

    private final Button edit = new Button("Edit");
    private final Button delete = new Button("Delete");

    public CrudToolbar(Grid<T> grid, Runnable onCreate, Consumer<T> onEdit, Consumer<T> onDelete) {
        Button create = new Button("New", event -> onCreate.run());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        edit.addClickListener(event -> withSelection(grid, onEdit));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> withSelection(grid, item -> confirmDelete(item,
                onDelete)));
        grid.asSingleSelect().addValueChangeListener(
                event -> updateButtons(event.getValue() != null));
        updateButtons(false);
        add(create, edit, delete);
    }

    /** Lets a view add its own buttons (e.g. "Sync from OpenAPI"). */
    public void addAction(Button button) {
        add(button);
    }

    private void updateButtons(boolean hasSelection) {
        edit.setEnabled(hasSelection);
        delete.setEnabled(hasSelection);
    }

    private static <T> void withSelection(Grid<T> grid, Consumer<T> action) {
        T selected = grid.asSingleSelect().getValue();
        if (selected != null) {
            action.accept(selected);
        }
    }

    private static <T> void confirmDelete(T item, Consumer<T> onDelete) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete?");
        dialog.setText("This cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> onDelete.accept(item));
        dialog.open();
    }
}
