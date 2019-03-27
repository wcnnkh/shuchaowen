package scw.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import net.sf.cglib.proxy.Enhancer;
import scw.aop.Filter;
import scw.beans.annotaion.Autowrite;
import scw.beans.annotaion.Bean;
import scw.beans.annotaion.Config;
import scw.beans.annotaion.Destroy;
import scw.beans.annotaion.InitMethod;
import scw.beans.annotaion.Properties;
import scw.beans.annotaion.Retry;
import scw.beans.property.PropertiesFactory;
import scw.beans.xml.XmlBeanParameter;
import scw.common.ClassInfo;
import scw.common.FieldInfo;
import scw.common.exception.BeansException;
import scw.common.reflect.Invoker;
import scw.common.reflect.ReflectInvoker;
import scw.common.utils.ClassUtils;
import scw.common.utils.StringUtils;
import scw.logger.Logger;
import scw.logger.LoggerFactory;

public final class BeanUtils {
	private static Logger logger = LoggerFactory.getLogger(BeanUtils.class);

	private static volatile boolean initStatic = false;
	private static volatile boolean destroyStatic = false;

	private BeanUtils() {
	};

	/**
	 * 调用init方法
	 * 
	 * @param beanFactory
	 * @param classList
	 * @throws Exception
	 */
	private static void invokerInitStaticMethod(Collection<Class<?>> classList) throws Exception {
		List<Invoker> list = new ArrayList<Invoker>();
		for (Class<?> clz : classList) {
			for (Method method : clz.getDeclaredMethods()) {
				if (!Modifier.isStatic(method.getModifiers())) {
					continue;
				}

				InitMethod initMethod = method.getAnnotation(InitMethod.class);
				if (initMethod == null) {
					continue;
				}

				if (method.getParameterCount() != 0) {
					throw new BeansException("ClassName=" + clz.getName() + ",MethodName="
							+ method.getName() + "There must be no parameter.");
				}

				Invoker invoke = new ReflectInvoker(null, method);
				list.add(invoke);
			}
		}

		// 调用指定注解的方法
		CountDownLatch countDownLatch = new CountDownLatch(list.size());
		for (Invoker info : list) {
			InitProcess process = new InitProcess(info, countDownLatch);
			new Thread(process).start();
		}
		countDownLatch.await();
	}

	public synchronized static void destroyStaticMethod(Collection<Class<?>> classList) throws Exception {
		if (!initStatic && destroyStatic) {
			return;
		}

		destroyStatic = true;
		List<Invoker> list = new ArrayList<Invoker>();
		for (Class<?> clz : classList) {
			for (Method method : clz.getDeclaredMethods()) {
				if (!Modifier.isStatic(method.getModifiers())) {
					continue;
				}

				Destroy destroy = method.getAnnotation(Destroy.class);
				if (destroy == null) {
					continue;
				}

				if (method.getParameterCount() != 0) {
					throw new RuntimeException("ClassName=" + clz.getName() + ",MethodName="
							+ method.getName() + "There must be no parameter.");
				}

				Invoker invoke = new ReflectInvoker(null, method);
				list.add(invoke);
			}
		}

		CountDownLatch countDownLatch = new CountDownLatch(list.size());
		for (Invoker info : list) {
			InitProcess process = new InitProcess(info, countDownLatch);
			new Thread(process).start();
		}
		countDownLatch.await();
	}

	public synchronized static void initStatic(BeanFactory beanFactory, PropertiesFactory propertiesFactory,
			Collection<Class<?>> classList) throws Exception {
		if (initStatic) {
			return;
		}

		initStatic = true;
		initAutowriteStatic(beanFactory, propertiesFactory, classList);
		invokerInitStaticMethod(classList);
	}

	public static void autoWrite(Class<?> clz, BeanFactory beanFactory, PropertiesFactory propertiesFactory, Object obj,
			FieldInfo field) {
		setBean(beanFactory, clz, obj, field);
		setConfig(beanFactory, clz, obj, field);
		setProperties(beanFactory, propertiesFactory, clz, obj, field);
	}

	/**
	 * 过滤非静态方法和非静态字段
	 * 
	 * @param classList
	 */
	private static void initAutowriteStatic(BeanFactory beanFactory, PropertiesFactory propertiesFactory,
			Collection<Class<?>> classList) throws Exception {
		for (Class<?> clz : classList) {
			ClassInfo classInfo = ClassUtils.getClassInfo(clz);
			for (Entry<String, FieldInfo> entry : classInfo.getFieldMap().entrySet()) {
				FieldInfo field = entry.getValue();
				if (!Modifier.isStatic(field.getField().getModifiers())) {
					continue;
				}

				autoWrite(clz, beanFactory, propertiesFactory, null, field);
			}
		}
	}

	/**
	 * 对参数重新排序
	 * 
	 * @param executable
	 * @param beanMethodParameters
	 * @return
	 */
	public static XmlBeanParameter[] sortParameters(Executable executable, XmlBeanParameter[] beanMethodParameters) {
		if (executable.getParameterCount() != beanMethodParameters.length) {
			return null;
		}

		String[] paramNames;
		if (executable instanceof Constructor) {
			paramNames = ClassUtils.getParameterName((Constructor<?>) executable);
		} else {
			paramNames = ClassUtils.getParameterName((Method) executable);
		}

		XmlBeanParameter[] methodParameters = new XmlBeanParameter[beanMethodParameters.length];
		Class<?>[] oldTypes = executable.getParameterTypes();
		Class<?>[] types = new Class<?>[methodParameters.length];
		for (int i = 0; i < methodParameters.length; i++) {
			XmlBeanParameter beanMethodParameter = beanMethodParameters[i].clone();
			if (!StringUtils.isNull(beanMethodParameter.getName())) {
				for (int a = 0; a < paramNames.length; a++) {
					if (paramNames[a].equals(beanMethodParameter.getName())) {
						types[a] = oldTypes[a];
						methodParameters[a] = beanMethodParameters[i].clone();
						methodParameters[a].setParameterType(oldTypes[a]);
					}
				}
			} else if (beanMethodParameter.getParameterType() != null) {
				methodParameters[i] = beanMethodParameter;
				types[i] = beanMethodParameter.getParameterType();
			} else {
				types[i] = oldTypes[i];
				methodParameters[i] = beanMethodParameter;
				methodParameters[i].setParameterType(types[i]);
			}

		}

		boolean find = true;
		for (int b = 0; b < types.length; b++) {
			if (oldTypes[b] != types[b]) {
				find = false;
				break;
			}
		}
		return find ? methodParameters : null;
	}

	public static Object[] getBeanMethodParameterArgs(XmlBeanParameter[] beanParameters, BeanFactory beanFactory,
			scw.beans.property.PropertiesFactory propertiesFactory) throws Exception {
		Object[] args = new Object[beanParameters.length];
		for (int i = 0; i < args.length; i++) {
			XmlBeanParameter xmlBeanParameter = beanParameters[i];
			args[i] = xmlBeanParameter.parseValue(beanFactory, propertiesFactory);
		}
		return args;
	}

	private static void setConfig(BeanFactory beanFactory, Class<?> clz, Object obj, FieldInfo field) {
		Config config = field.getField().getAnnotation(Config.class);
		if (config != null) {
			staticFieldWarnLog(Config.class.getName(), clz, field.getField());
			Object value = null;
			try {
				existDefaultValueWarnLog(Config.class.getName(), clz, field, obj);

				value = beanFactory.get(config.parse()).parse(beanFactory, field, config.value(), config.charset());
				field.set(obj, value);
			} catch (Exception e) {
				logger.error("clz=" + clz.getName() + ",fieldName=" + field.getName(), e);
			}
		}
	}

	private static boolean checkExistDefaultValue(FieldInfo field, Object obj)
			throws IllegalArgumentException, IllegalAccessException {
		if (field.getType().isPrimitive()) {// 值类型一定是有默认值的,所以不用判断直接所回false
			return false;
		}
		return field.forceGet(obj) != null;
	}

	private static void existDefaultValueWarnLog(String tag, Class<?> clz, FieldInfo field, Object obj)
			throws IllegalArgumentException, IllegalAccessException {
		if (checkExistDefaultValue(field, obj)) {
			logger.warn(
					tag + " class[" + clz.getName() + "] fieldName[" + field.getName() + "] existence default value");
		}
	}

	private static void staticFieldWarnLog(String tag, Class<?> clz, Field field) {
		if (Modifier.isStatic(field.getModifiers())) {
			logger.warn(tag + " class[" + clz.getName() + "] fieldName[" + field.getName() + "] is a static field");
		}
	}

	private static void setProperties(BeanFactory beanFactory, PropertiesFactory propertiesFactory, Class<?> clz,
			Object obj, FieldInfo field) {
		Properties properties = field.getField().getAnnotation(Properties.class);
		if (properties != null) {
			staticFieldWarnLog(Properties.class.getName(), clz, field.getField());

			Object value = null;
			try {
				existDefaultValueWarnLog(Properties.class.getName(), clz, field, obj);

				String v = propertiesFactory.getValue(properties.value());
				if (v != null) {
					value = StringUtils.conversion(v, field.getType());
					field.set(obj, value);
				}
			} catch (Exception e) {
				logger.error("clz=" + clz.getName() + ",fieldName=" + field.getName(), e);
			}
		}
	}

	private static void setBean(BeanFactory beanFactory, Class<?> clz, Object obj, FieldInfo field) {
		Autowrite s = field.getField().getAnnotation(Autowrite.class);
		if (s != null) {
			staticFieldWarnLog(Autowrite.class.getName(), clz, field.getField());

			String name = s.value();
			if (name.length() == 0) {
				name = field.getType().getName();
			}

			try {
				existDefaultValueWarnLog(Autowrite.class.getName(), clz, field, obj);
				field.set(obj, beanFactory.get(name));
			} catch (Exception e) {
				logger.error("clz=" + clz.getName() + ",fieldName=" + field.getName(), e);
			}
		}
	}

	public static Enhancer createEnhancer(Class<?> clz, BeanFactory beanFactory, String[] filterNames) {
		ClassInfo classInfo = ClassUtils.getClassInfo(clz);
		Enhancer enhancer = new Enhancer();

		enhancer.setInterfaces(clz.getInterfaces());
		if (classInfo.getSerialVersionUID() != null) {
			enhancer.setSerialVersionUID(classInfo.getSerialVersionUID());
		}

		enhancer.setCallback(new BeanMethodInterceptor(beanFactory, filterNames));
		enhancer.setSuperclass(clz);
		return enhancer;
	}

	public static Enhancer createFieldListenEnhancer(Class<?> clz) {
		ClassInfo classInfo = ClassUtils.getClassInfo(clz);
		Class<?>[] beanListenInterfaces;
		if (BeanFieldListen.class.isAssignableFrom(clz)) {
			beanListenInterfaces = clz.getInterfaces();
		} else {// 没有自己实现此接口，增加此接口
			Class<?>[] arr = clz.getInterfaces();
			if (arr.length == 0) {
				beanListenInterfaces = new Class[] { BeanFieldListen.class };
			} else {
				beanListenInterfaces = new Class[arr.length + 1];
				System.arraycopy(arr, 0, beanListenInterfaces, 0, arr.length);
				beanListenInterfaces[arr.length] = BeanFieldListen.class;
			}
		}

		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(beanListenInterfaces);
		if (classInfo.getSerialVersionUID() != null) {
			enhancer.setSerialVersionUID(classInfo.getSerialVersionUID());
		}

		enhancer.setCallback(new FieldListenMethodInterceptor());
		enhancer.setSuperclass(clz);
		return enhancer;
	}

	public static Class<?> getFieldListenProxyClass(Class<?> clz) {
		ClassInfo classInfo = ClassUtils.getClassInfo(clz);
		Class<?>[] beanListenInterfaces;
		if (BeanFieldListen.class.isAssignableFrom(clz)) {
			beanListenInterfaces = clz.getInterfaces();
		} else {// 没有自己实现此接口，增加此接口
			Class<?>[] arr = clz.getInterfaces();
			if (arr.length == 0) {
				beanListenInterfaces = new Class[] { BeanFieldListen.class };
			} else {
				beanListenInterfaces = new Class[arr.length + 1];
				System.arraycopy(arr, 0, beanListenInterfaces, 0, arr.length);
				beanListenInterfaces[arr.length] = BeanFieldListen.class;
			}
		}

		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(beanListenInterfaces);
		if (classInfo.getSerialVersionUID() != null) {
			enhancer.setSerialVersionUID(classInfo.getSerialVersionUID());
		}

		enhancer.setCallbackType(FieldListenMethodInterceptor.class);
		enhancer.setSuperclass(clz);
		return enhancer.createClass();
	}

	/**
	 * 可以监听属性变化
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newFieldListenInstance(Class<T> clz) {
		return (T) createFieldListenEnhancer(clz).create();
	}

	/**
	 * 把一个普通对象转成可以监听字段变化的对象
	 * 
	 * @param bean
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T transformationFieldListen(T bean) {
		ClassInfo classInfo = ClassUtils.getClassInfo(bean.getClass());
		BeanFieldListen proxy = (BeanFieldListen) newFieldListenInstance(classInfo.getClz());
		for (Entry<String, FieldInfo> entry : classInfo.getFieldMap().entrySet()) {
			FieldInfo fieldInfo = entry.getValue();
			if (fieldInfo.isStatic()) {
				continue;
			}

			Object v;
			try {
				v = fieldInfo.forceGet(bean);
				if (v != null) {
					fieldInfo.forceSet(proxy, v);
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		proxy.start_field_listen();
		return (T) proxy;
	}

	public static boolean checkProxy(Class<?> type, String[] filterNames) {
		if (Modifier.isFinal(type.getModifiers())) {// final修饰的类无法代理
			return false;
		}

		if (Filter.class.isAssignableFrom(type)) {
			return false;
		}

		boolean b = true;
		if (filterNames != null && filterNames.length != 0) {
			b = true;
		}

		for (Method method : type.getDeclaredMethods()) {
			Retry retry = getRetry(method);
			if (retry != null && retry.errors().length != 0) {
				b = true;
			}
		}

		Bean bean = type.getAnnotation(Bean.class);
		if (bean != null && !bean.proxy()) {
			b = false;
		}
		return b;
	}

	public static Retry getRetry(Method method) {
		Retry retry = method.getAnnotation(Retry.class);
		if (retry == null) {
			retry = method.getDeclaringClass().getAnnotation(Retry.class);
		}
		return retry;
	}
}

class InitProcess implements Runnable {
	private Invoker invoke;
	private CountDownLatch countDown;

	public InitProcess(Invoker invoke, CountDownLatch countDownLatch) {
		this.invoke = invoke;
		this.countDown = countDownLatch;
	}

	public void run() {
		try {
			invoke.invoke();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		countDown.countDown();
	}
}
