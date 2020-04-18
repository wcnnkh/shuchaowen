package scw.mq;

import java.lang.reflect.Method;

import scw.beans.AbstractBeanFactoryLifeCycle;
import scw.beans.AutoProxyMethodInvoker;
import scw.beans.BeanFactory;
import scw.beans.BeanUtils;
import scw.core.GlobalPropertyFactory;
import scw.core.annotation.AnnotationUtils;
import scw.core.instance.annotation.Configuration;
import scw.core.utils.ClassUtils;
import scw.mq.amqp.Exchange;
import scw.mq.annotation.AmqpConsumer;
import scw.mq.annotation.Consumer;
import scw.mq.support.MqMethodConsumer;
import scw.util.value.property.PropertyFactory;

@Configuration(order=Integer.MIN_VALUE)
public final class MQAnnotationScan extends AbstractBeanFactoryLifeCycle {

	public void init(BeanFactory beanFactory, PropertyFactory propertyFactory)
			throws Exception {
		for (Class<?> clazz : ClassUtils
				.getClassSet(getScanAnnotationPackageName())) {
			scanningConsumer(beanFactory, clazz);
			scanningAMQPConsumer(beanFactory, clazz);
		}
	}

	public String getScanAnnotationPackageName() {
		return GlobalPropertyFactory.getInstance().getValue(
				"scw.scan.mq.package", String.class,
				BeanUtils.getScanAnnotationPackageName());
	}

	private void scanningConsumer(BeanFactory beanFactory, Class<?> clz) {
		for (Method method : AnnotationUtils.getAnnoationMethods(clz, true,
				true, Consumer.class)) {
			Consumer c = method.getAnnotation(Consumer.class);
			ConsumerFactory consumerFactory = beanFactory.getInstance(c
					.factory());
			logger.info("添加消费者：{}, name={}, factory={}", method, c.name(),
					c.factory());
			consumerFactory.bindConsumer(c.name(), new MqMethodConsumer(
					new AutoProxyMethodInvoker(beanFactory, clz, method)));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void scanningAMQPConsumer(BeanFactory beanFactory,
			Class<?> clz) {
		for (Method method : AnnotationUtils.getAnnoationMethods(clz, true,
				true, AmqpConsumer.class)) {
			AmqpConsumer c = method.getAnnotation(AmqpConsumer.class);
			Exchange mq = beanFactory.getInstance(c.exchangeService());
			logger.info(
					"添加消费者：{}, amqp routingKey={}, queueName={}, durable={}, exclusive={}, autoDelete={}",
					method, c.routingKey(), c.queueName(), c.durable(),
					c.exclusive(), c.autoDelete());
			mq.bindConsumer(c.routingKey(), c.queueName(), c.durable(), c
					.exclusive(), c.autoDelete(), new MqMethodConsumer(
					new AutoProxyMethodInvoker(beanFactory, clz, method)));
		}
	}

	public void destroy(BeanFactory beanFactory, PropertyFactory propertyFactory) {
	}
}