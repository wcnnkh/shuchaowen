package scw.rpc.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

import scw.aop.MethodInterceptor;
import scw.core.reflect.MethodInvoker;
import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.rpc.CallableFactory;

public class RemoteMethodInterceptor implements MethodInterceptor{
	private static Logger logger = LoggerFactory.getLogger(RemoteMethodInterceptor.class);
	private final CallableFactory callableFactory;

	public RemoteMethodInterceptor(CallableFactory callableFactory){
		this.callableFactory = callableFactory;
	}
	
	public Object intercept(MethodInvoker invoker, Object[] args)
			throws Throwable {
		Method method = invoker.getMethod();
		if (Modifier.isStatic(method.getModifiers()) || !Modifier.isAbstract(method.getModifiers())) {
			logger.trace("ignore method " + method);
			return invoker.invoke(args);
		}
		
		Callable<Object> callable = callableFactory.getCallable(invoker.getDeclaringClass(), method, args);
		if(callable == null){
			logger.debug("ignore");
			return invoker.invoke(args);
		}
		
		return callable.call();
	}

}
