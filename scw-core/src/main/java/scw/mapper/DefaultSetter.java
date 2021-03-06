package scw.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import scw.core.annotation.AnnotatedElementUtils;
import scw.core.parameter.DefaultParameterDescriptor;
import scw.core.parameter.ParameterDescriptor;
import scw.core.utils.ArrayUtils;
import scw.value.Value;

public class DefaultSetter extends AbstractFieldDescriptor implements Setter {
	private final String name;
	private final boolean nullable;
	private ParameterDescriptor setterParameterDescriptor;

	public DefaultSetter(Class<?> declaringClass, String name, Field field, Method method) {
		super(declaringClass, field, method);
		this.name = name;

		if (method != null) {
			Annotation[][] annotations = method.getParameterAnnotations();
			if (!ArrayUtils.isEmpty(annotations)) {
				setterParameterDescriptor = new DefaultParameterDescriptor(name, annotations[0],
						method.getParameterTypes()[0], method.getGenericParameterTypes()[0]);
			}
		}

		if (setterParameterDescriptor == null) {
			this.nullable = AnnotatedElementUtils.isNullable(this, () -> false);
		} else {
			this.nullable = AnnotatedElementUtils.isNullable(setterParameterDescriptor,
					() -> AnnotatedElementUtils.isNullable(DefaultSetter.this, () -> false));
		}
	}

	public String getName() {
		return name;
	}

	public boolean isNullable() {
		return nullable;
	}

	@Override
	public Value getDefaultValue() {
		if (setterParameterDescriptor == null) {
			return super.getDefaultValue();
		}

		Value value = setterParameterDescriptor.getDefaultValue();
		return value == null ? super.getDefaultValue() : value;
	}

	public Class<?> getType() {
		if (setterParameterDescriptor != null) {
			return setterParameterDescriptor.getType();
		}

		Field field = getField();
		if (field != null) {
			return field.getType();
		}
		throw createNotSupportException();
	}

	public Type getGenericType() {
		if (setterParameterDescriptor != null) {
			return setterParameterDescriptor.getGenericType();
		}

		Field field = getField();
		if (field != null) {
			return field.getGenericType();
		}
		throw createNotSupportException();
	}
}
