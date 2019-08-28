package scw.core.instance.support;

import java.lang.reflect.Constructor;

import scw.core.instance.InstanceFactory;
import scw.core.reflect.ReflectUtils;
import scw.core.utils.ClassUtils;
import scw.core.utils.StringUtils;

@SuppressWarnings("unchecked")
public class ReflectionInstanceFactory implements InstanceFactory {
	public <T> T getInstance(Class<T> type) {
		if (type == null) {
			return null;
		}

		Constructor<?> constructor = ReflectUtils.getConstructor(type, false);
		if (constructor == null) {
			return null;
		}

		return newInstance(constructor);
	}

	public static Class<?> forName(String className) {
		if (StringUtils.isEmpty(className)) {
			return null;
		}

		try {
			return Class.forName(className, false,
					ClassUtils.getDefaultClassLoader());
		} catch (ClassNotFoundException e) {
		}
		return null;
	}

	private <T> T newInstance(Constructor<?> constructor, Object... params) {
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
		}

		try {
			return (T) constructor.newInstance(params);
		} catch (Exception e) {
			return null;
		}
	}

	public <T> T getInstance(String name) {
		return (T) getInstance(forName(name));
	}

	public <T> T getInstance(String name, Object... params) {
		return (T) getInstance(forName(name), params);
	}

	public <T> T getInstance(Class<T> type, Object... params) {
		if (type == null) {
			return null;
		}

		Constructor<?> constructor = ReflectUtils.findConstructorByParameters(
				type, false, params);
		if (constructor == null) {
			return null;
		}

		return newInstance(constructor, params);
	}

	public <T> T getInstance(String name, Class<?>[] parameterTypes,
			Object... params) {
		return (T) getInstance(forName(name), parameterTypes, params);
	}

	public <T> T getInstance(Class<T> type, Class<?>[] parameterTypes,
			Object... params) {
		if (type == null) {
			return null;
		}

		Constructor<?> constructor = ReflectUtils.getConstructor(type, false,
				parameterTypes);
		if (constructor == null) {
			return null;
		}
		return newInstance(constructor, params);
	}

}
