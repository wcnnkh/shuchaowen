package scw.aop;

public interface MethodInterceptor {
	Object intercept(MethodInvoker invoker, Object[] args, MethodInterceptorChain chain) throws Throwable;
}
