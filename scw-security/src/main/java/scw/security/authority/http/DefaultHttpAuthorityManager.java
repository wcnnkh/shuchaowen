package scw.security.authority.http;

import java.util.HashMap;
import java.util.Map;

import scw.core.utils.StringUtils;
import scw.json.JSONUtils;
import scw.lang.AlreadyExistsException;
import scw.security.authority.DefaultAuthorityManager;

public class DefaultHttpAuthorityManager<T extends HttpAuthority> extends
		DefaultAuthorityManager<T> implements HttpAuthorityManager<T> {
	private Map<String, Map<String, String>> pathMap = new HashMap<String, Map<String, String>>();

	public T getAuthority(String path, String method) {
		Map<String, String> map = pathMap.get(path);
		if (map == null) {
			return null;
		}

		String id = map.get(method);
		if (id == null) {
			return null;
		}

		return getAuthority(id);
	}

	public synchronized void register(T authority) {
		if (StringUtils.isNotEmpty(authority.getPath())) {
			Map<String, String> map = pathMap.get(authority.getPath());
			if (map == null) {
				map = new HashMap<String, String>();
			}

			if (map.containsKey(authority.getMethod())) {
				throw new AlreadyExistsException(
						JSONUtils.getJsonSupport().toJSONString(authority));
			}

			map.put(authority.getMethod(), authority.getId());
			pathMap.put(authority.getPath(), map);
		}
		super.register(authority);
	};
	
	@Override
	public void remove(T authority) {
		if (StringUtils.isNotEmpty(authority.getPath())) {
			Map<String, String> map = pathMap.get(authority.getPath());
			if(map != null){
				map.remove(authority.getMethod());
			}
		}
		super.remove(authority);
	}

}
