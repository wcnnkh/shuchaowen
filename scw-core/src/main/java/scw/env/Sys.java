package scw.env;

import java.util.Iterator;
import java.util.Properties;

import scw.convert.ConvertibleEnumeration;
import scw.core.utils.CollectionUtils;
import scw.event.Observable;
import scw.instance.InstanceDefinition;
import scw.instance.InstanceFactory;
import scw.instance.NoArgsInstanceFactory;
import scw.instance.ServiceLoader;
import scw.instance.ServiceLoaderFactory;
import scw.instance.support.DefaultInstanceFactory;
import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.util.Clock;
import scw.util.MultiIterator;
import scw.util.XUtils;
import scw.value.StringValue;
import scw.value.Value;

/**
 * 不使用System这个名称的原因是名称冲突
 * 
 * @author shuchaowen
 *
 */
public final class Sys extends DefaultEnvironment implements ServiceLoaderFactory, InstanceFactory {
	/**
	 * 为了兼容老版本
	 */
	public static final String WEB_ROOT_PROPERTY = "web.root";

	private static Logger logger = LoggerFactory.getLogger(Sys.class);
	public static final Sys env = new Sys();
	private static DefaultInstanceFactory instanceFactory = new DefaultInstanceFactory(
			env, true);

	private static Clock clock;

	/**
	 * 不调用构造方法实例化对象
	 */
	private static NoArgsInstanceFactory unsafeInstanceFactory;

	static {
		env.load();
		unsafeInstanceFactory = instanceFactory.getServiceLoader(
				NoArgsInstanceFactory.class,
				"scw.instance.support.SunNoArgsInstanceFactory",
				"scw.instance.support.UnsafeNoArgsInstanceFactory").first();
		clock = env.getServiceLoader(Clock.class).first();
		if (clock == null) {
			clock = Clock.SYSTEM;
		}
	}

	private void load() {
		/**
		 * 加载配置文件
		 */
		loadProperties("system.properties");
		loadProperties(getValue("system.properties.location", String.class,
				"/private.properties"));

		// 初始化日志管理器
		Observable<Properties> observable = getProperties(getValue(
				"scw.logger.level.config", String.class,
				"/logger-level.properties"));
		LoggerFactory.getLevelManager().combine(observable);

		/**
		 * 加载默认服务
		 */
		configure(instanceFactory);
	}

	/**
	 * 不安全的实例工厂<br/>
	 * 不调用构造方法实例化对象
	 * 
	 * @return
	 */
	public static NoArgsInstanceFactory getUnsafeInstanceFactory() {
		return unsafeInstanceFactory;
	}

	public static Clock getClock() {
		return clock;
	}

	/**
	 * @see Clock#currentTimeMillis()
	 * @return
	 */
	public static long currentTimeMillis() {
		return clock.currentTimeMillis();
	}

	public Iterator<String> iterator() {
		return new MultiIterator<String>(super.iterator(),
				CollectionUtils.toIterator(ConvertibleEnumeration
						.convertToStringEnumeration(System.getProperties()
								.keys())), System.getenv().keySet().iterator());
	}

	private Sys() {
	}

	public Value getValue(String key) {
		Value value = super.getValue(key);
		if (value != null) {
			return value;
		}

		String v = getSystemProperty(key);
		return v == null ? null : new StringValue(v);
	}

	public String getSystemProperty(String key) {
		String value = System.getProperty(key);
		if (value == null) {
			value = System.getenv(key);
		}

		if (value == null && WEB_ROOT_PROPERTY.equals(key)) {
			value = getWorkPath();
		}
		return value;
	}

	@Override
	public boolean containsKey(String key) {
		if (super.containsKey(key)) {
			return true;
		}

		return getSystemProperty(key) != null;
	}

	/**
	 * 获取工作目录
	 */
	@Override
	public String getWorkPath() {
		String path = super.getWorkPath();
		if (path == null) {
			path = XUtils.getWebAppDirectory(getClassLoader());
			if (path != null) {
				setWorkPath(path);
				logger.info("default " + WORK_PATH_PROPERTY + " in " + path);
			}
		}
		return path;
	}

	@Override
	public <T> T getInstance(Class<T> clazz) {
		return instanceFactory.getInstance(clazz);
	}

	@Override
	public boolean isInstance(Class<?> clazz) {
		return instanceFactory.isInstance(clazz);
	}
	
	@Override
	public boolean isInstance(String name) {
		return instanceFactory.isInstance(name);
	}
	
	@Override
	public <T> T getInstance(String name) {
		return instanceFactory.getInstance(name);
	}

	@Override
	public <S> ServiceLoader<S> getServiceLoader(Class<S> serviceClass) {
		return instanceFactory.getServiceLoader(serviceClass);
	}

	@Override
	public InstanceDefinition getDefinition(String name) {
		return instanceFactory.getDefinition(name);
	}

	@Override
	public InstanceDefinition getDefinition(Class<?> clazz) {
		return instanceFactory.getDefinition(clazz);
	}

	@Override
	public boolean isInstance(String name, Object... params) {
		return instanceFactory.isInstance(name, params);
	}

	@Override
	public <T> T getInstance(String name, Object... params) {
		return instanceFactory.getInstance(name, params);
	}

	@Override
	public boolean isInstance(Class<?> clazz, Object... params) {
		return instanceFactory.isInstance(clazz, params);
	}

	@Override
	public <T> T getInstance(Class<T> clazz, Object... params) {
		return instanceFactory.getInstance(clazz, params);
	}

	@Override
	public boolean isInstance(String name, Class<?>[] parameterTypes) {
		return instanceFactory.isInstance(name, parameterTypes);
	}

	@Override
	public <T> T getInstance(String name, Class<?>[] parameterTypes, Object[] params) {
		return instanceFactory.getInstance(name, parameterTypes, params);
	}

	@Override
	public boolean isInstance(Class<?> clazz, Class<?>[] parameterTypes) {
		return instanceFactory.isInstance(clazz, parameterTypes);
	}

	@Override
	public <T> T getInstance(Class<T> clazz, Class<?>[] parameterTypes, Object[] params) {
		return instanceFactory.getInstance(clazz, parameterTypes, params);
	}
}
