package scw.http.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import scw.beans.BeanFactory;
import scw.core.instance.InstanceUtils;
import scw.http.server.cors.CorsFilter;
import scw.http.server.resource.DefaultStaticResourceLoader;
import scw.http.server.resource.StaticResourceHttpServerHandler;
import scw.http.server.resource.StaticResourceLoader;
import scw.value.property.PropertyFactory;

public class DefaultHttpService implements HttpService {
	private final HttpServiceHandlerAccessor handlerAccessor = new HttpServiceHandlerAccessor();
	private List<HttpServiceFilter> filters = new ArrayList<HttpServiceFilter>();

	public DefaultHttpService() {
	}

	public DefaultHttpService(BeanFactory beanFactory, PropertyFactory propertyFactory) {
		StaticResourceLoader staticResourceLoader = beanFactory.isInstance(StaticResourceLoader.class)
				? beanFactory.getInstance(StaticResourceLoader.class)
				: new DefaultStaticResourceLoader(propertyFactory);
		StaticResourceHttpServerHandler resourceHandler = new StaticResourceHttpServerHandler(staticResourceLoader);
		handlerAccessor.bind(resourceHandler);
		filters.add(new CorsFilter(beanFactory, propertyFactory));
		filters.addAll(InstanceUtils.getConfigurationList(HttpServiceFilter.class, beanFactory, propertyFactory));
		filters = Arrays.asList(filters.toArray(new HttpServiceFilter[0]));
		handlerAccessor
				.bind(InstanceUtils.getConfigurationList(HttpServiceHandler.class, beanFactory, propertyFactory));
	}

	public final HttpServiceHandlerAccessor getHandlerAccessor() {
		return handlerAccessor;
	}

	public void service(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		FiltersHttpService service = new FiltersHttpService();
		try {
			service.service(request, response);
		} finally {
			if (!response.isCommitted()) {
				if (request.isSupportAsyncControl()) {
					ServerHttpAsyncControl serverHttpAsyncControl = request.getAsyncControl(response);
					if (serverHttpAsyncControl.isStarted()) {
						serverHttpAsyncControl.addListener(new ServerHttpResponseAsyncFlushListener(response));
						return;
					}
				}

				response.flush();
			}
		}
	}

	protected void doHandle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		HttpServiceHandler handler = handlerAccessor.get(request);
		if (handler != null) {
			handler.doHandle(request, response);
		}
	}

	public List<HttpServiceFilter> getFilters() {
		return filters;
	}

	private final class FiltersHttpService implements HttpService {
		private Iterator<HttpServiceFilter> iterator = filters.iterator();

		public void service(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
			if (iterator.hasNext()) {
				iterator.next().doFilter(request, response, FiltersHttpService.this);
			} else {
				doHandle(request, response);
			}
		}
	}
}
