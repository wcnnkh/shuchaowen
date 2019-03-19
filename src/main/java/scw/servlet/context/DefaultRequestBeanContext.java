package scw.servlet.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import scw.common.exception.BeansException;
import scw.servlet.Request;
import scw.servlet.beans.RequestBean;
import scw.servlet.beans.RequestBeanFactory;

public class DefaultRequestBeanContext implements RequestBeanContext{
	private volatile LinkedHashMap<String, Object> beanMap;
	private Request request;
	private RequestBeanFactory requestBeanFactory;
	
	public DefaultRequestBeanContext(Request request, RequestBeanFactory requestBeanFactory){
		this.request = request;
		this.requestBeanFactory = requestBeanFactory;
	}

	public final <T> T getBean(Class<T> type) {
		return getBean(type, null);
	}

	@SuppressWarnings("unchecked")
	public final <T> T getBean(Class<T> type, String name) {
		RequestBean requestBean = null;
		if (name != null && name.length() != 0) {
			requestBean = requestBeanFactory.get(name);
		}
		
		if(requestBean == null){
			requestBean = requestBeanFactory.get(type.getName());
		}

		if (requestBean == null) {
			return null;
		}

		return (T) getBean(requestBean, type, name);
	}

	private Object getBean(RequestBean requestBean, Class<?> type, String name) {
		Object obj = null;
		if (beanMap == null) {
			synchronized (this) {
				if (beanMap == null) {
					beanMap = new LinkedHashMap<String, Object>(4);
					obj = newInstanceReuestBean(requestBean, type, name);
				}
			}
		} else {
			obj = beanMap.get(requestBean.getId());
			if (obj == null) {
				synchronized (this) {
					obj = beanMap.get(requestBean.getId());
					if (obj == null) {
						obj = newInstanceReuestBean(requestBean, type, name);
					}
				}
			}
		}
		return obj;
	}

	private Object newInstanceReuestBean(RequestBean requestBean,
			Class<?> type, String name) {
		Object obj = requestBean.newInstance(request);
		if (obj != null) {
			beanMap.put(requestBean.getId(), obj);
			try {
				requestBean.autowrite(obj);
				requestBean.init(obj);
			} catch (Exception e) {
				throw new BeansException(e);
			}
		}
		return obj;
	}

	public void destroy() {
		if (beanMap != null) {
			synchronized (this) {
				if (beanMap != null) {
					List<String> beanKeyList = new ArrayList<String>();
					for (Entry<String, Object> entry : beanMap.entrySet()) {
						beanKeyList.add(entry.getKey());
					}

					for (String id : beanKeyList) {
						RequestBean requestBean = requestBeanFactory.get(id);
						try {
							requestBean.destroy(beanMap.get(id));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				beanMap = null;
			}
		}
	}
}
