package scw.servlet.beans.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scw.beans.BeanFactory;
import scw.beans.property.ValueWiredManager;
import scw.beans.xml.XmlBeanUtils;
import scw.core.PropertyFactory;
import scw.core.exception.AlreadyExistsException;
import scw.core.utils.StringUtils;
import scw.servlet.beans.RequestBean;
import scw.servlet.beans.RequestBeanFactory;

public final class XmlRequestBeanFactory implements RequestBeanFactory {
	private final Map<String, XmlRequestBean> beanMap = new HashMap<String, XmlRequestBean>();
	private final Map<String, String> nameMapping = new HashMap<String, String>();

	private static final String BEAN_TAG_NAME = "request:bean";

	public XmlRequestBeanFactory(ValueWiredManager valueWiredManager, BeanFactory beanFactory,
			PropertyFactory propertyFactory, String configXml,
			String[] filterNames) throws Exception {
		if (!StringUtils.isNull(configXml)) {
			NodeList nhosts = XmlBeanUtils.getRootNodeList(configXml);
			for (int i = 0; i < nhosts.getLength(); i++) {
				Node nRoot = nhosts.item(i);
				if (BEAN_TAG_NAME.equalsIgnoreCase(nRoot.getNodeName())) {
					XmlRequestBean bean = new XmlRequestBean(valueWiredManager, beanFactory,
							propertyFactory, nRoot, filterNames);
					if (beanMap.containsKey(bean.getId())) {
						throw new AlreadyExistsException(bean.getId());
					}

					beanMap.put(bean.getId(), bean);
					if (bean.getNames() != null) {
						for (String n : bean.getNames()) {
							if (nameMapping.containsKey(n)) {
								throw new AlreadyExistsException(n);
							}

							nameMapping.put(n, bean.getId());
						}
					}
				}
			}
		}
	}

	public RequestBean get(String name) {
		RequestBean requestBean = beanMap.get(name);
		if (requestBean == null) {
			String id = nameMapping.get(name);
			if (id != null) {
				requestBean = beanMap.get(id);
			}
		}
		return requestBean;
	}

	public boolean contains(String name) {
		return nameMapping.containsKey(name) || beanMap.containsKey(name);
	}
}
