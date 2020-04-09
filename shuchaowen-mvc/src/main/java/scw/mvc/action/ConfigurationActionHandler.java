package scw.mvc.action;

import scw.beans.BeanFactory;
import scw.beans.BeanUtils;
import scw.beans.annotation.Configuration;
import scw.mvc.action.exception.ActionExceptionHandlerChain;
import scw.mvc.action.filter.Filter;
import scw.mvc.output.Output;
import scw.util.value.property.PropertyFactory;

@Configuration(order = ConfigurationActionHandler.ORDER)
public final class ConfigurationActionHandler extends ActionHandler {
	public static final int ORDER = -1000;
	
	public ConfigurationActionHandler(BeanFactory beanFactory, PropertyFactory propertyFactory) {
		super(beanFactory.getInstance(ActionFactory.class), 
				BeanUtils.getConfigurationList(Filter.class, beanFactory, propertyFactory), beanFactory.getInstance(Output.class), beanFactory.getInstance(ActionExceptionHandlerChain.class));
	}
}