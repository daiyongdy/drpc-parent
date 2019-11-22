package com.drpc.serialization;

/**
 * Created by daiyong
 */
public enum SerializerType {

	HESSIAN(1);

	private int code;

	SerializerType(int code) {
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}
}
