package scw.orm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import scw.core.reflect.AnnotationFactory;
import scw.core.reflect.FieldDefinition;
import scw.core.reflect.ReflectUtils;
import scw.core.reflect.SimpleAnnotationFactory;

public abstract class AbstractFieldDefinition implements FieldDefinition {
	private final Field field;
	private final AnnotationFactory annotationFactory;
	private final Method getter;
	private final Method setter;
	private final Class<?> clazz;

	public AbstractFieldDefinition(Class<?> clazz, Field field, boolean getter, boolean setter) {
		this.clazz = clazz;
		this.field = field;
		this.annotationFactory = new SimpleAnnotationFactory(field);
		this.getter = getter ? ReflectUtils.getGetterMethod(clazz, field) : null;
		this.setter = setter ? ReflectUtils.getSetterMethod(clazz, field) : null;
	}

	public final <T extends Annotation> T getAnnotation(Class<T> type) {
		return annotationFactory.getAnnotation(type);
	}

	public final Field getField() {
		return field;
	}

	public final Object get(Object obj) throws Exception {
		return getter == null ? field.get(obj) : getter.invoke(obj);
	}

	public final void set(Object obj, Object value) throws Exception {
		if (setter == null) {
			field.set(obj, value);
		} else {
			setter.invoke(obj, value);
		}
	}

	public Class<?> getDeclaringClass() {
		return clazz;
	}
}
