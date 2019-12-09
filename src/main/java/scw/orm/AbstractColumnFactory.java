package scw.orm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import scw.core.reflect.ReflectionUtils;
import scw.lang.AlreadyExistsException;
import scw.orm.support.DefaultColumn;

public abstract class AbstractColumnFactory implements ColumnFactory {

	protected Column analysisField(Class<?> clazz, Field field) {
		return new DefaultColumn(clazz, field);
	}

	protected LinkedHashMap<String, Column> analysisClass(Class<?> clazz) {
		LinkedHashMap<String, Column> map = new LinkedHashMap<String, Column>();
		for (Field field : clazz.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}

			ReflectionUtils.setAccessibleField(field);
			Column column = analysisField(clazz, field);
			if (map.containsKey(column.getName())) {
				throw new AlreadyExistsException("Class " + clazz.getName() + " column " + column.getName());
			}
			map.put(column.getName(), column);
		}
		return map;
	}

	public Column getColumn(Class<?> clazz, String name) {
		Map<String, Column> map = getColumnMap(clazz);
		return map == null ? null : map.get(name);
	}
}
