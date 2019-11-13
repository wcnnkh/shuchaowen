package scw.beans;

import scw.core.aop.ProxyUtils;

public final class EmptyBeanDefinition implements BeanDefinition {
	private Object instance;
	private Class<?> type;
	private String[] names;

	public EmptyBeanDefinition(Class<?> type, Object instance, String[] names) {
		this.type = type;
		this.instance = instance;
		this.names = names;
	}

	public boolean isInstance() {
		return true;
	}

	@SuppressWarnings("unchecked")
	public <T> T create() {
		return (T) instance;
	}

	public <T> T create(Object... params) {
		return create();
	}

	public <T> T create(Class<?>[] parameterTypes, Object... params) {
		return create();
	}

	public String getId() {
		return type.getName();
	}

	public String[] getNames() {
		return names == null ? new String[0] : names;
	}

	public Class<?> getType() {
		return type;
	}

	public boolean isProxy() {
		return ProxyUtils.isProxy(instance);
	}

	public boolean isSingleton() {
		return true;
	}

	public void autowrite(Object bean) throws Exception {
	}

	public void init(Object bean) throws Exception {
	}

	public void destroy(Object bean) throws Exception {
	}

}
