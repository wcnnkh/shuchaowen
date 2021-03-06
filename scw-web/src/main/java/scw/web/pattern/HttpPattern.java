package scw.web.pattern;

import java.util.Collections;
import java.util.Map;

import scw.core.utils.ObjectUtils;
import scw.core.utils.StringUtils;
import scw.lang.Nullable;
import scw.net.MimeTypes;
import scw.util.AntPathMatcher;
import scw.util.PathMatcher;
import scw.web.ServerHttpRequest;
import scw.web.WebUtils;

public class HttpPattern implements ServerHttpRequestAccept, Cloneable,
		Comparable<HttpPattern> {
	private static final PathMatcher DEFAULT_PATH_MATCHER = new AntPathMatcher();

	private final String path;
	private final String method;
	private PathMatcher pathMatcher;
	private final MimeTypes mimeTypes;

	public HttpPattern(String path) {
		this(path, null, null);
	}

	public HttpPattern(String path, @Nullable String method) {
		this(path, method, null);
	}

	public HttpPattern(String path, @Nullable String method,
			@Nullable MimeTypes mimeTypes) {
		this.path = path;
		this.method = method;
		this.mimeTypes = mimeTypes == null ? null : mimeTypes.readyOnly();
	}

	protected HttpPattern(HttpPattern httpPattern) {
		this.path = httpPattern == null ? null : httpPattern.path;
		this.method = httpPattern == null ? null : httpPattern.method;
		this.mimeTypes = httpPattern == null ? null : httpPattern.mimeTypes
				.readyOnly();
		if (httpPattern != null) {
			this.pathMatcher = httpPattern.pathMatcher;
		}
	}

	public PathMatcher getPathMatcher() {
		return pathMatcher == null ? DEFAULT_PATH_MATCHER : pathMatcher;
	}

	public String getPath() {
		return path;
	}

	public String getMethod() {
		return method;
	}

	public MimeTypes getMimeTypes() {
		return mimeTypes;
	}

	public boolean isPattern() {
		if (path == null || method == null) {
			return true;
		}

		return getPathMatcher().isPattern(path);
	}

	@Override
	public boolean accept(ServerHttpRequest request) {
		if (method != null
				&& !ObjectUtils.nullSafeEquals(method, request.getRawMethod())) {
			return false;
		}

		if (mimeTypes != null
				&& !mimeTypes.isCompatibleWith(request.getContentType())) {
			return false;
		}

		if (path != null) {
			if (isPattern()) {
				if (getPathMatcher().match(path, request.getPath())) {
					WebUtils.setRestfulParameterMap(
							request,
							getPathMatcher().extractUriTemplateVariables(path,
									request.getPath()));
					return true;
				}
				return false;
			} else {
				if (!StringUtils.equals(path, request.getPath())) {
					return false;
				}
			}
		}
		return true;
	}

	public Map<String, String> extractUriTemplateVariables(String path) {
		if (path == null) {
			return Collections.emptyMap();
		}

		return getPathMatcher().extractUriTemplateVariables(path, path);
	}

	@Override
	public int hashCode() {
		int code = 0;
		if (path != null) {
			code += path.hashCode();
		}

		if (method != null) {
			code += method.hashCode();
		}

		if (mimeTypes != null) {
			code += mimeTypes.hashCode();
		}
		return code;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (obj instanceof HttpPattern) {
			HttpPattern httpPattern = (HttpPattern) obj;
			if (!ObjectUtils.nullSafeEquals(method, httpPattern.method)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(mimeTypes, httpPattern.mimeTypes)) {
				return false;
			}

			if (StringUtils.isEmpty(path)
					&& StringUtils.isEmpty(((HttpPattern) obj).path)) {
				return true;
			}

			if (StringUtils.isNotEmpty(path)
					&& StringUtils.isNotEmpty(((HttpPattern) obj).path)) {
				return getPathMatcher().match(path, ((HttpPattern) obj).path)
						|| getPathMatcher().match(((HttpPattern) obj).path,
								path);
			}
		}
		return false;
	}

	@Override
	public String toString() {
		if (method == null && path == null && mimeTypes == null) {
			return "[ANY]";
		}

		StringBuilder sb = new StringBuilder();
		if (method != null) {
			sb.append(method);
		}

		if (path != null) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(path);
		}

		if (mimeTypes != null) {
			sb.append(mimeTypes);
		}
		return sb.toString();
	}

	@Override
	public HttpPattern clone() {
		return new HttpPattern(this);
	}

	@Override
	public int compareTo(HttpPattern o) {
		if (o.mimeTypes != null && this.mimeTypes != null) {
			return this.mimeTypes.compareTo(o.mimeTypes);
		}
		return this.equals(o) ? 0 : 1;
	}

	public HttpPattern setPathMatcher(PathMatcher pathMatcher) {
		HttpPattern httpPattern = new HttpPattern(this);
		httpPattern.pathMatcher = pathMatcher;
		return httpPattern;
	}
}
