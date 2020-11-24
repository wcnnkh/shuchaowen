package scw.mvc;

import scw.core.parameter.ParameterDescriptor;
import scw.core.parameter.ParameterFactory;
import scw.http.server.ServerHttpRequest;
import scw.http.server.ServerHttpResponse;
import scw.mvc.security.UserSessionFactoryAdapter;
import scw.security.session.UserSession;
import scw.value.Value;

public interface HttpChannel extends ParameterFactory, UserSessionFactoryAdapter {
	long getCreateTime();

	ServerHttpRequest getRequest();

	ServerHttpResponse getResponse();

	<E> E[] getArray(String name, Class<? extends E> type);

	boolean isCompleted();

	Value getValue(String name);

	Value getValue(String name, Value defaultValue);

	Object getParameter(ParameterDescriptor parameterDescriptor);

	<T> T getBean(Class<T> type);

	<T> T getBean(String name);
	
	<T> T getUid(Class<T> type);
	
	String getSessionId();
	
	<T> UserSession<T> getUserSession(Class<T> type);
}
