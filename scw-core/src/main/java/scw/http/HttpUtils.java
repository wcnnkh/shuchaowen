package scw.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import scw.core.Assert;
import scw.core.Constants;
import scw.core.instance.InstanceUtils;
import scw.core.utils.ArrayUtils;
import scw.core.utils.CollectionUtils;
import scw.core.utils.ObjectUtils;
import scw.core.utils.StringUtils;
import scw.core.utils.TypeUtils;
import scw.core.utils.XUtils;
import scw.http.client.HttpClient;
import scw.http.server.JsonServerHttpRequest;
import scw.http.server.ServerHttpRequest;
import scw.http.server.ServerHttpResponse;
import scw.http.server.ip.ServerHttpRequestIpGetter;
import scw.io.IOUtils;
import scw.io.Resource;
import scw.json.JSONSupport;
import scw.json.JsonArray;
import scw.json.JsonElement;
import scw.json.JsonObject;
import scw.lang.NotSupportedException;
import scw.net.FileMimeTypeUitls;
import scw.net.MimeType;
import scw.net.uri.UriComponentsBuilder;
import scw.util.ToMap;
import scw.value.EmptyValue;
import scw.value.StringValue;
import scw.value.Value;

public final class HttpUtils {
	private HttpUtils() {
	};
	
	private static final HttpClient HTTP_CLIENT = InstanceUtils.loadService(HttpClient.class,
			"scw.http.client.SimpleHttpClient");
	private static final ServerHttpRequestIpGetter SERVER_HTTP_REQUEST_IP_GETTER = InstanceUtils
			.loadService(ServerHttpRequestIpGetter.class, "scw.http.server.ip.DefaultServerHttpRequestIpGetter");
	
	static {

	}

	public static HttpClient getHttpClient() {
		return HTTP_CLIENT;
	}

	public static ServerHttpRequestIpGetter getServerHttpRequestIpGetter() {
		return SERVER_HTTP_REQUEST_IP_GETTER;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String toJsonString(Object body, JSONSupport jsonSupport) {
		if (body == null) {
			return null;
		}

		if (body instanceof ToMap) {
			return jsonSupport.toJSONString(XUtils.toMap((ToMap) body));
		} else {
			return jsonSupport.toJSONString(body);
		}
	}

	@SuppressWarnings("rawtypes")
	public static String toFormString(Object body, String charsetName, JSONSupport jsonSupport)
			throws UnsupportedEncodingException {
		if (body == null) {
			return null;
		}

		if (body instanceof String || TypeUtils.isPrimitiveOrWrapper(body.getClass())) {
			return body.toString();
		} else if (body instanceof ToMap) {
			return toFormBody(((ToMap) body).toMap(), charsetName);
		} else if (body instanceof Map) {
			return toFormBody((Map) body, charsetName);
		} else {
			String json = jsonSupport.toJSONString(body);
			Map map = jsonSupport.parseObject(json, Map.class);
			return toFormBody(map, charsetName);
		}
	}

	public static String toFormBody(String key, Collection<?> values, String charsetName)
			throws UnsupportedEncodingException {
		if (StringUtils.isEmpty(key) || CollectionUtils.isEmpty(values)) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Object value : values) {
			if (value == null) {
				continue;
			}

			if (sb.length() > 0) {
				sb.append("&");
			}

			sb.append(key);
			sb.append("=");
			if (StringUtils.isEmpty(charsetName)) {
				sb.append(URLEncoder.encode(value.toString(), charsetName));
			} else {
				sb.append(value.toString());
			}
		}
		return sb.toString();
	}

	/**
	 * 将map转换为表单参数结构
	 * @param parameterMap
	 * @param charsetName
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@SuppressWarnings("rawtypes")
	public static String toFormBody(Map<?, ?> parameterMap, String charsetName) throws UnsupportedEncodingException {
		if (CollectionUtils.isEmpty(parameterMap)) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Entry<?, ?> entry : parameterMap.entrySet()) {
			Object value = entry.getValue();
			if (value == null) {
				continue;
			}

			String key = entry.getKey().toString();
			String text;
			if (value instanceof Collection) {
				text = toFormBody(key, (Collection) value, charsetName);
			} else if (value.getClass().isArray()) {
				text = toFormBody(key, ArrayUtils.toList(value), charsetName);
			} else {
				text = toFormBody(key, Arrays.asList(value), charsetName);
			}

			if (text == null) {
				continue;
			}

			if (sb.length() != 0) {
				sb.append("&");
			}

			sb.append(text);
		}
		return sb.toString();
	}

	/**
	 * 在url后面追加参数
	 * @param url
	 * @param paramMap
	 * @param charsetName
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String appendParameters(String url, Map<String, ?> paramMap, String charsetName)
			throws UnsupportedEncodingException {
		if (paramMap == null || paramMap.isEmpty()) {
			return url;
		}

		StringBuilder sb = new StringBuilder(128);
		if (!StringUtils.isEmpty(url)) {
			sb.append(url);
			if (url.lastIndexOf("?") == -1) {
				sb.append("?");
			} else {
				sb.append("&");
			}
		}

		String text = toFormBody(paramMap, charsetName);
		if (text != null) {
			sb.append(text);
		}
		return sb.toString();
	}

	public static String encode(Object value, String charsetName) {
		if (value == null) {
			return null;
		}

		try {
			return URLEncoder.encode(value.toString(), charsetName);
		} catch (UnsupportedEncodingException e) {
			throw new NotSupportedException(e);
		}
	}

	public static String encode(Object value) {
		return encode(value, Constants.DEFAULT_CHARSET_NAME);
	}

	public static String decode(String value, String charsetName) {
		if (value == null) {
			return null;
		}

		try {
			return URLDecoder.decode(value, charsetName);
		} catch (UnsupportedEncodingException e) {
			throw new NotSupportedException(e);
		}
	}

	public static String decode(String value) {
		return decode(value, Constants.DEFAULT_CHARSET_NAME);
	}

	public static String decode(String content, String charsetName, int count) throws UnsupportedEncodingException {
		if (count <= 0) {
			return content;
		}

		String newContent = content;
		for (int i = 0; i < count; i++) {
			newContent = decode(newContent, charsetName);
		}
		return newContent;
	}

	public static String encode(Object content, String charsetName, int count) throws UnsupportedEncodingException {
		if (count <= 0 || content == null) {
			return content == null ? null : content.toString();
		}

		String newContent = content.toString();
		for (int i = 0; i < count; i++) {
			newContent = encode(newContent, charsetName);
		}
		return newContent;
	}

	public static boolean isValidOrigin(HttpRequest request, Collection<String> allowedOrigins) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(allowedOrigins, "Allowed origins must not be null");

		String origin = request.getHeaders().getOrigin();
		if (origin == null || allowedOrigins.contains("*")) {
			return true;
		} else if (CollectionUtils.isEmpty(allowedOrigins)) {
			return isSameOrigin(request);
		} else {
			return allowedOrigins.contains(origin);
		}
	}

	/**
	 * 是否是同一个origin
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isSameOrigin(HttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		String origin = headers.getOrigin();
		if (origin == null) {
			return true;
		}
		
		return isSameOrigin(request.getURI(), UriComponentsBuilder.fromOriginHeader(origin).build().toUri());
	}
	
	/**
	 * 判断两个url是否同源
	 * @param url1
	 * @param url2
	 * @return
	 */
	public static boolean isSameOrigin(String url1, String url2){
		if(url1 == null || url2 == null){
			return false;
		}
		
		if(StringUtils.equals(url1, url2)){
			return true;
		}
		
		try {
			return isSameOrigin(new URI(url1), new URI(url2));
		} catch (URISyntaxException e) {
			return false;
		}
	}

	/**
	 * 判断两个uri是否同源
	 * @param uri1
	 * @param uri2
	 * @return
	 */
	public static boolean isSameOrigin(URI uri1, URI uri2) {
		if(uri1 == null || uri2 == null){
			return false;
		}
		
		if(uri1.equals(uri2)){
			return true;
		}
		
		return (ObjectUtils.nullSafeEquals(uri1.getScheme(), uri2.getScheme())
				&& ObjectUtils.nullSafeEquals(uri1.getHost(), uri2.getHost())
				&& getPort(uri1.getScheme(), uri1.getPort()) == getPort(uri2.getScheme(), uri2.getPort()));
	}

	private static int getPort(String scheme, int port) {
		if (port == -1) {
			if ("http".equals(scheme) || "ws".equals(scheme)) {
				port = 80;
			} else if ("https".equals(scheme) || "wss".equals(scheme)) {
				port = 443;
			}
		}
		return port;
	}

	/**
	 * 从cookie中获取数据
	 * 
	 * @param request
	 * 
	 * @param name
	 *            cookie中的名字
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
	 * 将文件信息写入ContentDisposition
	 * @param outputMessage
	 * @param fileName
	 */
	public static void writeFileMessageHeaders(HttpOutputMessage outputMessage, String fileName) {
		MimeType mimeType = FileMimeTypeUitls.getMimeType(fileName);
		if (mimeType != null) {
			outputMessage.setContentType(mimeType);
		}
		ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
				.filename(fileName, Constants.UTF_8).build();
		outputMessage.getHeaders().setContentDisposition(contentDisposition);
	}
	
	/**
	 * 写入一个静态资源
	 * @param request
	 * @param response
	 * @param resource
	 * @param mimeType
	 * @throws IOException
	 */
	public static void writeStaticResource(ServerHttpRequest request, ServerHttpResponse response, Resource resource, MimeType mimeType) throws IOException{
		if(!resource.exists()){
			response.sendError(HttpStatus.NOT_FOUND.value(), "The resource does not exist!");
			return ;
		}
		
		if (mimeType != null) {
			response.setContentType(mimeType);
		}
		
		long ifModifiedSince = request.getHeaders().getIfModifiedSince();
		long lastModified = resource.lastModified();
		if(lastModified <= ifModifiedSince){
			//客户端缓存未过期
			response.setStatusCode(HttpStatus.NOT_MODIFIED);
			return ;
		}
		
		if(lastModified > 0){
			response.getHeaders().setLastModified(lastModified);
		}
		IOUtils.copy(resource.getInputStream(), response.getBody());
	}
	
	/**
	 * 根据参数名获取
	 * @param request
	 * @param name
	 * @return 如果不存在返回{@see EmptyValue}
	 * @throws IOException
	 */
	public static Value getParameter(ServerHttpRequest request, String name){
		String value = request.getParameterMap().getFirst(name);
		if(value != null){
			return new StringValue(value);
		}
		
		if(request instanceof JsonServerHttpRequest){
			JsonObject jsonObject = ((JsonServerHttpRequest) request).getJsonObject();
			if(jsonObject != null){
				return jsonObject.get(name);
			}
		}
		return EmptyValue.INSTANCE;
	}
	
	/**
	 * 此方法不会返回空，如果不存在返回的数组长度为0
	 * @param request
	 * @param name
	 * @return
	 */
	public static Value[] getParameterValues(ServerHttpRequest request, String name){
		List<String> valueList = request.getParameterMap().get(name);
		if(!CollectionUtils.isEmpty(valueList)){
			Value[] values = new Value[valueList.size()];
			int index = 0;
			for(String value : valueList){
				values[index++] = new StringValue(value);
			}
			return values;
		}
		
		if(request instanceof JsonServerHttpRequest){
			JsonObject jsonObject = ((JsonServerHttpRequest) request).getJsonObject();
			if(jsonObject != null){
				JsonElement jsonElement = jsonObject.get(name);
				if(jsonElement.isJsonArray()){
					JsonArray jsonArray = jsonElement.getAsJsonArray();
					Value[] values = new Value[jsonArray.size()];
					int index = 0;
					for(JsonElement element : jsonElement.getAsJsonArray()){
						values[index++] = element;
					}
					return values;
				}
			}
		}
		return new Value[0];
	}
}
