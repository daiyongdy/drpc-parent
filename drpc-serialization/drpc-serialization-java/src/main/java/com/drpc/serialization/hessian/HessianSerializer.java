package com.drpc.serialization.hessian;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.drpc.serialization.api.SerializerType;
import com.drpc.serialization.api.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by daiyong
 */
public class HessianSerializer extends Serializer {

	@Override
	public int code() {
		return SerializerType.HESSIAN.getCode();
	}

	@Override
	public <T> byte[] writeObject(T obj) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream(DEFAULT_BUF_SIZE);
		Hessian2Output output = new Hessian2Output(baos);
		try {
			output.writeObject(obj);
			output.flush();
			return baos.toByteArray();
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				output.close();
			} catch (IOException ignored) {}
		}
	}

	/**
	 * 解码
	 * @param bytes
	 * @param offset
	 * @param length
	 * @param clazz
	 * @param <T>
	 * @return
	 * @throws IOException
	 */
	@Override
	public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) throws IOException {
		Hessian2Input input = new Hessian2Input(new ByteArrayInputStream(bytes, offset, length));
		try {
			Object obj = input.readObject(clazz);
			return clazz.cast(obj);
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				input.close();
			} catch (IOException ignored) {}
		}
	}

}
