package scw.beans;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Node;

import scw.beans.property.PropertiesFactory;
import scw.beans.rpc.dubbo.XmlDubboBeanFactory;
import scw.beans.rpc.http.HttpRPCBeanFactory;
import scw.beans.xml.XmlBeanFactory;
import scw.beans.xml.XmlBeanUtils;
import scw.common.exception.AlreadyExistsException;
import scw.common.exception.BeansException;
import scw.common.exception.ShuChaoWenRuntimeException;
import scw.common.utils.ClassUtils;
import scw.common.utils.StringUtils;

public final class CommonBeanFactory implements BeanFactory {
	private volatile Map<String, Object> singletonMap = new HashMap<String, Object>();
	private volatile Map<String, Bean> beanMap = new HashMap<String, Bean>();
	private volatile Map<String, String> nameMappingMap = new HashMap<String, String>();
	private final PropertiesFactory propertiesFactory;
	private String[] filterNames;
	private String packages;
	private final boolean initStatic;// 是否初始化静态方法

	public CommonBeanFactory(PropertiesFactory propertiesFactory, String[] filterNames, boolean initStatic) {
		this.propertiesFactory = propertiesFactory;
		this.filterNames = filterNames;
		this.initStatic = initStatic;
	}

	public CommonBeanFactory(PropertiesFactory propertiesFactory, String xmlPath, boolean initStatic) throws Exception {
		this.initStatic = initStatic;
		this.propertiesFactory = propertiesFactory;
		if (!StringUtils.isNull(xmlPath)) {
			Node root = XmlBeanUtils.getRootNode(xmlPath);
			this.packages = XmlBeanUtils.getNodeAttributeValue(propertiesFactory, root, "packages");
			this.filterNames = StringUtils
					.commonSplit(XmlBeanUtils.getNodeAttributeValue(propertiesFactory, root, "filters"));

			addBeanConfigFactory(new XmlDubboBeanFactory(propertiesFactory, xmlPath));
			addBeanConfigFactory(new HttpRPCBeanFactory(propertiesFactory, xmlPath));
			addBeanConfigFactory(new XmlBeanFactory(this, propertiesFactory, xmlPath, filterNames));
			addBeanConfigFactory(new AnnotationBeanFactory(this, propertiesFactory, packages, filterNames));
		}
	}

	public void registerNameMapping(String key, String value) {
		if (nameMappingMap.containsKey(key)) {
			throw new AlreadyExistsException(key);
		}

		synchronized (nameMappingMap) {
			nameMappingMap.put(key, value);
		}
	}

	public void addSingleton(String id, Object singleton) {
		Bean bean = getBean(id);
		if (bean == null) {
			throw new scw.common.exception.NotFoundException(id);
		}

		synchronized (singletonMap) {
			singletonMap.put(id, singleton);
			try {
				bean.autowrite(singleton);
				bean.init(singleton);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void addBeanConfigFactory(BeanConfigFactory beanConfigFactory) {
		if (beanConfigFactory != null) {
			Map<String, Bean> map = beanConfigFactory.getBeanMap();
			if (map != null) {
				synchronized (beanMap) {
					for (Entry<String, Bean> entry : map.entrySet()) {
						String key = entry.getKey();
						if (beanMap.containsKey(key)) {
							throw new AlreadyExistsException(key);
						}

						beanMap.put(key, entry.getValue());
					}
				}
			}

			Map<String, String> nameMapping = beanConfigFactory.getNameMappingMap();
			if (nameMapping != null) {
				synchronized (nameMappingMap) {
					for (Entry<String, String> entry : nameMapping.entrySet()) {
						String key = entry.getKey();
						if (nameMappingMap.containsKey(key)) {
							throw new AlreadyExistsException(key);
						}
						nameMappingMap.put(key, entry.getValue());
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		Object obj = singletonMap.get(name);
		if (obj != null) {
			return (T) obj;
		}

		Bean bean = getBean(name);
		if (bean == null) {
			return null;
		}

		if (bean.isSingleton()) {
			obj = singletonMap.get(bean.getId());
			if (obj == null) {
				synchronized (singletonMap) {
					obj = singletonMap.get(bean.getId());
					if (obj == null) {
						obj = bean.newInstance();
						singletonMap.put(bean.getId(), obj);
						try {
							bean.autowrite(obj);
							bean.init(obj);
						} catch (Exception e) {
							throw new BeansException(e);
						}
					}
				}
			}
			return (T) obj;
		} else {
			obj = bean.newInstance();
			try {
				bean.autowrite(obj);
				bean.init(obj);
			} catch (Exception e) {
				throw new BeansException(e);
			}
			return (T) obj;
		}
	}

	public <T> T get(Class<T> type) {
		return get(type.getName());
	}

	public Bean getBean(String name) {
		Bean bean = getBeanCache(name);
		if (bean == null) {
			synchronized (beanMap) {
				bean = getBeanCache(name);
				if (bean == null) {
					try {
						bean = newBean(name);
						if (bean != null) {
							beanMap.put(bean.getId(), bean);
							addBeanNameMapping(bean);
						}
					} catch (Exception e) {
						throw new BeansException(e);
					}
				}
			}
		}
		return bean;
	}

	private void addBeanNameMapping(Bean bean) {
		if (bean.getNames() != null) {
			synchronized (bean) {
				for (String n : bean.getNames()) {
					nameMappingMap.put(n, bean.getId());
				}
			}
		}
	}

	public boolean contains(String name) {
		boolean b = singletonMap.containsKey(name) || nameMappingMap.containsKey(name) || beanMap.containsKey(name);
		if (b) {
			return b;
		}

		try {
			Class<?> clz = Class.forName(name);
			if (ClassUtils.isInstance(clz)) {
				b = true;
			}
		} catch (ClassNotFoundException e) {
		}
		return false;
	}

	private Bean getBeanCache(String name) {
		Bean bean = beanMap.get(name);
		if (bean == null) {
			String v = nameMappingMap.get(name);
			if (v != null) {
				bean = beanMap.get(v);
			}
		}
		return bean;
	}

	private Bean newBean(String name) {
		try {
			String n = nameMappingMap.get(name);
			if (n == null) {
				n = name;
			}
			Class<?> clz = Class.forName(n);
			if (!ClassUtils.isInstance(clz)) {
				return null;
			}
			return new AnnotationBean(this, propertiesFactory, clz, filterNames);
		} catch (Exception e) {
		}
		return null;
	}

	public PropertiesFactory getPropertiesFactory() {
		return propertiesFactory;
	}

	public String[] getFilterNames() {
		return filterNames;
	}

	public String getPackages() {
		return packages;
	}

	public Collection<Class<?>> getClassList() {
		return ClassUtils.getClasses(packages);
	}

	public boolean isInitStatic() {
		return initStatic;
	}

	public void init() {
		try {
			if (initStatic) {
				BeanUtils.initStatic(this, propertiesFactory, getClassList());
			}
		} catch (Exception e) {
			throw new ShuChaoWenRuntimeException(e);
		}
	}

	public void destroy() {
		for (Entry<String, Object> entry : singletonMap.entrySet()) {
			Bean bean = getBean(entry.getKey());
			try {
				bean.destroy(entry.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (initStatic) {
			try {
				BeanUtils.destroyStaticMethod(getClassList());
			} catch (Exception e) {
				throw new ShuChaoWenRuntimeException(e);
			}
		}
	}
}
