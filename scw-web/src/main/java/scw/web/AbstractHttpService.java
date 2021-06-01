package scw.web;

import java.io.IOException;

import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.web.cors.Cors;
import scw.web.cors.CorsRegistry;
import scw.web.cors.CorsUtils;

public abstract class AbstractHttpService implements HttpService {
	private static Logger logger = LoggerFactory.getLogger(HttpService.class);
	private final HttpServiceRegistry serviceRegistry = new HttpServiceRegistry();
	private CorsRegistry corsRegistry;

	public void service(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		CorsRegistry corsRegistry = getCorsRegistry();
		if (corsRegistry != null) {
			if (CorsUtils.isCorsRequest(request)) {
				Cors cors = corsRegistry.get(request);
				if (cors != null) {
					cors.write(request, response.getHeaders());
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(request.toString());
		}

		WebUtils.setLocalServerHttpRequest(request);
		Iterable<? extends HttpServiceInterceptor> interceptors = getHttpServiceInterceptors();
		HttpService service = serviceRegistry.get(request);
		HttpService serviceToUse = new HttpServiceInterceptorChain(
				interceptors == null ? null : interceptors.iterator(), service);
		try {
			serviceToUse.service(request, response);
		} finally {
			try {
				if (!response.isCommitted()) {
					if (request.isSupportAsyncControl()) {
						ServerHttpAsyncControl serverHttpAsyncControl = request.getAsyncControl(response);
						if (serverHttpAsyncControl.isStarted()) {
							serverHttpAsyncControl.addListener(new ServerHttpResponseCompleteAsyncListener(response));
							return;
						}
					}
				}
				response.close();
			} catch (Exception e) {
				WebUtils.setLocalServerHttpRequest(null);
			}
		}
	}

	public HttpServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public CorsRegistry getCorsRegistry() {
		return corsRegistry;
	}

	public void setCorsRegistry(CorsRegistry corsRegistry) {
		this.corsRegistry = corsRegistry;
	}

	public abstract Iterable<? extends HttpServiceInterceptor> getHttpServiceInterceptors();
}