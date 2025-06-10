package io.github.davolabsl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.AttributeConverter;
import java.io.IOException;

@Slf4j
public class FasteArrayConverter implements AttributeConverter<String[], String> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(String[] data) {
        if (data != null && data.length > 0) {
            try {
                return this.objectMapper.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                log.error("Error converting data to JSON", e);
                throw new IllegalArgumentException("Error converting data to JSON", e);
            }
        } else {
            return "[]";
        }
    }

    @Override
    public String[] convertToEntityAttribute(String dataJSON) {
        if (dataJSON != null && !dataJSON.isEmpty()) {
            try {
                return this.objectMapper.readValue(dataJSON, new TypeReference<String[]>() {
                });
            } catch (IOException e) {
                log.error("Error reading JSON", e);
                throw new IllegalArgumentException("Error reading JSON", e);
            }
        } else {
            return new String[0]; // Return an empty array instead of null
        }
    }
}
