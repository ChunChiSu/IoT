package com.cht.iot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

public class JsonUtils {

	static final ObjectMapper jackson = new ObjectMapper();
	static {
		jackson.setSerializationInclusion(Inclusion.NON_NULL);
	}
	
	public static String toJson(Object obj) throws IOException {
		return jackson.writeValueAsString(obj);
	}
	
	public static <T> T fromJson(InputStream is, Class<T> clazz) throws IOException {
		return jackson.readValue(is, clazz);
	}
	
	public static <T> T fromJson(Reader r, Class<T> clazz) throws IOException {
		return jackson.readValue(r, clazz);
	}
	
	public static <T> T fromJson(String s, Class<T> clazz) throws IOException {
		return jackson.readValue(s, clazz);
	}
}
