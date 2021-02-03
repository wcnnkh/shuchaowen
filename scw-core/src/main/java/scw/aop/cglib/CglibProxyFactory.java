package scw.aop.cglib;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import scw.aop.MethodInterceptor;
import scw.aop.Proxy;
import scw.aop.jdk.JdkProxyFactory;
import scw.core.utils.ClassUtils;

public class CglibProxyFactory extends JdkProxyFactory {
	public boolean canProxy(Class<?> clazz) {
		return super.canProxy(clazz) || !Modifier.isFinal(clazz.getModifiers());
	}

	public boolean isProxy(Class<?> clazz) {
		return super.isProxy(clazz) || Enhancer.isEnhanced(clazz);
	}

	private Class<?>[] getInterfaces(Class<?> clazz, Class<?>[] interfaces) {
		if (interfaces == null || interfaces.length == 0) {
			return new Class<?>[0];
		}

		Class<?>[] interfacesToUse = new Class<?>[interfaces.length];
		int index = 0;
		for (Class<?> i : interfaces) {
			if (i.isAssignableFrom(clazz) || Factory.class.isAssignableFrom(i)) {
				continue;
			}

			interfacesToUse[index++] = i;
		}
		return Arrays.copyOf(interfacesToUse, index);
	}

	public Class<?> getProxyClass(Class<?> clazz, Class<?>[] interfaces) {
		if(super.canProxy(clazz)){
			return super.getProxyClass(clazz, interfaces);
		}
		
		Enhancer enhancer = CglibUtils.createEnhancer(clazz, getInterfaces(clazz, interfaces));
		enhancer.setCallbackType(CglibMethodInterceptor.class);
		return enhancer.createClass();
	}

	public Proxy getProxy(Class<?> clazz, Class<?>[] interfaces, MethodInterceptor methodInterceptor) {
		if(super.canProxy(clazz)){
			return super.getProxy(clazz, interfaces, methodInterceptor);
		}
		
		return new CglibProxy(clazz, getInterfaces(clazz, interfaces), methodInterceptor);
	}

	public Class<?> getUserClass(Class<?> clazz) {
		if(super.isProxy(clazz)){
			return super.getUserClass(clazz);
		}
		
		Class<?> clz = clazz.getSuperclass();
		if (clz == null || clz == Object.class) {
			return clazz;
		}
		return clz;
	}

	/** The CGLIB class separator: "$$" */
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	public boolean isProxy(String className, ClassLoader classLoader) throws ClassNotFoundException{
		if(className == null){
			return false;
		}
		
		return super.isProxy(className, classLoader) || className.contains(CGLIB_CLASS_SEPARATOR);
	}

	public Class<?> getUserClass(String className, ClassLoader classLoader)
			throws ClassNotFoundException {
		if(super.isProxy(className, classLoader)){
			return super.getUserClass(className, classLoader);
		}
		
		return ClassUtils.forName(className.substring(0, className.indexOf(CGLIB_CLASS_SEPARATOR)),
				classLoader);
	}
}
