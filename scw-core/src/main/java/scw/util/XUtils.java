package scw.util;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import scw.core.reflect.ReflectionUtils;
import scw.core.utils.ClassUtils;
import scw.core.utils.CollectionUtils;
import scw.core.utils.StringUtils;
import scw.io.FileUtils;
import scw.lang.Ignore;
import scw.lang.NotSupportedException;
import scw.lang.Nullable;
import scw.mapper.FieldFeature;
import scw.mapper.MapperUtils;
import scw.value.Value;

public final class XUtils {
	private XUtils() {
	};

	/**
	 * 获取UUID，已经移除了‘-’
	 * 
	 * @return
	 */
	public static String getUUID() {
		return StringUtils.removeChar(UUID.randomUUID().toString(), '-');
	}

	/**
	 * 递归解析
	 * 
	 * @param instance
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Map toMap(Object instance) {
		return toMap(instance, true);
	}

	/**
	 * 将对象转换为map
	 * 
	 * @see ValueUtils#isBaseType(Class) 此类型无法解析
	 * @param instance
	 * @param recursion
	 *            是否递归
	 * @return
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Map toMap(Object instance, boolean recursion) {
		if (instance == null) {
			return Collections.emptyMap();
		}

		if (instance instanceof ToMap) {
			ToMap toMap = (ToMap) instance;
			return recursion ? parseMap(toMap.toMap()) : toMap.toMap();
		} else if (instance instanceof Map) {
			return parseMap((Map) instance);
		}

		if (Value.isBaseType(instance.getClass())) {
			throw new NotSupportedException(instance.getClass().getName());
		}

		Map<String, Object> valueMap = MapperUtils.getMapper()
				.getFields(instance.getClass())
				.accept(FieldFeature.IGNORE_STATIC).getValueMap(instance);
		return recursion ? parseMap(valueMap) : valueMap;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parseMap(Map map) {
		if (CollectionUtils.isEmpty(map)) {
			return Collections.EMPTY_MAP;
		}

		Set<Entry> entries = map.entrySet();
		Map valueMap = new LinkedHashMap();
		for (Entry entry : entries) {
			valueMap.put(entry.getKey(), parseValue(entry.getValue()));
		}
		return valueMap;
	}

	@SuppressWarnings({ "rawtypes" })
	private static Object parseValue(Object value) {
		if (value == null) {
			return value;
		}

		if (Value.isBaseType(value.getClass())) {
			return value;
		} else if (value instanceof ToMap) {
			return parseMap(((ToMap) value).toMap());
		} else if (value instanceof Collection) {
			Collection list = (Collection) value;
			if (CollectionUtils.isEmpty(list)) {
				return value;
			}

			List<Object> newList = new ArrayList<Object>(list.size());
			for (Object v : list) {
				newList.add(parseValue(v));
			}
			return newList;
		} else if (value instanceof Map) {
			return parseMap((Map) value);
		} else if (value.getClass().isArray()) {
			int len = Array.getLength(value);
			if (len == 0) {
				return value;
			}

			Object[] array = new Object[len];
			for (int i = 0; i < len; i++) {
				array[i] = parseValue(Array.get(value, i));
			}
			return array;
		} else {
			Map<String, Object> valueMap = MapperUtils.getMapper()
					.getFields(value.getClass())
					.accept(FieldFeature.IGNORE_STATIC).getValueMap(value);
			return parseMap(valueMap);
		}
	}

	public static void appendToMap(Properties properties,
			Map<String, String> map) {
		if (properties == null || map == null) {
			return;
		}

		for (Entry<Object, Object> entry : properties.entrySet()) {
			map.put(entry.getKey() == null ? null : entry.getKey().toString(),
					entry.getValue() == null ? null : entry.getValue()
							.toString());
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T getDelegate(Object wrapper, Class<T> targetType) {
		if (targetType.isInstance(wrapper)) {
			return (T) wrapper;
		}

		if (wrapper instanceof Decorator) {
			return ((Decorator) wrapper).getDelegate(targetType);
		}
		return null;
	}

	public static Comparator<String> getComparator(final StringMatcher matcher) {
		return new Comparator<String>() {

			public int compare(String o1, String o2) {
				if (matcher.isPattern(o1) && matcher.isPattern(o2)) {
					if (matcher.match(o1, o2)) {
						return 1;
					} else if (matcher.match(o2, o1)) {
						return -1;
					} else {
						return -1;
					}
				} else if (matcher.isPattern(o1)) {
					return 1;
				} else if (matcher.isPattern(o2)) {
					return -1;
				}
				return o1.equals(o1) ? 0 : -1;
			}
		};
	}

	/**
	 * 获取名称
	 * 
	 * @see Named#getName()
	 * @param instance
	 * @param defaultName
	 * @return
	 */
	public static String getName(Object instance, String defaultName) {
		if (instance == null) {
			return defaultName;
		}

		if (instance instanceof Named) {
			String name = ((Named) instance).getName();
			return name == null ? defaultName : name;
		}

		return defaultName;
	}

	/**
	 * 获取运行时所在的目录
	 * 
	 * @param classLoader
	 * @return
	 */
	public static String getRuntimeDirectory(ClassLoader classLoader) {
		URL url = classLoader.getResource("");
		return url == null ? FileUtils.getUserDir() : url.getPath();
	}
	
	/**
	 * 获取webapp目录
	 * 
	 * @param classLoader
	 * @return
	 */
	public static String getWebAppDirectory(ClassLoader classLoader) {
		// /xxxxx/{project}/target/classes
		String path = getRuntimeDirectory(classLoader);
		File file = new File(path);
		if (file.isFile()) {
			return null;
		}

		if (!file.getName().equals("classes")) {
			return path;
		}

		for (int i = 0; i < 2; i++) {
			file = file.getParentFile();
			if (file == null) {
				return path;
			}

			if (file.getName().equals("WEB-INF") && file.getParent() != null) {
				return file.getParent();
			}
		}

		if (file != null) {
			File webapp = new File(file, "/src/main/webapp");
			if (webapp.exists()) {
				return webapp.getPath();
			}
			/*
			 * //可能会出现一个bin目录，忽略此目录 final File binDirectory = new File(file, "bin"); //
			 * 路径/xxxx/src/main/webapp/WEB-INF 4层深度 File wi = FileUtils.search(file, new
			 * Accept<File>() {
			 * 
			 * public boolean accept(File e) { if(e.isDirectory() &&
			 * "WEB-INF".equals(e.getName())){ //可能会出现一个bin目录，忽略此目录 if(binDirectory.exists()
			 * && binDirectory.isDirectory() &&
			 * e.getPath().startsWith(binDirectory.getPath())){ return false; } return true;
			 * } return false; } }, 4); if (wi != null) { return wi.getParent(); }
			 */
		}
		return path;
	}
	
	/**
	 * 此类是否可用
	 * @param clazz
	 * @return
	 */
	public static boolean isAvailable(Class<?> clazz) {
		if(clazz == null){
			return false;
		}
		
		if(ClassUtils.isPrimitiveOrWrapper(clazz) || clazz.isAnnotationPresent(Ignore.class)){
			return false;
		}
		
		return ReflectionUtils.isSupported(clazz) && JavaVersion.isSupported(clazz);
	}
	
	public static String getClassPath() {
		return System.getProperty("java.class.path");
	}
	
	/**
	 * 获取环境变量分割符
	 * 
	 * @return
	 */
	public static String getPathSeparator() {
		return System.getProperty("path.separator");
	}

	public static String[] getClassPathArray() {
		String classPath = getClassPath();
		if (StringUtils.isEmpty(classPath)) {
			return null;
		}

		return StringUtils.split(classPath, getPathSeparator());
	}
	
	public static String getOSName() {
		return System.getProperty("os.name");
	}
}