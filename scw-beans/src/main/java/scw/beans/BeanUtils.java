package scw.beans;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import scw.aop.support.ProxyUtils;
import scw.beans.annotation.AopEnable;
import scw.beans.annotation.ConfigurationProperties;
import scw.beans.annotation.IgnoreConfigurationProperty;
import scw.beans.annotation.Service;
import scw.beans.annotation.Singleton;
import scw.context.ContextAware;
import scw.convert.TypeDescriptor;
import scw.core.annotation.AnnotatedElementUtils;
import scw.core.utils.StringUtils;
import scw.env.Environment;
import scw.env.EnvironmentAware;
import scw.env.Sys;
import scw.lang.Nullable;
import scw.logger.Levels;
import scw.mapper.Field;
import scw.orm.convert.EntityConversionService;
import scw.orm.convert.PropertyFactoryToEntityConversionService;
import scw.util.Accept;
import scw.util.StringMatchers;
import scw.value.PropertyFactory;
import scw.value.Value;

public final class BeanUtils {
	private static final List<AopEnableSpi> AOP_ENABLE_SPIS = Sys.env.getServiceLoader(AopEnableSpi.class).toList();

	private BeanUtils() {
	};

	public static boolean isSingleton(Class<?> type, AnnotatedElement annotatedElement) {
		Singleton singleton = annotatedElement.getAnnotation(Singleton.class);
		if (singleton != null) {
			return singleton.value();
		}

		for (Class<?> interfaceClass : type.getInterfaces()) {
			if (!isSingleton(interfaceClass, annotatedElement)) {
				return false;
			}
		}
		// 默认是单例
		return true;
	}

	public static Class<?> getServiceInterface(Class<?> clazz) {
		Class<?> classToUse = clazz;
		while (classToUse != null && classToUse != Object.class) {
			for (Class<?> i : classToUse.getInterfaces()) {
				if (AnnotatedElementUtils.isIgnore(classToUse) || i.getMethods().length == 0) {
					continue;
				}

				return i;
			}
			classToUse = classToUse.getSuperclass();
		}
		return null;
	}

	public static void aware(Object instance, BeanFactory beanFactory, BeanDefinition beanDefinition) {
		if (instance instanceof BeanFactoryAware) {
			((BeanFactoryAware) instance).setBeanFactory(beanFactory);
		}

		if (instance instanceof BeanDefinitionAware) {
			((BeanDefinitionAware) instance).setBeanDefinition(beanDefinition);
		}

		if (instance instanceof EnvironmentAware) {
			((EnvironmentAware) instance).setEnvironment(beanFactory.getEnvironment());
		}

		if (instance instanceof ContextAware) {
			((ContextAware) instance).setContext(beanFactory);
		}
	}

	public static RuntimeBean getRuntimeBean(Object instance) {
		if (instance == null) {
			return null;
		}

		if (instance instanceof RuntimeBean) {
			return ((RuntimeBean) instance);
		}

		return null;
	}

	/**
	 * 默认是不使用代理的，除非使用以下方式(see)：
	 * 
	 * @see AopEnable
	 * @see Service
	 * @see AopEnableSpi
	 * @param clazz
	 * @param annotatedElement
	 * @return
	 */
	public static boolean isAopEnable(Class<?> clazz, AnnotatedElement annotatedElement) {
		if (Modifier.isFinal(clazz.getModifiers())) {// final修饰的类无法代理
			return false;
		}

		AopEnable aopEnable = annotatedElement.getAnnotation(AopEnable.class);
		if (aopEnable != null) {
			return aopEnable.value();
		}

		aopEnable = clazz.getAnnotation(AopEnable.class);
		if (aopEnable != null) {
			return aopEnable.value();
		}

		// 如果是一个服务那么应该默认使用aop
		Service service = clazz.getAnnotation(Service.class);
		if (service != null) {
			return true;
		}

		for (AopEnableSpi spi : AOP_ENABLE_SPIS) {
			if (spi.isAopEnable(clazz, annotatedElement)) {
				return true;
			}
		}

		Class<?> classToUse = clazz.getSuperclass();
		while (classToUse != null && classToUse != Object.class) {
			if (isAopEnable(classToUse, classToUse)) {
				return true;
			}

			for (Class<?> interfaceClass : classToUse.getInterfaces()) {
				if (isAopEnable(interfaceClass, interfaceClass)) {
					return true;
				}
			}
			classToUse = classToUse.getSuperclass();
		}
		return false;
	}

	public static void configurationProperties(Object instance, @Nullable AnnotatedElement annotatedElement,
			Environment environment) {
		ConfigurationProperties configurationProperties = annotatedElement == null ? null
				: annotatedElement.getAnnotation(ConfigurationProperties.class);
		configurationProperties(instance, configurationProperties, environment);
	}

	public static void configurationProperties(Object instance, Environment environment, String prefix, Levels levels) {
		configurationProperties(instance, new ConfigurationProperties() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return ConfigurationProperties.class;
			}

			@Override
			public String value() {
				return null;
			}

			@Override
			public String prefix() {
				return prefix;
			}

			@Override
			public Levels loggerLevel() {
				return levels;
			}
		}, environment);
	}

	public static void configurationProperties(Object instance,
			@Nullable ConfigurationProperties configurationProperties, Environment environment) {
		Class<?> configurationPropertiesClass = ProxyUtils.getFactory().getUserClass(instance.getClass());
		if (configurationProperties == null) {
			// 定义上不存在此注解
			while (configurationPropertiesClass != null && configurationPropertiesClass != Object.class) {
				configurationProperties = configurationPropertiesClass.getAnnotation(ConfigurationProperties.class);
				if (configurationProperties != null) {
					EntityConversionService entityConversionService = createEntityConversionService(environment,
							configurationProperties);
					configurationMap(getPrefix(configurationProperties), instance, environment,
							entityConversionService);
					configurationProperties(configurationProperties, instance, configurationPropertiesClass,
							environment, entityConversionService);
					break;
				}
				configurationPropertiesClass = configurationPropertiesClass.getSuperclass();
			}
		} else {
			EntityConversionService entityConversionService = createEntityConversionService(environment,
					configurationProperties);
			configurationMap(getPrefix(configurationProperties), instance, environment, entityConversionService);
			configurationProperties(configurationProperties, instance, configurationPropertiesClass, environment,
					entityConversionService);
		}
	}

	public static EntityConversionService createEntityConversionService(Environment environment,
			ConfigurationProperties configurationProperties) {
		PropertyFactoryToEntityConversionService entityConversionService = new PropertyFactoryToEntityConversionService();
		entityConversionService.setConversionService(environment.getConversionService());
		entityConversionService.setStrict(false);
		entityConversionService.getFieldAccept().add(new Accept<Field>() {

			public boolean accept(Field field) {
				IgnoreConfigurationProperty ignore = field.getAnnotation(IgnoreConfigurationProperty.class);
				if (ignore != null) {
					return false;
				}

				// 如果字段上存在beans下的注解应该忽略此字段
				for (Annotation annotation : field.getAnnotations()) {
					if (annotation.annotationType().getName().startsWith("scw.beans.")) {
						return false;
					}
				}
				return true;
			}
		});
		if (configurationProperties != null) {
			entityConversionService.setPrefix(getPrefix(configurationProperties));
			entityConversionService.setLoggerLevel(configurationProperties.loggerLevel().getValue());
		}
		entityConversionService.setUseSuperClass(false);
		return entityConversionService;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void configurationMap(String prefix, Object instance, Environment environment,
			EntityConversionService conversionService) {
		if (!(instance instanceof Map)) {
			return;
		}

		Map properties = (Map) instance;
		TypeDescriptor descriptor = TypeDescriptor.forObject(instance);
		environment.stream(prefix, StringMatchers.PREFIX).forEach((key) -> {
			Value value = environment.getValue(key);
			if (value == null) {
				return;
			}

			String targetKey = StringUtils.isEmpty(prefix) ? key
					: key.substring(prefix.length() + conversionService.getConnector().length());
			Object mapKey = environment.getConversionService().convert(targetKey, TypeDescriptor.forObject(targetKey),
					descriptor.getMapKeyTypeDescriptor());
			Object mapValue = environment.getConversionService().convert(value, TypeDescriptor.forObject(value),
					descriptor.getMapValueTypeDescriptor());
			properties.put(mapKey, mapValue);
		});
	}

	private static String getPrefix(ConfigurationProperties configurationProperties) {
		return StringUtils.EMPTY.negate().first(configurationProperties.prefix(), configurationProperties.value());
	}

	private static void configurationProperties(ConfigurationProperties configurationProperties, Object instance,
			Class<?> configClass, Environment environment, EntityConversionService entityConversionService) {
		Class<?> clazz = configClass;
		while (clazz != null && clazz != Object.class) {
			ConfigurationProperties configuration = configurationProperties == null
					? clazz.getAnnotation(ConfigurationProperties.class)
					: configurationProperties;
			if (configuration != null) {
				entityConversionService.setPrefix(getPrefix(configuration));
				entityConversionService.setLoggerLevel(configuration.loggerLevel().getValue());
			}
			entityConversionService.configurationProperties(environment, TypeDescriptor.valueOf(PropertyFactory.class),
					instance, TypeDescriptor.valueOf(clazz));
			clazz = clazz.getSuperclass();
		}
	}
}
