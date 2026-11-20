package com.example.ratelimiter.adapter.in.ui.component;

import com.example.ratelimiter.domain.model.CodedEnum;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.List;

/** Factory for combo boxes over domain enums, labelled with their external codes. */
public final class CodedEnumFields {

    private CodedEnumFields() {
    }

    public static <E extends Enum<E> & CodedEnum> ComboBox<E> comboBox(String label,
            Class<E> type) {
        ComboBox<E> comboBox = new ComboBox<>(label);
        comboBox.setItems(List.of(type.getEnumConstants()));
        comboBox.setItemLabelGenerator(CodedEnum::value);
        comboBox.setClearButtonVisible(true);
        return comboBox;
    }
}
