package scw.util;

public interface ServiceLoaderFactory {
	<S> ServiceLoader<S> getServiceLoader(Class<S> service, ClassLoader loader);

	<S> ServiceLoader<S> getServiceLoader(Class<S> service);
}
