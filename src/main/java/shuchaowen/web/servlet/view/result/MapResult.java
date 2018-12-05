package shuchaowen.web.servlet.view.result;

import java.util.HashMap;
import java.util.Map;

public class MapResult extends DataResult<Map<String, Object>>{
	private static final long serialVersionUID = 1L;
	private Map<String, Object> dataMap = new HashMap<String, Object>();
	
	@Override
	public Map<String, Object> getData() {
		return dataMap;
	}
	
	public MapResult put(String key, Object value){
		dataMap.put(key, value);
		return this;
	}
}
