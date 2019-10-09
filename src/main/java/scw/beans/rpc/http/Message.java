package scw.beans.rpc.http;

import java.io.Serializable;
import java.lang.reflect.Method;

import scw.core.attribute.SimpleAttributeManager;
import scw.core.reflect.SerializableMethodDefinition;

public final class Message extends SimpleAttributeManager implements Serializable {
	private static final long serialVersionUID = 1L;
	private SerializableMethodDefinition methodDefinition;
	private Object[] args;

	protected Message() {
	};

	public Message(Class<?> clz, Method method, Object[] args) {
		this.methodDefinition = new SerializableMethodDefinition(clz, method);
		this.args = args;
	}

	public SerializableMethodDefinition getMethodDefinition() {
		return methodDefinition;
	}

	protected void setMethodDefinition(SerializableMethodDefinition methodDefinition) {
		this.methodDefinition = methodDefinition;
	}

	public Object[] getArgs() {
		return args;
	}

	protected void setArgs(Object[] args) {
		this.args = args;
	}

	public Method getMethod() {
		return methodDefinition.getMethod();
	}

	private transient String messageKey;

	public String getMessageKey() {
		if (messageKey == null) {
			this.messageKey = getMethod().toString();
		}
		return messageKey;
	}
}
