package core.framework.plugin.utils;

import com.fasterxml.jackson.databind.JavaType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;


/**
 * @author ebin
 */
public final class JSON {
    private JSON() {
    }

    public static <T> T fromJSON(Type instanceType, String json) {
        try {
            JavaType javaType = JSONMapper.OBJECT_MAPPER.getTypeFactory().constructType(instanceType);
            return JSONMapper.OBJECT_MAPPER.readValue(json, javaType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T fromJSON(Class<T> instanceClass, String json) {
        try {
            return JSONMapper.OBJECT_MAPPER.readValue(json, instanceClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T fromJSON(Class<T> instanceClass, byte[] json) {
        try {
            return JSONMapper.OBJECT_MAPPER.readValue(json, instanceClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String toJSON(Object instance) {
        try {
            return JSONMapper.OBJECT_MAPPER.writeValueAsString(instance);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
