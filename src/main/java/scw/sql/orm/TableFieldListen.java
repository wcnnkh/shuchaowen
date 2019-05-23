package scw.sql.orm;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;

public interface TableFieldListen extends Serializable{
	public static final String GET_CHANGE_MAP = "get_field_change_map";
	public static final String CLEAR_FIELD_LISTEN = "clear_field_listen";
	
	/**
	 * 返回的map是调用了set方法的字段，值是在调用startFieldListen之前的值
	 * @return
	 */
	Map<String, Object> get_field_change_map();
	
	void field_change(Field fieldInfo, Object oldValue);
	
	/**
	 *  清空监听数据
	 */
	void clear_field_listen();
}