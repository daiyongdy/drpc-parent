package com.drpc;

import java.io.Serializable;

/**
 * Created by daiyong
 */
public class ResultWrapper implements Serializable {
	private Object result;
	private Exception exception;

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}
}
