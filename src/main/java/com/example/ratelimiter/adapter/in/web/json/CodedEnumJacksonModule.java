package com.example.ratelimiter.adapter.in.web.json;

import com.example.ratelimiter.domain.model.CodedEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * (De)serialises every domain {@link CodedEnum} by its external code ({@code moving-window},
 * {@code resource_group}, ...) without putting Jackson annotations into the domain model.
 */
public class CodedEnumJacksonModule extends SimpleModule {

    public CodedEnumJacksonModule() {
        super("coded-enum-module");
        addSerializer(CodedEnum.class, new CodedEnumSerializer());
        setDeserializers(new CodedEnumDeserializers());
    }

    private static final class CodedEnumSerializer extends StdSerializer<CodedEnum> {

        private CodedEnumSerializer() {
            super(CodedEnum.class);
        }

        @Override
        public void serialize(CodedEnum value, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            generator.writeString(value.value());
        }
    }

    private static final class CodedEnumDeserializers extends SimpleDeserializers {

        @Override
        public JsonDeserializer<?> findEnumDeserializer(
                Class<?> type, DeserializationConfig config, BeanDescription description) {
            return CodedEnum.class.isAssignableFrom(type) ? new CodedEnumDeserializer(type) : null;
        }
    }

    private static final class CodedEnumDeserializer extends StdDeserializer<CodedEnum> {

        private final Class<?> enumType;

        private CodedEnumDeserializer(Class<?> enumType) {
            super(enumType);
            this.enumType = enumType;
        }

        @Override
        public CodedEnum deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            String text = parser.getValueAsString();
            return Arrays.stream(enumType.getEnumConstants())
                    .map(CodedEnum.class::cast)
                    .filter(constant -> constant.value().equalsIgnoreCase(text))
                    .findFirst()
                    .orElseThrow(() -> context.weirdStringException(text, enumType,
                            "supported values: " + supportedValues()));
        }

        private String supportedValues() {
            return Arrays.stream(enumType.getEnumConstants())
                    .map(constant -> ((CodedEnum) constant).value())
                    .collect(Collectors.joining(", "));
        }
    }
}
