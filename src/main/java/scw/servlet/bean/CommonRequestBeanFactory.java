package scw.servlet.bean;

import scw.beans.BeanFactory;
import scw.beans.property.PropertiesFactory;
import scw.servlet.bean.xml.XmlRequestBeanFactory;

public final class CommonRequestBeanFactory implements RequestBeanFactory {
	private final XmlRequestBeanFactory xmlRequestBeanFactory;
	private final AnnotationRequestBeanFactory annotationRequestBeanFactory;

	public CommonRequestBeanFactory(BeanFactory beanFactory, PropertiesFactory propertiesFactory, String configXml) throws Exception {
		this.xmlRequestBeanFactory = new XmlRequestBeanFactory(beanFactory, propertiesFactory, configXml);
		this.annotationRequestBeanFactory = new AnnotationRequestBeanFactory(beanFactory, propertiesFactory);
	}

	public RequestBean get(String name) {
		RequestBean requestBean = xmlRequestBeanFactory.get(name);
		if (requestBean == null) {
			requestBean = annotationRequestBeanFactory.get(name);
		}
		return requestBean;
	}

	public boolean contains(String name) {
		return xmlRequestBeanFactory.contains(name) || annotationRequestBeanFactory.contains(name);
	}
}
