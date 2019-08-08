package com.topfox.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 为了输出空对象 {} 空数组 [] 的实现
 */
public class ListJsonSerialize extends JsonSerializer<Object> {

    @Override
    public void serialize(Object data, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(data instanceof List && ((List)data).size() == 0) {
            jsonGenerator.writeStartArray();
            jsonGenerator.writeEndArray();
        }else if(data instanceof Map && ((Map)data).keySet().size() ==0){
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }else{
            jsonGenerator.writeObject(data);
        }
    }
}
