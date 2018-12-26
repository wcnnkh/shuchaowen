package scw.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.Enhancer;
import scw.beans.annotaion.Destroy;
import scw.beans.annotaion.InitMethod;
import scw.beans.annotaion.Retry;
import scw.beans.annotaion.SelectCache;
import scw.beans.annotaion.Service;
import scw.beans.property.PropertiesFactory;
import scw.common.ClassInfo;
import scw.common.FieldInfo;
import scw.common.exception.BeansException;
import scw.common.utils.ClassUtils;
import scw.common.utils.StringUtils;

public class AnnotationBean implements Bean {
	private final BeanFactory beanFactory;
	private final Class<?> type;
	private final String id;
	private volatile Constructor<?> constructor;
	private final List<Method> initMethodList = new ArrayList<Method>();
	private final List<Method> destroyMethodList = new ArrayList<Method>();
	private final boolean proxy;
	private Enhancer enhancer;
	private final PropertiesFactory propertiesFactory;
	private String[] filterNames;

	public AnnotationBean(BeanFactory beanFactory, PropertiesFactory propertiesFactory, Class<?> type,
			String[] filterNames) throws Exception {
		this.beanFactory = beanFactory;
		this.type = type;
		this.propertiesFactory = propertiesFactory;

		String id = ClassUtils.getProxyRealClassName(type);
		Service service = type.getAnnotation(Service.class);
		if (service != null) {
			Class<?>[] interfaces = type.getInterfaces();
			if (interfaces.length != 0) {
				id = interfaces[0].getName();
			}
			
			if (!StringUtils.isNull(service.value())) {
				id = service.value();
			}
		}
		this.id = id;

		Class<?> tempClz = type;
		for (Method method : tempClz.getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			InitMethod initMethod = method.getAnnotation(InitMethod.class);
			if (initMethod != null) {
				method.setAccessible(true);
				initMethodList.add(method);
			}

			Destroy destroy = method.getAnnotation(Destroy.class);
			if (destroy != null) {
				method.setAccessible(true);
				destroyMethodList.add(method);
			}
		}

		this.filterNames = filterNames;
		this.proxy = (filterNames != null && filterNames.length != 0) || checkProxy(type);
	}

	public static List<BeanMethod> getInitMethodList(Class<?> type) {
		List<BeanMethod> list = new ArrayList<BeanMethod>();
		Class<?> tempClz = type;
		for (Method method : tempClz.getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			InitMethod initMethod = method.getAnnotation(InitMethod.class);
			if (initMethod != null) {
				method.setAccessible(true);
				list.add(new NoArgumentBeanMethod(method));
			}
		}
		return list;
	}

	public static List<BeanMethod> getDestroyMethdoList(Class<?> type) {
		List<BeanMethod> list = new ArrayList<BeanMethod>();
		Class<?> tempClz = type;
		for (Method method : tempClz.getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			Destroy destroy = method.getAnnotation(Destroy.class);
			if (destroy != null) {
				method.setAccessible(true);
				list.add(new NoArgumentBeanMethod(method));
			}
		}
		return list;
	}

	public static Retry getRetry(Class<?> type, Method method) {
		Retry retry = method.getAnnotation(Retry.class);
		if (retry == null) {
			retry = type.getAnnotation(Retry.class);
		}
		return retry;
	}

	public static boolean checkProxy(Class<?> type) {
		if (Modifier.isFinal(type.getModifiers())) {
			return false;
		}

		SelectCache selectCache = type.getAnnotation(SelectCache.class);
		if (selectCache != null) {
			return true;
		}

		for (Method method : type.getDeclaredMethods()) {
			if (BeanUtils.isTransaction(type, method)) {
				return true;
			}

			Retry retry = getRetry(type, method);
			if (retry != null && retry.errors().length != 0) {
				return true;
			}

			selectCache = method.getAnnotation(SelectCache.class);
			if (selectCache != null) {
				return true;
			}
		}
		return false;
	}

	public boolean isSingleton() {
		return true;
	}

	public boolean isProxy() {
		return this.proxy;
	}

	public String getId() {
		return this.id;
	}

	public Class<?> getType() {
		return this.type;
	}

	private Enhancer getProxyEnhancer() {
		if (enhancer == null) {
			enhancer = BeanUtils.createEnhancer(type, beanFactory, null, filterNames);
		}
		return enhancer;
	}

	@SuppressWarnings("unchecked")
	public <T> T newInstance() {
		if (constructor == null) {
			synchronized (this) {
				if (constructor == null) {
					try {
						this.constructor = type.getDeclaredConstructor();// 不用考虑并发
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}
				}
			}
		}

		Object bean;
		try {
			if (isProxy()) {
				Enhancer enhancer = getProxyEnhancer();
				bean = enhancer.create();
			} else {
				bean = constructor.newInstance();
			}
			return (T) bean;
		} catch (Exception e) {
			throw new BeansException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T newInstance(Class<?>[] parameterTypes, Object... args) {
		Object bean;
		try {
			if (isProxy()) {
				Enhancer enhancer = getProxyEnhancer();
				bean = enhancer.create(parameterTypes, args);
			} else {
				bean = type.getDeclaredConstructor(parameterTypes).newInstance(args);
			}
			return (T) bean;
		} catch (Exception e) {
			throw new BeansException(e);
		}
	}

	public void autowrite(Object bean) throws Exception {
		ClassInfo classInfo = ClassUtils.getClassInfo(type);
		while (classInfo != null) {
			for (FieldInfo field : classInfo.getFieldMap().values()) {
				if (Modifier.isStatic(field.getField().getModifiers())) {
					continue;
				}

				BeanUtils.autoWrite(classInfo.getClz(), beanFactory, propertiesFactory, bean, field);
			}
			classInfo = classInfo.getSuperInfo();
		}
	}

	public void init(Object bean) throws Exception {
		if (initMethodList != null && !initMethodList.isEmpty()) {
			for (Method method : initMethodList) {
				method.invoke(bean);
			}
		}
	}

	public void destroy(Object bean) {
		if (destroyMethodList != null && !destroyMethodList.isEmpty()) {
			for (Method method : destroyMethodList) {
				try {
					method.invoke(bean);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String[] getNames() {
		return null;
	}
}
