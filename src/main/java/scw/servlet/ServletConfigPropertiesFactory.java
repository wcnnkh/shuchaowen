package scw.servlet;

import javax.servlet.ServletConfig;

import scw.application.CommonApplication;
import scw.beans.property.XmlPropertiesFactory;
import scw.core.PropertiesFactory;
import scw.core.utils.ConfigUtils;
import scw.core.utils.StringUtils;

public class ServletConfigPropertiesFactory implements PropertiesFactory {
	private final ServletConfig servletConfig;
	private final PropertiesFactory propertiesFactory;
	private final String configXml;

	public ServletConfigPropertiesFactory(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
		String configXml = getServletConfig("shuchaowen");
		if (StringUtils.isEmpty(configXml)) {
			configXml = CommonApplication.getDefaultConfigPath();
		}
		this.configXml = StringUtils.isEmpty(configXml) ? null : configXml;
		this.propertiesFactory = new XmlPropertiesFactory(this.configXml);
	}

	public String getConfig(String name) {
		String value = servletConfig.getInitParameter(name);
		if (value == null) {
			value = ConfigUtils.getSystemProperty(name);
		}
		return value;
	}

	public String getServletConfig(String key) {
		return servletConfig.getInitParameter(key);
	}

	public String getValue(String key) {
		String value = null;
		if (propertiesFactory != null) {
			value = propertiesFactory.getValue(key);
		}

		if (value == null) {
			value = getServletConfig(key);
		}
		return value;
	}

	public String getConfigXml() {
		return configXml;
	}
}
