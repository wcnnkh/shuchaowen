package scw.beans.rpc.http;

import java.io.Serializable;

public class HttpRcpResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private Throwable throwable;
	private Object response;

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}
}
