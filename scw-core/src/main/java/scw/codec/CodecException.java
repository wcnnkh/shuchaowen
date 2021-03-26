package scw.codec;

import scw.lang.NestedRuntimeException;

public class CodecException extends NestedRuntimeException {
	private static final long serialVersionUID = 1L;

	public CodecException(String msg) {
		super(msg);
	}

	public CodecException(Throwable cause) {
		super(cause);
	}

	public CodecException(String message, Throwable cause) {
		super(message, cause);
	}
}
