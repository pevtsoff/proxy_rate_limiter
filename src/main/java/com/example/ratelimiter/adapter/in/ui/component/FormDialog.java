package com.example.ratelimiter.adapter.in.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;

/**
 * Modal edit form. The dialog stays open when saving fails so the admin can correct the input.
 */
public class FormDialog extends Dialog {

    private final FormLayout form = new FormLayout();

    /**
     * @param save      calls the use case; may throw a domain exception
     * @param afterSave e.g. refresh the grid
     */
    public FormDialog(String title, Runnable save, Runnable afterSave) {
        setHeaderTitle(title);
        add(form);
        Button saveButton = new Button("Save", event -> {
            if (UiActions.run(save, "Saved")) {
                close();
                afterSave.run();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(new Button("Cancel", event -> close()), saveButton);
    }

    public FormDialog withFields(Component... fields) {
        form.add(fields);
        return this;
    }
}
