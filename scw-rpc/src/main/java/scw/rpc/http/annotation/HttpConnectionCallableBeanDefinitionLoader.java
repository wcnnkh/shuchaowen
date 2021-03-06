package scw.rpc.http.annotation;

import javax.ws.rs.Path;

import scw.beans.BeanDefinition;
import scw.beans.BeanDefinitionLoader;
import scw.beans.BeanDefinitionLoaderChain;
import scw.beans.ConfigurableBeanFactory;
import scw.context.annotation.Provider;
import scw.core.Ordered;
import scw.http.client.DefaultHttpClient;
import scw.http.client.HttpConnectionFactory;
import scw.rpc.CallableFactory;
import scw.rpc.support.RemoteCallableBeanDefinition;

@Provider(order = Ordered.LOWEST_PRECEDENCE)
public class HttpConnectionCallableBeanDefinitionLoader implements
		BeanDefinitionLoader {

	public BeanDefinition load(ConfigurableBeanFactory beanFactory, Class<?> sourceClass,
			BeanDefinitionLoaderChain loaderChain) {
		HttpRemote remote = sourceClass.getAnnotation(HttpRemote.class);
		Path path = sourceClass.getAnnotation(Path.class);
		if (remote == null && path == null) {
			return loaderChain.load(beanFactory, sourceClass);
		}
		
		HttpConnectionFactory httpConnectionFactory = new DefaultHttpClient(beanFactory.getEnvironment().getConversionService(), beanFactory);
		CallableFactory callableFactory;
		if(remote != null){
			callableFactory = new AnnotationHttpCallableFactory(
					httpConnectionFactory, remote);
		}else{
			callableFactory = new AnnotationHttpCallableFactory(httpConnectionFactory, path);
		}
		return new RemoteCallableBeanDefinition(beanFactory, callableFactory,
				sourceClass);
	}

}
