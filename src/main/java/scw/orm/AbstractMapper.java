package scw.orm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import scw.core.reflect.AnnotationUtils;
import scw.core.utils.CollectionUtils;
import scw.core.utils.IteratorCallback;
import scw.core.utils.StringUtils;
import scw.lang.Ignore;
import scw.lang.Nullable;
import scw.orm.annotation.Entity;
import scw.orm.annotation.NotColumn;
import scw.orm.annotation.PrimaryKey;
import scw.orm.sql.SqlMapper;
import scw.orm.support.DefaultGetterFilterChain;
import scw.orm.support.DefaultObjectRelationalMapping;
import scw.orm.support.DefaultSetterFilterChain;
import scw.orm.support.SimpleGetter;

public abstract class AbstractMapper implements Mapper {
	public abstract char getPrimaryKeyConnectorCharacter();

	public abstract Collection<? extends SetterFilter> getSetterFilters();

	public abstract Collection<? extends GetterFilter> getGetterFilters();

	public void setter(MappingContext context, Object bean, Object value) throws ORMException {
		setter(context, new FieldSetter(bean), value);
	}

	public void setter(MappingContext context, Setter setter, Object value) throws ORMException {
		SetterFilterChain filterChain = new DefaultSetterFilterChain(getSetterFilters(), null);
		filterChain.setter(context, setter, value);
	}

	public Object getter(MappingContext context, Getter getter) throws ORMException {
		GetterFilterChain filterChain = new DefaultGetterFilterChain(getGetterFilters(), null);
		return filterChain.getter(context, getter);
	}

	public Object getter(MappingContext context, Object bean) throws ORMException {
		return getter(context, new FieldGetter(bean));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <T> void create(Class<T> declaringClass, MappingContext superContext, Class<?> clazz, T bean,
			SetterMapping setterMapping) throws ORMException {
		Map<String, Column> map = getColumnMap(clazz);
		for (Entry<String, Column> entry : map.entrySet()) {
			MappingContext context = new MappingContext(superContext, entry.getValue(), declaringClass);
			if (isIgnore(context)) {
				continue;
			}

			if (isEntity(context)) {
				setter(context, bean, create(context, context.getColumn().getDeclaringClass(), setterMapping));
			} else {
				setterMapping.setter(context, bean, this);
			}
		}

		Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null && superClazz != Object.class) {
			create(declaringClass, superContext, superClazz, bean, setterMapping);
		}
	}

	public <T> T create(MappingContext superContext, Class<T> clazz, SetterMapping<? extends Mapper> setterMapping)
			throws ORMException {
		T bean = newInstance(clazz);
		create(clazz, superContext, clazz, bean, setterMapping);
		return bean;
	}

	public void iterator(MappingContext superContext, Class<?> clazz, IteratorMapping<? extends Mapper> iterator)
			throws ORMException {
		iterator(clazz, superContext, clazz, iterator);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void iterator(Class<?> declaringClass, MappingContext superContext, Class<?> clazz,
			IteratorMapping iterator) throws ORMException {
		Map<String, Column> map = getColumnMap(clazz);
		for (Entry<String, Column> entry : map.entrySet()) {
			MappingContext context = new MappingContext(superContext, entry.getValue(), declaringClass);
			if (isIgnore(context)) {
				continue;
			}

			iterator.iterator(context, this);
		}

		Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null && superClazz != Object.class) {
			iterator(declaringClass, superContext, superClazz, iterator);
		}
	}

	protected void appendMappingContexts(Class<?> declaringClass, MappingContext superContext, Class<?> clazz,
			List<MappingContext> list, IteratorCallback<MappingContext> filter) {
		Map<String, Column> map = getColumnMap(clazz);
		for (Entry<String, Column> entry : map.entrySet()) {
			MappingContext context = new MappingContext(superContext, entry.getValue(), declaringClass);
			if (isIgnore(context)) {
				continue;
			}

			if (filter == null || filter.iteratorCallback(context)) {
				list.add(context);
			}
		}

		Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null && superClazz != Object.class) {
			appendMappingContexts(declaringClass, superContext, superClazz, list, filter);
		}
	}

	public Collection<MappingContext> getMappingContexts(MappingContext superContext, Class<?> clazz,
			IteratorCallback<MappingContext> filter) {
		LinkedList<MappingContext> list = new LinkedList<MappingContext>();
		appendMappingContexts(clazz, superContext, clazz, list, filter);
		return list;
	}

	public Collection<MappingContext> getMappingContexts(Class<?> clazz, IteratorCallback<MappingContext> filter) {
		return getMappingContexts(null, clazz, filter);
	}

	public boolean isPrimaryKey(MappingContext context) {
		return context.getColumn().getAnnotation(PrimaryKey.class) != null;
	}

	public Collection<MappingContext> getPrimaryKeys(MappingContext supperContext, Class<?> clazz) {
		return getMappingContexts(supperContext, clazz, new IteratorCallback<MappingContext>() {

			public boolean iteratorCallback(MappingContext data) {
				return isPrimaryKey(data);
			}
		});
	}

	public Collection<MappingContext> getPrimaryKeys(Class<?> clazz) {
		return getPrimaryKeys(null, clazz);
	}

	protected void appendObjectKeyByValue(Appendable appendable, Object value) {
		try {
			appendable.append(getPrimaryKeyConnectorCharacter());
			appendable.append(StringUtils.transferredMeaning(value == null ? null : value.toString(),
					getPrimaryKeyConnectorCharacter()));
		} catch (IOException e) {
			throw new ORMException("append object key error value=[" + value + "]", e);
		}
	}

	public <T> String getObjectKey(Class<? extends T> clazz, final T bean) {
		final StringBuilder sb = new StringBuilder(128);
		sb.append(clazz.getName());
		try {
			iterator(null, clazz, new IteratorMapping<SqlMapper>() {

				public void iterator(MappingContext context, SqlMapper mappingOperations) throws ORMException {
					if (isPrimaryKey(context)) {
						appendObjectKeyByValue(sb, getter(context, bean));
					}
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}

	public String getObjectKeyById(Class<?> clazz, Collection<Object> primaryKeys) {
		StringBuilder sb = new StringBuilder(128);
		sb.append(clazz.getName());
		Iterator<MappingContext> iterator = getPrimaryKeys(clazz).iterator();
		Iterator<Object> valueIterator = primaryKeys.iterator();
		while (iterator.hasNext() && valueIterator.hasNext()) {
			try {
				appendObjectKeyByValue(sb, getter(iterator.next(), new SimpleGetter(valueIterator.next())));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public <K> Map<String, K> getInIdKeyMap(Class<?> clazz, Collection<K> lastPrimaryKeys, Object[] primaryKeys) {
		if (CollectionUtils.isEmpty(lastPrimaryKeys)) {
			return Collections.EMPTY_MAP;
		}

		Map<String, K> keyMap = new HashMap<String, K>();
		Iterator<K> valueIterator = lastPrimaryKeys.iterator();

		while (valueIterator.hasNext()) {
			K k = valueIterator.next();
			Object[] ids;
			if (primaryKeys == null || primaryKeys.length == 0) {
				ids = new Object[] { k };
			} else {
				ids = new Object[primaryKeys.length];
				System.arraycopy(primaryKeys, 0, ids, 0, primaryKeys.length);
				ids[ids.length - 1] = k;
			}
			keyMap.put(getObjectKeyById(clazz, Arrays.asList(ids)), k);
		}
		return keyMap;
	}

	public boolean isIgnore(MappingContext context) {
		Ignore ignore = context.getColumn().getAnnotation(Ignore.class);
		if (ignore != null) {
			return true;
		}

		NotColumn exclude = context.getColumn().getAnnotation(NotColumn.class);
		if (exclude != null) {
			return true;
		}
		return false;
	}

	public boolean isEntity(MappingContext context) {
		return context.getColumn().getAnnotation(Entity.class) != null
				|| context.getColumn().getType().getAnnotation(Entity.class) != null;
	}

	public Collection<MappingContext> getNotPrimaryKeys(MappingContext supperContext, Class<?> clazz) {
		return getMappingContexts(supperContext, clazz, new IteratorCallback<MappingContext>() {

			public boolean iteratorCallback(MappingContext data) {
				return !isPrimaryKey(data) && !isEntity(data);
			}
		});
	}

	public Collection<MappingContext> getNotPrimaryKeys(Class<?> clazz) {
		return getNotPrimaryKeys(null, clazz);
	}

	public ObjectRelationalMapping getObjectRelationalMapping(Class<?> clazz) {
		return getObjectRelationalMapping(null, clazz);
	}

	public ObjectRelationalMapping getObjectRelationalMapping(MappingContext superContext, Class<?> clazz) {
		List<MappingContext> primaryKeys = new ArrayList<MappingContext>(4);
		List<MappingContext> notPrimaryKeys = new ArrayList<MappingContext>(8);
		List<MappingContext> entitys = new ArrayList<MappingContext>(4);
		Map<String, MappingContext> contextMap = new LinkedHashMap<String, MappingContext>();
		appendObjectRelationalMapping(clazz, superContext, clazz, primaryKeys, notPrimaryKeys, entitys, contextMap);
		return new DefaultObjectRelationalMapping(primaryKeys, notPrimaryKeys, entitys, contextMap);
	}

	protected void appendObjectRelationalMapping(Class<?> declaringClass, MappingContext superContext, Class<?> clazz,
			Collection<MappingContext> primaryKeys, Collection<MappingContext> notPrimaryKeys,
			Collection<MappingContext> entitys, Map<String, MappingContext> contextMap) {
		Map<String, scw.orm.Column> map = getColumnMap(clazz);
		for (Entry<String, scw.orm.Column> entry : map.entrySet()) {
			MappingContext context = new MappingContext(superContext, entry.getValue(), declaringClass);
			if (isIgnore(context)) {
				continue;
			}

			if (isEntity(context)) {
				entitys.add(context);
			} else if (isPrimaryKey(context)) {
				primaryKeys.add(context);
			} else {
				notPrimaryKeys.add(context);
			}

			contextMap.put(context.getColumn().getName(), context);
		}

		Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null && superClazz != Object.class) {
			appendObjectRelationalMapping(declaringClass, superContext, superClazz, primaryKeys, notPrimaryKeys,
					entitys, contextMap);
		}
	}

	public boolean isNullable(MappingContext context) {
		if (context.getColumn().getType().isPrimitive()) {
			return false;
		}

		if (isPrimaryKey(context)) {
			return false;
		}

		Nullable nullable = AnnotationUtils.getAnnotation(Nullable.class, context.getDeclaringClass(),
				context.getColumn());
		if (nullable == null) {
			return true;
		}
		return nullable.value();
	}
}
