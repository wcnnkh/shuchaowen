package scw.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import scw.convert.ConfigurableConversionService;
import scw.convert.resolve.ConfigurableResourceResolver;
import scw.convert.resolve.ResourceResolverConversionService;
import scw.convert.resolve.support.DefaultResourceResolver;
import scw.convert.support.DefaultConversionService;
import scw.core.Assert;
import scw.core.utils.ArrayUtils;
import scw.core.utils.ClassUtils;
import scw.core.utils.StringUtils;
import scw.env.ObservablePropertiesPropertyFactory.ValueCreator;
import scw.event.Observable;
import scw.instance.Configurable;
import scw.instance.ConfigurableServices;
import scw.instance.ServiceLoaderFactory;
import scw.io.FileSystemResourceLoader;
import scw.io.ProtocolResolver;
import scw.io.Resource;
import scw.io.ResourceLoader;
import scw.io.ResourceUtils;
import scw.io.resolver.ConfigurablePropertiesResolver;
import scw.io.resolver.support.PropertiesResolvers;
import scw.lang.Nullable;
import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.util.ClassLoaderProvider;
import scw.util.ConcurrentReferenceHashMap;
import scw.util.DefaultClassLoaderProvider;
import scw.util.MultiIterator;
import scw.util.placeholder.ConfigurablePlaceholderReplacer;
import scw.util.placeholder.support.DefaultPlaceholderReplacer;
import scw.value.AnyValue;
import scw.value.PropertyFactory;
import scw.value.StringValue;
import scw.value.Value;
import scw.value.support.DefaultPropertyFactory;

public class DefaultEnvironment extends DefaultPropertyFactory implements ConfigurableEnvironment, Configurable {
	private static final String[] SUFFIXS = new String[] { "scw_res_suffix", "SHUCHAOWEN_CONFIG_SUFFIX",
			"resource.suffix" };
	private static Logger logger = LoggerFactory.getLogger(DefaultEnvironment.class);

	private final ConcurrentReferenceHashMap<String, Resource> cacheMap = new ConcurrentReferenceHashMap<String, Resource>();
	private final FileSystemResourceLoader configurableResourceLoader = new FileSystemResourceLoader() {
		protected boolean ignoreClassPathResource(scw.io.FileSystemResource resource) {
			return super.ignoreClassPathResource(resource) || resource.getPath().startsWith(getWorkPath());
		};
	};

	private final PropertiesResolvers configurablePropertiesResolver = new PropertiesResolvers();
	private final DefaultConversionService configurableConversionService = new DefaultConversionService(
			configurablePropertiesResolver, getObservableCharset());
	private final DefaultResourceResolver configurableResourceResolver = new DefaultResourceResolver(
			configurableConversionService, configurablePropertiesResolver, getObservableCharset());
	private final DefaultPlaceholderReplacer placeholderReplacer = new DefaultPlaceholderReplacer();
	private ClassLoaderProvider classLoaderProvider;
	
	public DefaultEnvironment() {
		this(null);
	}
	
	public DefaultEnvironment(@Nullable ClassLoaderProvider classLoaderProvider) {
		super(true);
		this.classLoaderProvider = classLoaderProvider;
		configurableResourceLoader.setClassLoaderProvider(this);
		configurableConversionService
				.addConversionService(new ResourceResolverConversionService(configurableResourceResolver));
	}
	
	public void setClassLoaderProvider(ClassLoaderProvider classLoaderProvider) {
		this.classLoaderProvider = classLoaderProvider;
	}

	public void setClassLoader(ClassLoader classLoader) {
		setClassLoaderProvider(new DefaultClassLoaderProvider(classLoader));
	}
	
	@Override
	public void addProtocolResolver(ProtocolResolver resolver) {
		configurableResourceLoader.addProtocolResolver(resolver);
	}

	@Override
	public void addResourceLoader(ResourceLoader resourceLoader) {
		configurableResourceLoader.addResourceLoader(resourceLoader);
	}

	private Resource getResourceByCache(String location) {
		Resource resource = cacheMap.get(location);
		if (resource == null) {
			resource = configurableResourceLoader.getResource(location);
			Resource cache = cacheMap.putIfAbsent(location, resource);
			if (cache != null) {
				resource = cache;
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Find resource {} result {}", location, resource);
				}
			}
			if (resource == null) {
				cacheMap.putIfAbsent(location, ResourceUtils.NONEXISTENT_RESOURCE);
			}
		}
		return resource;
	}

	/**
	 * 可使用的资源别名，使用优先级从左到右
	 * 
	 * @return
	 */
	protected String[] getResourceEnvironmentalNames() {
		Value value = null;
		for (String suffix : SUFFIXS) {
			value = getValue(suffix);
			if (value != null && !value.isEmpty()) {
				return value.getAsObject(String[].class);
			}
		}
		return StringUtils.MEPTY_ARRAY;
	}

	/**
	 * 预计使用的资源列表，返回的资源并不一定存在, 使用优先级从高到低
	 * 
	 * @param resourceName
	 * @return
	 */
	public List<String> getEnvironmentalResourceNameList(String resourceName) {
		String[] suffixs = getResourceEnvironmentalNames();
		String resourceNameToUse = resolvePlaceholders(resourceName);
		if (ArrayUtils.isEmpty(suffixs)) {
			return Arrays.asList(resourceNameToUse);
		}

		List<String> list = new ArrayList<String>(suffixs.length + 1);
		for (int i = suffixs.length - 1; i >= 0; i--) {
			list.add(getEnvironmentalResourceName(resourceNameToUse, suffixs[i]));
		}
		list.add(resourceNameToUse);
		return list;
	}

	protected String getEnvironmentalResourceName(String resourceName, String evnironmental) {
		int index = resourceName.lastIndexOf(".");
		if (index == -1) {// 不存在
			return resourceName + evnironmental;
		} else {
			return resourceName.substring(0, index) + evnironmental + resourceName.substring(index);
		}
	};

	public Resource[] getResources(String locationPattern) {
		List<String> nameList = getEnvironmentalResourceNameList(locationPattern);
		List<Resource> resources = new ArrayList<Resource>(nameList.size());
		for (String name : nameList) {
			Resource res = getResourceByCache(name);
			if (res == null) {
				continue;
			}
			resources.add(res);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Get resources [{}] results {}", resources);
		}
		return resources.toArray(new Resource[0]);
	}

	protected void aware(Object instance) {
		if (instance == null) {
			return;
		}

		if (instance instanceof EnvironmentAware) {
			((EnvironmentAware) instance).setEnvironment(this);
		}
	}

	@Override
	public void addFactory(PropertyFactory propertyFactory) {
		aware(propertyFactory);
		super.addFactory(propertyFactory);
	}

	public ClassLoader getClassLoader() {
		return ClassUtils.getClassLoader(classLoaderProvider);
	}

	public boolean put(String key, Object value) {
		Assert.requiredArgument(key != null, "key");
		Assert.requiredArgument(value != null, "value");
		return put(key, toProperty(value));
	}

	public Value toProperty(Object value) {
		Value v;
		if (value instanceof Value) {
			return (Value) value;
		} else if (value instanceof String) {
			v = new StringFormatValue((String) value);
		} else {
			v = new AnyFormatValue(value);
		}
		return v;
	}

	public boolean putIfAbsent(String key, Object value) {
		Assert.requiredArgument(key != null, "key");
		Assert.requiredArgument(value != null, "value");
		return putIfAbsent(key, toProperty(value));
	}

	public void loadProperties(String keyPrefix, Observable<Properties> properties) {
		ValueCreator valueCreator = new ValueCreator() {

			public Value create(String key, Object value) {
				return toProperty(value);
			}
		};

		ObservablePropertiesPropertyFactory factory = new ObservablePropertiesPropertyFactory(properties, keyPrefix,
				valueCreator);
		addFactory(factory);
	}

	private class StringFormatValue extends StringValue {
		private static final long serialVersionUID = 1L;

		public StringFormatValue(String value) {
			super(value);
		}

		@Override
		public String getAsString() {
			return resolvePlaceholders(super.getAsString());
		}
	}

	private class AnyFormatValue extends AnyValue {
		private static final long serialVersionUID = 1L;

		public AnyFormatValue(Object value) {
			super(value, DefaultEnvironment.this.getConversionService());
		}

		public String getAsString() {
			return resolvePlaceholders(super.getAsString());
		};
	}
	
	private ConfigurableServices<PropertyFactory> propertyFactorys = new ConfigurableServices<>(PropertyFactory.class, (s) -> aware(s));
	
	@Override
	public Iterator<PropertyFactory> getFactories() {
		return new MultiIterator<>(super.getFactories(), propertyFactorys.iterator());
	}
	
	@Override
	public void configure(ServiceLoaderFactory serviceLoaderFactory) {
		configurablePropertiesResolver.configure(serviceLoaderFactory);
		configurableResourceResolver.configure(serviceLoaderFactory);
		configurableConversionService.configure(serviceLoaderFactory);
		propertyFactorys.configure(serviceLoaderFactory);
		placeholderReplacer.configure(serviceLoaderFactory);
	}

	@Override
	public ConfigurablePlaceholderReplacer getPlaceholderReplacer() {
		return placeholderReplacer;
	}

	@Override
	public ConfigurablePropertiesResolver getPropertiesResolver() {
		return configurablePropertiesResolver;
	}

	@Override
	public ConfigurableConversionService getConversionService() {
		return configurableConversionService;
	}

	@Override
	public ConfigurableResourceResolver getResourceResolver() {
		return configurableResourceResolver;
	}
}