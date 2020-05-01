package scw.orm;

import java.util.Collection;
import java.util.Map;

import scw.core.IteratorCallback;

public interface Mapper extends ColumnFactory {

	void setter(MappingContext context, Object bean, Object value) throws ORMException;

	void setter(MappingContext context, Setter setter, Object value) throws ORMException;

	Object getter(MappingContext context, Object bean) throws ORMException;

	Object getter(MappingContext context, Getter getter) throws ORMException;
	
	ORMInstanceFactory getInstanceFactory();

	<T, M extends Mapper> T create(MappingContext superContext, Class<T> clazz, SetterMapping<M> setterMapping)
			throws ORMException;

	<M extends Mapper> void iterator(MappingContext superContext, Class<?> clazz, IteratorMapping<M> iterator);

	boolean isPrimaryKey(MappingContext mappingContext);

	boolean isIgnore(MappingContext context);

	boolean isEntity(MappingContext context);

	boolean isNullable(MappingContext context);

	Collection<MappingContext> getPrimaryKeys(MappingContext supperContext, Class<?> clazz);

	Collection<MappingContext> getPrimaryKeys(Class<?> clazz);

	<T> String getObjectKey(Class<? extends T> clazz, T bean) throws ORMException;

	String getObjectKeyById(Class<?> clazz, Collection<Object> primaryKeys) throws ORMException;

	<K> Map<String, K> getInIdKeyMap(Class<?> clazz, Collection<? extends K> lastPrimaryKeys, Object[] primaryKeys)
			throws ORMException;

	Collection<MappingContext> getNotPrimaryKeys(MappingContext supperContext, Class<?> clazz);

	Collection<MappingContext> getNotPrimaryKeys(Class<?> clazz);

	Collection<MappingContext> getMappingContexts(Class<?> clazz, IteratorCallback<MappingContext> filter);

	Collection<MappingContext> getMappingContexts(MappingContext superContext, Class<?> clazz,
			IteratorCallback<MappingContext> filter);

	ObjectRelationalMapping getObjectRelationalMapping(MappingContext superContext, Class<?> clazz);

	ObjectRelationalMapping getObjectRelationalMapping(Class<?> clazz);
}