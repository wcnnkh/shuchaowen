package scw.nacos.client;

import scw.lang.NestedRuntimeException;

public class NacosConfigException extends NestedRuntimeException {
	private static final long serialVersionUID = 1L;

	public NacosConfigException(String msg) {
		super(msg);
	}

	public NacosConfigException(Throwable cause) {
		super(cause);
	}

	public NacosConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
