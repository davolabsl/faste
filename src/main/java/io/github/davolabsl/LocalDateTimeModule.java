package io.github.davolabsl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeModule extends SimpleModule {

    public LocalDateTimeModule() {
        addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    }

    private class LocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

        private static final long serialVersionUID = 1L;

        protected LocalDateTimeSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")));
        }
    }
}