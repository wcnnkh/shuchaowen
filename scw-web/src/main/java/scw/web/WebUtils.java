package scw.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import scw.codec.support.CharsetCodec;
import scw.core.utils.CollectionUtils;
import scw.core.utils.StringUtils;
import scw.dom.DomUtils;
import scw.http.HttpCookie;
import scw.http.HttpMethod;
import scw.http.HttpStatus;
import scw.io.IOUtils;
import scw.io.Resource;
import scw.json.JSONUtils;
import scw.json.JsonArray;
import scw.json.JsonElement;
import scw.json.JsonObject;
import scw.lang.NamedThreadLocal;
import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.net.MimeType;
import scw.net.message.multipart.MultipartMessage;
import scw.net.message.multipart.MultipartMessageResolver;
import scw.util.XUtils;
import scw.value.AnyValue;
import scw.value.EmptyValue;
import scw.value.StringValue;
import scw.value.Value;
import scw.web.support.DefaultHttpService;

public final class WebUtils {
	private static Logger logger = LoggerFactory.getLogger(WebUtils.class);
	private static ThreadLocal<ServerHttpRequest> SERVER_HTTP_REQUEST_LOCAL = new NamedThreadLocal<ServerHttpRequest>(
			WebUtils.class.getSimpleName() + "-ServerHttpRequest");
	private static final String RESTFUL_PARAMETER_MAP = "_restful_parameter_map";

	/**
	 * 缓存是否过期,如果未过期那么返回304，如果已过期则setLastModified
	 * 
	 * @param request
	 * @param response
	 * @param lastModified
	 * @return
	 */
	public static boolean isExpired(ServerHttpRequest request, ServerHttpResponse response, long lastModified) {
		response.getHeaders().setLastModified(lastModified);
		long ifModifiedSince = request.getHeaders().getIfModifiedSince();
		if (ifModifiedSince < 0 || lastModified < 0) {
			// 缓存已过期,请求中没有此值
			return true;
		}

		// 不比较毫秒
		if (ifModifiedSince / 1000 != lastModified / 1000) {
			// 缓存已过期
			return true;
		}

		// 客户端缓存未过期
		response.setStatusCode(HttpStatus.NOT_MODIFIED);
		return false;
	}

	/**
	 * 写入一个静态资源
	 * 
	 * @param request
	 * @param response
	 * @param resource
	 * @param mimeType
	 * @throws IOException
	 */
	public static void writeStaticResource(ServerHttpRequest request, ServerHttpResponse response, Resource resource,
			MimeType mimeType) throws IOException {
		if (resource == null || !resource.exists()) {
			response.sendError(HttpStatus.NOT_FOUND.value(), "The resource does not exist!");
			return;
		}

		if (mimeType != null) {
			response.setContentType(mimeType);
		}

		if (!isExpired(request, response, resource.lastModified())) {
			return;
		}
		IOUtils.copy(resource.getInputStream(), response.getOutputStream());
	}

	/**
	 * 根据参数名获取
	 * 
	 * @param request
	 * @param name
	 * @return 如果不存在返回{@see EmptyValue}
	 */
	public static Value getParameter(ServerHttpRequest request, String name) {
		String value = request.getParameterMap().getFirst(name);
		if (value == null) {
			Map<String, String> parameterMap = getRestfulParameterMap(request);
			if (parameterMap != null) {
				value = parameterMap.get(name);
			}
		}

		if (value != null) {
			value = decodeGETParameter(request, value);
			return new StringValue(value);
		}

		JsonServerHttpRequest jsonServerHttpRequest = XUtils.getDelegate(request, JsonServerHttpRequest.class);
		if (jsonServerHttpRequest != null) {
			JsonObject jsonObject = jsonServerHttpRequest.getJsonObject();
			if (jsonObject != null) {
				JsonElement element = jsonObject.getValue(name);
				if (element != null) {
					return element;
				}
			}
		}

		MultiPartServerHttpRequest multiPartServerHttpRequest = XUtils.getDelegate(request,
				MultiPartServerHttpRequest.class);
		if (multiPartServerHttpRequest != null) {
			MultipartMessage multipartMessage = multiPartServerHttpRequest.getMultipartMessageMap().getFirst(name);
			if (multipartMessage != null) {
				return new AnyValue(multipartMessage);
			}
		}
		return EmptyValue.INSTANCE;
	}

	/**
	 * 此方法不会返回空，如果不存在返回的数组长度为0
	 * 
	 * @param request
	 * @param name
	 * @return
	 */
	public static Value[] getParameterValues(ServerHttpRequest request, String name) {
		List<String> valueList = request.getParameterMap().get(name);
		if (!CollectionUtils.isEmpty(valueList)) {
			Value[] values = new Value[valueList.size()];
			int index = 0;
			for (String value : valueList) {
				values[index++] = new StringValue(decodeGETParameter(request, value));
			}
			return values;
		}

		JsonServerHttpRequest jsonServerHttpRequest = XUtils.getDelegate(request, JsonServerHttpRequest.class);
		if (jsonServerHttpRequest != null) {
			JsonObject jsonObject = jsonServerHttpRequest.getJsonObject();
			if (jsonObject != null) {
				JsonElement jsonElement = jsonObject.getValue(name);
				if (jsonElement.isJsonArray()) {
					JsonArray jsonArray = jsonElement.getAsJsonArray();
					Value[] values = new Value[jsonArray.size()];
					int index = 0;
					for (JsonElement element : jsonElement.getAsJsonArray()) {
						values[index++] = element;
					}
					return values;
				}
			}
		}

		MultiPartServerHttpRequest multiPartServerHttpRequest = XUtils.getDelegate(request,
				MultiPartServerHttpRequest.class);
		if (multiPartServerHttpRequest != null) {
			List<MultipartMessage> items = multiPartServerHttpRequest.getMultipartMessageMap().get(name);
			Value[] values = new Value[items.size()];
			int index = 0;
			for (MultipartMessage element : items) {
				values[index++] = new AnyValue(element);
			}
			return values;
		}

		return Value.EMPTY_ARRAY;
	}

	public static ServerHttpRequest wrapperServerJsonRequest(ServerHttpRequest request) {
		if (request.getMethod() == HttpMethod.GET) {
			return request;
		}

		// 如果是一个json请求，那么包装一下
		if (request.getHeaders().isJsonContentType()) {
			JsonServerHttpRequest jsonServerHttpRequest = XUtils.getDelegate(request, JsonServerHttpRequest.class);
			if (jsonServerHttpRequest != null) {
				// 返回原始对象
				return request;
			}

			return new JsonServerHttpRequest(request);
		}
		return request;
	}

	public static ServerHttpRequest wrapperServerMultipartFormRequest(ServerHttpRequest request,
			MultipartMessageResolver multipartMessageResolver) {
		if (request.getMethod() == HttpMethod.GET) {
			return request;
		}

		// 如果是 一个MultiParty请求，那么包装一下
		if (request.getHeaders().isMultipartFormContentType()) {
			MultiPartServerHttpRequest multiPartServerHttpRequest = XUtils.getDelegate(request,
					MultiPartServerHttpRequest.class);
			if (multiPartServerHttpRequest != null) {
				// 返回原始对象
				return request;
			}

			if (multipartMessageResolver == null) {
				logger.warn("Multipart is not supported: {}", request);
			} else {
				return new MultiPartServerHttpRequest(request, multipartMessageResolver);
			}
		}
		return request;
	}

	/**
	 * 从cookie中获取数据
	 * 
	 * @param request
	 * 
	 * @param name    cookie中的名字
	 * @return
	 */
	public static HttpCookie getCookie(ServerHttpRequest request, String name) {
		if (name == null) {
			return null;
		}

		HttpCookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return null;
		}

		for (HttpCookie cookie : cookies) {
			if (cookie == null) {
				continue;
			}

			if (name.equals(cookie.getName())) {
				return cookie;
			}
		}
		return null;
	}

	/**
	 * 将一个ServerhttpRequest保存在ThreadLocal中
	 * 
	 * @see DefaultHttpService#service(ServerHttpRequest, ServerHttpResponse)
	 * @param request
	 */
	public static void setLocalServerHttpRequest(ServerHttpRequest request) {
		if (request == null) {
			SERVER_HTTP_REQUEST_LOCAL.remove();
		} else {
			SERVER_HTTP_REQUEST_LOCAL.set(request);
		}
	}

	/**
	 * 获取一个存储在ThreadLocal中的ServerHttpRequest
	 * 
	 * @see #setLocalServerHttpRequest(ServerHttpRequest)
	 * @see #SERVER_HTTP_REQUEST_LOCAL
	 * @return
	 */
	public static ServerHttpRequest getLocalServerHttpRequest() {
		return SERVER_HTTP_REQUEST_LOCAL.get();
	}

	public static void setRestfulParameterMap(ServerHttpRequest request, Map<String, String> restfulMap) {
		request.setAttribute(RESTFUL_PARAMETER_MAP, restfulMap);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getRestfulParameterMap(ServerHttpRequest request) {
		return (Map<String, String>) request.getAttribute(RESTFUL_PARAMETER_MAP);
	}

	public static String decodeGETParameter(ServerHttpRequest request, String value) {
		if (StringUtils.isEmpty(value)) {
			return value;
		}

		if (request.getMethod() != HttpMethod.GET) {
			return value;
		}

		if (StringUtils.containsChinese(value)) {
			return value;
		}

		return new CharsetCodec(request.getCharacterEncoding()).decode(CharsetCodec.ISO_8859_1.encode(value));
	}

	@SuppressWarnings("unchecked")
	public static <T> Map<String, T> getParameterMap(ServerHttpRequest request, String appendValueChars) {
		Map<String, Object> parameterMap = new LinkedHashMap<String, Object>();
		for (Entry<String, List<String>> entry : request.getParameterMap().entrySet()) {
			List<String> values = entry.getValue();
			if (CollectionUtils.isEmpty(values)) {
				continue;
			}

			List<String> arrays = new ArrayList<String>(values.size());
			for (String value : values) {
				arrays.add(decodeGETParameter(request, value));
			}

			if (appendValueChars == null) {
				if (arrays.size() == 1) {
					parameterMap.put(entry.getKey(), arrays.get(0));
				} else {
					parameterMap.put(entry.getKey(), arrays);
				}
			} else {
				parameterMap.put(entry.getKey(), StringUtils.collectionToDelimitedString(arrays, appendValueChars));
			}
		}
		return (Map<String, T>) parameterMap;
	}

	public static Object getRequestBody(ServerHttpRequest request) throws IOException {
		if (request.getHeaders().isJsonContentType()) {
			return JSONUtils.getJsonSupport().parseJson(request);
		} else if (request.getHeaders().isXmlContentType()) {
			Document document = DomUtils.getDomBuilder().parse(request.getReader());
			return document;
		} else {
			return WebUtils.getParameterMap(request, null);
		}
	}
}
