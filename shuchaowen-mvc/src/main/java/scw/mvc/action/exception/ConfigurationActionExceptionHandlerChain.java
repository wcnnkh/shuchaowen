package scw.mvc.action.exception;

import java.util.LinkedList;
import java.util.List;

import scw.beans.BeanFactory;
import scw.beans.BeanUtils;
import scw.util.value.property.PropertyFactory;


public final class ConfigurationActionExceptionHandlerChain extends DefaultActionExceptionHandlerChain{

	public ConfigurationActionExceptionHandlerChain(BeanFactory beanFactory, PropertyFactory propertyFactory) {
		super(getActionExceptionHandlers(beanFactory, propertyFactory), null);
	}

	private static List<ActionExceptionHandler> getActionExceptionHandlers(BeanFactory beanFactory, PropertyFactory propertyFactory){
		List<ActionExceptionHandler> handlers = new LinkedList<ActionExceptionHandler>();
		handlers.addAll(BeanUtils.getConfigurationList(ActionExceptionHandler.class, beanFactory, propertyFactory));
		
		if (beanFactory.isInstance(DefaultActionExceptionHandler.class)) {
			handlers.add(beanFactory
					.getInstance(DefaultActionExceptionHandler.class));
		}
		return handlers;
	}
}