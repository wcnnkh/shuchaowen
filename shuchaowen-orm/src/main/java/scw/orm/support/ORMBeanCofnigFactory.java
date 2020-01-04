package scw.orm.support;

import scw.application.ApplicationConfigUtils;
import scw.beans.AbstractBeanConfigFactory;
import scw.beans.BeanFactory;
import scw.beans.SimpleBeanConfigFactory;
import scw.beans.annotation.Configuration;
import scw.beans.property.ValueWiredManager;
import scw.core.Init;
import scw.core.PropertyFactory;
import scw.core.utils.StringUtils;
import scw.orm.ORMUtils;

@Configuration
public class ORMBeanCofnigFactory extends AbstractBeanConfigFactory implements SimpleBeanConfigFactory {

	public void init(ValueWiredManager valueWiredManager, BeanFactory beanFactory, PropertyFactory propertyFactory) {
		addInit(new OrmProxyRegister(propertyFactory));
	}

	private static class OrmProxyRegister implements Init {
		private PropertyFactory propertyFactory;

		public OrmProxyRegister(PropertyFactory propertyFactory) {
			this.propertyFactory = propertyFactory;
		}

		public void init() {
			String ormScanPackageName = propertyFactory.getProperty("orm.scan");
			if (StringUtils.isNotEmpty(ormScanPackageName)) {
				ORMUtils.registerCglibProxyTableBean(ormScanPackageName);
			} else {
				ORMUtils.registerCglibProxyTableBean(ApplicationConfigUtils.getORMPackage(propertyFactory));
			}
		}

	}
}