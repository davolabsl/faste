package io.github.davolabsl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class FasteCollectionHashMapConverter implements AttributeConverter<List<Map<String, Object>>, String> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "[]"; // Return an empty JSON array if the input list is null or empty
        }

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error converting data to JSON", e);
            throw new IllegalArgumentException("Error converting data to JSON", e);
        }
    }

    @Override
    public List<Map<String, Object>> convertToEntityAttribute(String dataJSON) {
        if (dataJSON == null || dataJSON.isEmpty()) {
            return new ArrayList<>(); // Return an empty list if the input JSON string is null or empty
        }

        try {
            return objectMapper.readValue(dataJSON, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            log.error("Error reading JSON", e);
            throw new IllegalArgumentException("Error reading JSON", e);
        }
    }
}
