package scw.servlet.beans.xml;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

import javax.servlet.ServletRequest;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scw.beans.BeanFactory;
import scw.beans.BeanMethod;
import scw.beans.BeanUtils;
import scw.beans.property.ValueWiredManager;
import scw.beans.xml.XmlBeanParameter;
import scw.beans.xml.XmlBeanUtils;
import scw.core.Destroy;
import scw.core.Init;
import scw.core.PropertyFactory;
import scw.core.cglib.proxy.Enhancer;
import scw.core.exception.BeansException;
import scw.core.exception.NotFoundException;
import scw.core.reflect.FieldDefinition;
import scw.core.reflect.ReflectUtils;
import scw.core.utils.ArrayUtils;
import scw.servlet.beans.RequestBean;
import scw.servlet.beans.RequestBeanUtils;

public final class XmlRequestBean implements RequestBean {

	private final BeanFactory beanFactory;
	private final PropertyFactory propertyFactory;
	private final Class<?> type;
	private String[] names;
	private final String id;
	private final String[] filterNames;
	// 构造函数的参数
	private final XmlBeanParameter[] constructorParameters;
	private final XmlBeanParameter[] properties;
	private final BeanMethod[] initMethods;
	private final BeanMethod[] destroyMethods;
	private final boolean proxy;

	private final Constructor<?> constructor;
	private final Class<?>[] constructorParameterTypes;
	private XmlBeanParameter[] beanMethodParameters;
	private Enhancer enhancer;
	private final FieldDefinition[] autoWriteFields;
	private final ValueWiredManager valueWiredManager;

	public XmlRequestBean(ValueWiredManager valueWiredManager, BeanFactory beanFactory, PropertyFactory propertyFactory,
			Node beanNode, String[] filterNames) throws Exception {
		this.valueWiredManager = valueWiredManager;
		this.beanFactory = beanFactory;
		this.propertyFactory = propertyFactory;
		this.type = XmlBeanUtils.getClass(beanNode);
		this.id = XmlBeanUtils.getId(beanNode);
		this.names = XmlBeanUtils.getNames(beanNode);
		this.filterNames = XmlBeanUtils.getFilters(beanNode, filterNames);
		// constructor
		NodeList nodeList = beanNode.getChildNodes();
		this.initMethods = XmlBeanUtils.getInitMethodList(type, nodeList);
		this.destroyMethods = XmlBeanUtils.getDestroyMethodList(type, nodeList);
		this.constructorParameters = XmlBeanUtils.getConstructorParameters(nodeList);
		this.properties = XmlBeanUtils.getBeanProperties(nodeList);
		this.proxy = BeanUtils.checkProxy(type, this.filterNames);
		this.constructor = getConstructor();
		if (constructor == null) {
			throw new NotFoundException(type.getName());
		}
		this.constructorParameterTypes = constructor.getParameterTypes();
		this.autoWriteFields = BeanUtils.getAutowriteFieldDefinitionList(type, false).toArray(new FieldDefinition[0]);
	}

	private Constructor<?> getConstructor() {
		if (ArrayUtils.isEmpty(constructorParameters)) {
			return ReflectUtils.getConstructor(type, false);
		} else {
			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				XmlBeanParameter[] beanMethodParameters = BeanUtils.sortParameters(constructor, constructorParameters);
				if (beanMethodParameters != null) {
					this.beanMethodParameters = beanMethodParameters;
					constructor.setAccessible(true);
					return constructor;
				}
			}
		}
		return null;
	}

	public String getId() {
		return this.id;
	}

	public Class<?> getType() {
		return this.type;
	}

	private Enhancer getProxyEnhancer() {
		if (enhancer == null) {
			enhancer = BeanUtils.createEnhancer(type, beanFactory, filterNames);
		}
		return enhancer;
	}

	private Object createProxyInstance(ServletRequest request) throws Exception {
		Enhancer enhancer = getProxyEnhancer();
		if (ArrayUtils.isEmpty(constructorParameters)) {
			return enhancer.create();
		} else {
			Object[] args = RequestBeanUtils.getBeanMethodParameterArgs(request, constructorParameterTypes,
					beanMethodParameters, beanFactory, propertyFactory);
			return enhancer.create(constructorParameterTypes, args);
		}
	}

	private void setProperties(Object bean) throws Exception {
		if (ArrayUtils.isEmpty(properties)) {
			return;
		}

		for (XmlBeanParameter beanProperties : properties) {
			Field field = ReflectUtils.getField(type, beanProperties.getName(), true);
			if (field == null) {
				continue;
			}

			ReflectUtils.setFieldValue(type, field, bean,
					beanProperties.parseValue(beanFactory, propertyFactory, field.getGenericType()));
		}
	}

	private Object createInstance(ServletRequest request) throws Exception {
		if (ArrayUtils.isEmpty(constructorParameterTypes)) {
			return constructor.newInstance();
		} else {
			Object[] args = RequestBeanUtils.getBeanMethodParameterArgs(request, constructorParameterTypes,
					beanMethodParameters, beanFactory, propertyFactory);
			return constructor.newInstance(args);
		}
	}

	public void autowrite(Object bean) throws Exception {
		BeanUtils.autoWrite(valueWiredManager, beanFactory, propertyFactory, type, bean,
				Arrays.asList(autoWriteFields));
		setProperties(bean);
	}

	public void init(Object bean) throws Exception {
		if (!ArrayUtils.isEmpty(initMethods)) {
			for (BeanMethod method : initMethods) {
				method.invoke(bean, beanFactory, propertyFactory);
			}
		}

		if (bean instanceof Init) {
			((Init) bean).init();
		}
	}

	public void destroy(Object bean) throws Exception {
		valueWiredManager.cancel(bean);
		if (!ArrayUtils.isEmpty(destroyMethods)) {
			for (BeanMethod method : destroyMethods) {
				method.invoke(bean, beanFactory, propertyFactory);
			}
		}

		if (bean instanceof Destroy) {
			((Destroy) bean).destroy();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T newInstance(ServletRequest request) {
		Object bean;
		try {
			if (proxy) {
				bean = createProxyInstance(request);
			} else {
				bean = createInstance(request);
			}

			return (T) bean;
		} catch (Exception e) {
			throw new BeansException(type.getName(), e);
		}
	}

	public String[] getNames() {
		return names;
	}
}
