package scw.redis.connection;

import scw.lang.NestedRuntimeException;

public class RedisSystemException extends NestedRuntimeException {
	private static final long serialVersionUID = 1L;

	public RedisSystemException(String msg) {
		super(msg);
	}

	public RedisSystemException(Throwable cause) {
		super(cause);
	}

	public RedisSystemException(String message, Throwable cause) {
		super(message, cause);
	}
}
