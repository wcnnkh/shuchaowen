package scw.beans.async;

import java.lang.reflect.Method;

import scw.beans.BeanFactory;
import scw.beans.annotation.AsyncComplete;
import scw.beans.annotation.Autowrite;
import scw.core.aop.Filter;
import scw.core.aop.FilterChain;
import scw.core.aop.Invoker;
import scw.core.aop.ProxyUtils;
import scw.core.utils.ClassUtils;
import scw.core.utils.StringUtils;

/**
 * 只能受BeanFactory管理
 * 
 * @author shuchaowen
 *
 */
public final class AsyncCompleteFilter implements Filter {
	private static ThreadLocal<Boolean> ENABLE_TAG = new ThreadLocal<Boolean>();

	public static boolean isEnable() {
		Boolean b = ENABLE_TAG.get();
		return b == null ? true : b;
	}

	public static void setEnable(boolean enable) {
		ENABLE_TAG.set(enable);
	}

	@Autowrite
	private BeanFactory beanFactory;

	private Object realFilter(Invoker invoker, Object proxy, Method method, Object[] args, FilterChain filterChain)
			throws Throwable {
		if (!isEnable()) {
			return filterChain.doFilter(invoker, proxy, method, args);
		}

		AsyncComplete asyncComplete = method.getAnnotation(AsyncComplete.class);
		if (asyncComplete == null) {
			return filterChain.doFilter(invoker, proxy, method, args);
		}

		String beanName = asyncComplete.beanName();
		if (StringUtils.isEmpty(beanName)) {
			if (ProxyUtils.isJDKProxy(proxy)) {
				beanName = method.getDeclaringClass().getName();
			} else {
				beanName = ClassUtils.getUserClass(proxy).getName();
			}
		}

		AsyncInvokeInfo info = new AsyncInvokeInfo(asyncComplete, method.getDeclaringClass(), beanName, method, args);
		AsyncCompleteService service = beanFactory.get(asyncComplete.service());
		return service.service(info);
	}

	public Object filter(Invoker invoker, Object proxy, Method method, Object[] args, FilterChain filterChain)
			throws Throwable {
		try {
			return realFilter(invoker, proxy, method, args, filterChain);
		} finally {
			ENABLE_TAG.remove();
		}
	}
}