package scw.mvc.http.cors;

import scw.beans.BeanFactory;
import scw.beans.annotation.Configuration;
import scw.core.PropertyFactory;
import scw.mvc.MVCUtils;
import scw.mvc.handler.HandlerChain;
import scw.mvc.handler.HttpHandler;
import scw.mvc.http.HttpChannel;

@Configuration(order=CorsHandler.ORDER)
public final class CorsHandler extends HttpHandler{
	public static final int ORDER = 1000;
	
	private final CorsConfigFactory crossDomainDefinitionFactory;
	
	public CorsHandler(BeanFactory beanFactory, PropertyFactory propertyFactory){
		this(MVCUtils.getCorsConfigFactory(beanFactory, propertyFactory));
	}
	
	public CorsHandler(CorsConfigFactory crossDomainDefinitionFactory){
		this.crossDomainDefinitionFactory = crossDomainDefinitionFactory;
	}
	
	@Override
	protected void doHttpHandler(HttpChannel channel, HandlerChain chain)
			throws Throwable {
		if(crossDomainDefinitionFactory != null){
			CorsConfig corsConfig = crossDomainDefinitionFactory
					.getCorsConfig(channel);
			if (corsConfig != null) {
				MVCUtils.responseCrossDomain(corsConfig, channel.getResponse());
			}
		}
		chain.doHandler(channel);
	}
}
