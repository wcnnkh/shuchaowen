package scw.beans.config;

import scw.beans.BeanFactory;
import scw.mapper.FieldContext;
import scw.util.value.property.PropertyFactory;

/**
 * 解析配置文件
 * 
 * @author shuchaowen
 *
 */
public interface ConfigParse {
	Object parse(BeanFactory beanFactory, PropertyFactory propertyFactory, FieldContext fieldContext, String filePath, String charset) throws Exception;
}
