package io.github.davolabsl.util;

import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;
import java.io.*;

@Slf4j
public class FasteBlobObjectConverter implements AttributeConverter<Object, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(Object object) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(object);
            return byteOut.toByteArray();
        } catch (IOException e) {
            log.error("Byte array writing error", e);
        }
        return new byte[0];
    }

    @Override
    public Object convertToEntityAttribute(byte[] bytes) {
        try {
            if (bytes == null) {
                return null;
            }
            ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(byteIn);
            Object object = in.readObject();
            return object;
        } catch (IOException | ClassNotFoundException e) {
            log.error("Byte array reading error", e);
        }
        return null;
    }
}
