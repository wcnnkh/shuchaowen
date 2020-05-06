package scw.mq.support;

import org.w3c.dom.Node;

import scw.beans.BeanFactory;
import scw.mapper.FieldContext;
import scw.util.value.property.PropertyFactory;
import scw.xml.XmlAttributeMapper;

public class XmlAmqpConfigMapper extends XmlAttributeMapper{
	private BeanFactory beanFactory;
	
	public XmlAmqpConfigMapper(BeanFactory beanFactory, PropertyFactory propertyFactory, Node node) {
		super(propertyFactory, node);
		this.beanFactory = beanFactory;
	}

	@Override
	protected Object getNodeValue(String name, String value, Class<?> type, FieldContext fieldContext, Node node) {
		if (name.equals("exchange")) {
			return beanFactory.getInstance(value);
		}
		return super.getNodeValue(name, value, type, fieldContext, node);
	}
}
