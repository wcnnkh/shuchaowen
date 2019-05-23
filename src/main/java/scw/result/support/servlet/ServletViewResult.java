package scw.result.support.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import scw.core.logger.DebugLogger;
import scw.core.net.http.ContentType;
import scw.json.JSONUtils;
import scw.result.support.DefaultResult;
import scw.servlet.Request;
import scw.servlet.Response;
import scw.servlet.View;

public class ServletViewResult<T> extends DefaultResult<T> implements View {
	private static final long serialVersionUID = 1L;
	private String contentType;

	protected ServletViewResult() {
	}

	public ServletViewResult(boolean success, int code, T data, String msg,
			String contentType) {
		super(success, code, data, msg);
		this.contentType = contentType;
	}

	protected String parseString(Object obj) {
		return JSONUtils.toJSONString(obj);
	}

	public void render(Request request, Response response) throws IOException {
		if (contentType != null) {
			response.setContentType(contentType);
		}

		if (response.getContentType() == null) {
			response.setContentType(ContentType.TEXT_HTML);
		}

		Map<String, Object> map = new HashMap<String, Object>(4, 1);
		map.put("success", isSuccess());
		map.put("code", getCode());
		map.put("data", getData());
		map.put("msg", getMsg());

		String content = parseString(map);
		response.getWriter().write(content);

		if (response instanceof DebugLogger) {
			if (((DebugLogger) response).isDebugEnabled()) {
				((DebugLogger) response).debug(content);
			}
		}
	}
}