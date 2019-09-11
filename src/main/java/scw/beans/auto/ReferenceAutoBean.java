package scw.beans.auto;

import scw.beans.BeanFactory;

/**
 * 引用一个
 * 
 * @author shuchaowen
 *
 */
public class ReferenceAutoBean implements AutoBean {
	private String reference;
	private BeanFactory beanFactory;

	public ReferenceAutoBean(BeanFactory beanFactory, String reference) {
		this.beanFactory = beanFactory;
		this.reference = reference;
	}

	public boolean isReference() {
		return true;
	}

	public Class<?> getTargetClass() {
		return null;
	}

	public Object create(AutoBeanConfig config) throws Exception {
		return beanFactory.getInstance(reference);
	}

	public Object create(AutoBeanConfig config, Object... params) throws Exception {
		return beanFactory.getInstance(reference, params);
	}

	public Object create(AutoBeanConfig config, Class<?>[] parameterTypes, Object... params) throws Exception {
		return beanFactory.getInstance(reference, parameterTypes, params);
	}

	public boolean isInstance() {
		return beanFactory.getBeanDefinition(reference).isInstance();
	}
}
