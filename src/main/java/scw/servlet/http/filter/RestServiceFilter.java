package scw.servlet.http.filter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import scw.core.exception.AlreadyExistsException;
import scw.core.net.http.Method;
import scw.servlet.Action;
import scw.servlet.MethodAction;
import scw.servlet.annotation.Controller;
import scw.servlet.http.DefaultHttpRequest;

final class RestServiceFilter extends AbstractHttpServiceFilter {
	private final EnumMap<Method, Map<String, RestInfo>> restMap = new EnumMap<Method, Map<String, RestInfo>>(
			Method.class);

	@Override
	public void scanning(Class<?> clz, java.lang.reflect.Method method, Controller classController,
			Controller methodController, Action action) {
		RestInfo restUrlInfo = getRestInfo(action, clz, method);
		if (restUrlInfo == null) {
			return;
		}

		if (restUrlInfo.getKeyMap().size() == 0) {
			return;
		}

		/**
		 * resturl
		 */
		scw.core.net.http.Method[] types = MethodAction.mergeRequestType(clz, method);
		for (scw.core.net.http.Method type : types) {
			Map<String, RestInfo> map = restMap.get(type);
			if (map == null) {
				map = new HashMap<String, RestInfo>();
			}

			if (map.containsKey(restUrlInfo.getUrl())) {
				throw new AlreadyExistsException(ServletPathServiceFilter.getExistActionErrMsg(restUrlInfo.getAction(),
						map.get(restUrlInfo.getUrl()).getAction()));
			}

			map.put(restUrlInfo.getUrl(), restUrlInfo);
			restMap.put(type, map);
		}
	}

	@Override
	public Action getAction(HttpServletRequest request) {
		Method method = Method.valueOf(request.getMethod());
		if (method == null) {
			return null;
		}

		Map<String, RestInfo> map = restMap.get(method);
		if (map == null) {
			return null;
		}

		String[] pathArr = request.getServletPath().split("/");
		for (Entry<String, RestInfo> entry : map.entrySet()) {
			RestInfo restUrl = entry.getValue();
			if (pathArr.length != restUrl.getRegArr().length) {
				continue;
			}

			Map<String, String> valueMap = null;
			boolean find = true;
			for (int i = 0; i < restUrl.getRegArr().length; i++) {
				String str = restUrl.getRegArr()[i];
				if ("*".equals(str)) {
					if (valueMap == null) {
						valueMap = new HashMap<String, String>();
					}

					String key = restUrl.getKeyMap().get(i);
					valueMap.put(key, pathArr[i]);
				} else if (!pathArr[i].equals(str)) {
					find = false;
					break;
				}
			}

			if (find) {
				request.setAttribute(DefaultHttpRequest.RESTURL_PATH_PARAMETER, valueMap);
				return restUrl.getAction();
			}
		}
		return null;
	}
}