package com.drpc.serialization.api;

import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Created by daiyong
 */
public class SerializerFactory {

	/**
	 * java spi
	 */
	private static Map<Integer, Serializer> serializerMap = Maps.newHashMap();
	static {
		ServiceLoader<Serializer> loader = ServiceLoader.load(Serializer.class);
		Iterator<Serializer> iterator = loader.iterator();
		while (iterator.hasNext()){
			Serializer serializer = iterator.next();
			serializerMap.put(serializer.code(), serializer);
		}
	}

	public static Serializer getSerializer(Integer code) throws Exception {

		Serializer serializer = serializerMap.get(code);

		if (null == serializer) {
			throw new Exception("没有此序列化方式");
		}

		return serializer;
	}

}