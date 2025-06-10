package io.github.davolabsl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class FasteHashMapConverter implements AttributeConverter<Map<String, Object>, String> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(Map<String, Object> data) {

        String dataJson = null;
        try {
            dataJson = objectMapper.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            log.error("JSON writing error", e);
        }

        return dataJson;
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dataJSON) {

        Map<String, Object> data = null;
        try {
            if(dataJSON != null && !dataJSON.isEmpty()){
                data = objectMapper.readValue(dataJSON, new TypeReference<HashMap<String, Object>>() {});
            }
        } catch (final IOException e) {
            log.error("JSON reading error", e);
        }

        return data;
    }
}
