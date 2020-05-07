package scw.mapper;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import scw.core.instance.InstanceUtils;
import scw.core.instance.NoArgsInstanceFactory;
import scw.core.reflect.ReflectionUtils;
import scw.lang.NestedRuntimeException;

@SuppressWarnings("unchecked")
public class Copy {
	static final Method CLONE_METOHD = ReflectionUtils.getMethod(Object.class, "clone");
	private NoArgsInstanceFactory instanceFactory = InstanceUtils.INSTANCE_FACTORY;
	private FieldFactory fieldFactory = MapperUtils.getFieldFactory();
	/**
	 * 如果对象实现了java.lang.Cloneable 接口，是否反射调用clone方法
	 */
	private boolean invokeCloneableMethod = true;
	/**
	 * 默认不克隆transient修辞符字段
	 */
	private boolean cloneTransientField = false;

	/**
	 * 是否使用clone方式复制
	 */
	private boolean clone = false;

	public final boolean isClone() {
		return clone;
	}

	public void setClone(boolean clone) {
		this.clone = clone;
	}

	public final FieldFactory getFieldFactory() {
		return fieldFactory;
	}

	public void setFieldFactory(FieldFactory fieldFactory) {
		this.fieldFactory = fieldFactory;
	}

	public final boolean isInvokeCloneableMethod() {
		return invokeCloneableMethod;
	}

	public void setInvokeCloneableMethod(boolean invokeCloneableMethod) {
		this.invokeCloneableMethod = invokeCloneableMethod;
	}

	public final NoArgsInstanceFactory getInstanceFactory() {
		return instanceFactory;
	}

	public void setInstanceFactory(NoArgsInstanceFactory instanceFactory) {
		this.instanceFactory = instanceFactory;
	}

	public boolean isCloneTransientField() {
		return cloneTransientField;
	}

	public void setCloneTransientField(boolean cloneTransientField) {
		this.cloneTransientField = cloneTransientField;
	}

	protected Object cloneArray(Class<?> sourceClass, Object array, FieldContext parentContext,
			FieldContextFilter filter, FilterFeature... fieldFilterTypes) throws Exception {
		int size = Array.getLength(array);
		Object newArr = Array.newInstance(sourceClass.getComponentType(), size);
		for (int i = 0; i < size; i++) {
			Array.set(newArr, i, copy(Array.get(array, i), parentContext, filter, fieldFilterTypes));
		}
		return newArr;
	}

	protected FieldContext getSourceField(Class<?> sourceClass, final FieldContext targetFieldContext) {
		if (!targetFieldContext.getField().isSupportGetter()) {
			return null;
		}

		return fieldFactory.getFieldContext(sourceClass, null, new FieldContextFilter() {

			public boolean accept(FieldContext fieldContext) {
				if (!fieldContext.getField().isSupportSetter()) {
					return false;
				}

				if (targetFieldContext.getField().getGetter().getGenericType() != fieldContext.getField().getSetter()
						.getGenericType()) {
					return false;
				}

				if (!targetFieldContext.getField().getGetter().getName()
						.equals(fieldContext.getField().getSetter().getName())) {
					return false;
				}

				if (Modifier.isStatic(targetFieldContext.getField().getGetter().getModifiers())) {
					return Modifier.isStatic(fieldContext.getField().getSetter().getModifiers());
				} else {
					return !Modifier.isStatic(fieldContext.getField().getSetter().getModifiers());
				}
			}
		});
	}

	public <T, S> void copy(Class<? extends T> targetClass, T target, Class<? extends S> sourceClass, S source,
			FieldContext parentContext, FieldContextFilter filter, FilterFeature... fieldFilterTypes) throws Exception {
		for (FieldContext fieldContext : fieldFactory.getFieldContexts(targetClass, parentContext, filter,
				fieldFilterTypes)) {
			if (!fieldContext.getField().isSupportSetter()) {
				continue;
			}

			FieldContext sourceField = getSourceField(sourceClass, fieldContext);
			if (sourceField == null) {
				continue;
			}

			if (!sourceField.getField().isSupportGetter()) {
				continue;
			}

			Object value = sourceField.getField().getGetter().get(source);
			if (value == null) {
				continue;
			}

			if (isClone()) {
				if (Modifier.isTransient(fieldContext.getField().getSetter().getModifiers())) {
					if (isCloneTransientField()) {
						value = copy(value, fieldContext, filter, fieldFilterTypes);
					}
				} else {
					value = copy(value, fieldContext, filter, fieldFilterTypes);
				}
			}

			fieldContext.getField().getSetter().set(target, value);
		}
	}

	public <T, S> T copy(Class<? extends T> targetClass, Class<? extends S> sourceClass, S source,
			FieldContext parentContext, FieldContextFilter filter, FilterFeature... fieldFilterTypes) throws Exception {
		if (!getInstanceFactory().isInstance(targetClass)) {
			return (T) source;
		}

		T target = getInstanceFactory().getInstance(targetClass);
		if (getInstanceFactory().isSingleton(targetClass)) {
			return target;
		}

		copy(targetClass, target, sourceClass, source, parentContext, filter, fieldFilterTypes);
		return target;
	}

	public <T> T copy(T source, FieldContext parentContext, FieldContextFilter filter,
			FilterFeature... fieldFilterTypes) throws Exception {
		if (source == null) {
			return null;
		}

		if (source instanceof scw.mapper.Cloneable) {
			return (T) ((scw.mapper.Cloneable) source).clone();
		}

		Class<T> sourceClass = (Class<T>) source.getClass();
		if (getInstanceFactory().isSingleton(sourceClass)) {
			return source;
		}

		if (sourceClass.isPrimitive() || sourceClass.isEnum()) {
			return source;
		} else if (sourceClass.isArray()) {
			return (T) cloneArray(sourceClass, source, parentContext, filter, fieldFilterTypes);
		} else if (isInvokeCloneableMethod() && source instanceof Cloneable) {
			return (T) CLONE_METOHD.invoke(source);
		}
		return copy(sourceClass, sourceClass, source, parentContext, filter, fieldFilterTypes);
	}

	private static final Copy DEFAULT_COPY = new Copy();
	private static final Copy CLONE_COPY = new Copy();
	private static final Copy INVOKER_SETTER_COPY = new Copy();

	static {
		CLONE_COPY.setClone(true);
	}

	public static <T> T clone(T source, FieldContextFilter filter, FilterFeature... fieldFilterTypes) {
		try {
			return CLONE_COPY.copy(source, null, filter, fieldFilterTypes);
		} catch (Exception e) {
			throw new NestedRuntimeException("clone error", e);
		}
	}

	public static <T> T clone(T source) {
		return clone(source, null, FilterFeature.GETTER_IGNORE_STATIC, FilterFeature.SETTER_IGNORE_STATIC);
	}

	public static <T> T copy(Class<? extends T> targetClass, Object source, FieldContextFilter filter,
			FilterFeature... fieldFilterTypes) {
		try {
			return DEFAULT_COPY.copy(targetClass, source.getClass(), source, null, filter, fieldFilterTypes);
		} catch (Exception e) {
			throw new NestedRuntimeException("copy error", e);
		}
	}

	public static <T> T copy(Class<? extends T> targetClass, Object source) {
		return copy(targetClass, source, null, FilterFeature.GETTER_IGNORE_STATIC, FilterFeature.SETTER_IGNORE_STATIC);
	}

	public static void copy(Object target, Object source, FieldContextFilter filter,
			FilterFeature... fieldFilterTypes) {
		try {
			INVOKER_SETTER_COPY.copy(target.getClass(), target, source.getClass(), source, null, filter,
					fieldFilterTypes);
		} catch (Exception e) {
			throw new NestedRuntimeException("copy error", e);
		}
	}

	/**
	 * 推荐使用此方法
	 * 
	 * @param target
	 * @param source
	 */
	public static void copy(Object target, Object source) {
		copy(target, source, null, FilterFeature.GETTER_IGNORE_STATIC, FilterFeature.SETTER_IGNORE_STATIC);
	}
}