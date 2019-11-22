package com.drpc.serialization.api;

import java.io.IOException;

/**
 * Created by daiyong
 */
public abstract class Serializer {

	//默认缓冲区
	public static final int DEFAULT_BUF_SIZE = 512;

	public abstract int code();

	public abstract <T> byte[] writeObject(T obj) throws IOException;

	public abstract <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) throws IOException;

	public <T> T readObject(byte[] bytes, Class<T> clazz) throws IOException {
		return readObject(bytes, 0, bytes.length, clazz);
	}
}
