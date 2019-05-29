package scw.beans.tcc.service;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import scw.beans.BeanFactory;
import scw.beans.annotation.Autowrite;
import scw.beans.tcc.InvokeInfo;
import scw.beans.tcc.StageType;
import scw.beans.tcc.TCCService;
import scw.core.Constants;
import scw.core.serializer.Serializer;
import scw.mq.rabbit.RabbitUtils;
import scw.transaction.DefaultTransactionLifeCycle;
import scw.transaction.TransactionManager;

public final class RabbitTccService implements TCCService, scw.core.Destroy {
	private Connection connection;
	private ExecutorService executorService = new ThreadPoolExecutor(1, 20, 0, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());
	private Channel channel;
	private final String routingKey;
	@Autowrite
	private BeanFactory beanFactory;
	private final String exchangeName;
	private final Serializer serializer;

	public RabbitTccService(ConnectionFactory connectionFactory, String routingKey)
			throws IOException, TimeoutException {
		this(connectionFactory, "rabbit_tcc_service", routingKey, "queue." + routingKey, Constants.DEFAULT_SERIALIZER);
	}

	public RabbitTccService(ConnectionFactory connectionFactory, String routingKey, Serializer serializer)
			throws IOException, TimeoutException {
		this(connectionFactory, "rabbit_tcc_service", routingKey, "queue." + routingKey, serializer);
	}

	public RabbitTccService(ConnectionFactory connectionFactory, String exchangeName, String routingKey,
			String queueName, Serializer serializer) throws IOException, TimeoutException {
		this.routingKey = routingKey;
		this.exchangeName = exchangeName;
		this.connection = connectionFactory.newConnection();
		this.serializer = serializer;
		channel = connection.createChannel();
		channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT);
		channel.queueDeclare(queueName, true, true, false, null);
		channel.queueBind(queueName, exchangeName, routingKey);
		channel.basicConsume(queueName, false, new TccConsumter(channel));
	}

	public void destroy() {
		executorService.shutdownNow();
		try {
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void invoke(InvokeInfo invokeInfo, StageType stageType) {
		if (!invokeInfo.hasCanInvoke(stageType)) {
			return;
		}

		TransactionInfo info = new TransactionInfo(invokeInfo, stageType);
		RabbitUtils.basicPublish(channel, exchangeName, routingKey, serializer.serialize(info));
	}

	public void service(final InvokeInfo invokeInfo) {
		TransactionManager.transactionLifeCycle(new DefaultTransactionLifeCycle() {
			@Override
			public void beforeProcess() {
				invoke(invokeInfo, StageType.Confirm);
			}

			@Override
			public void beforeRollback() {
				invoke(invokeInfo, StageType.Cancel);
			}

			@Override
			public void complete() {
				invoke(invokeInfo, StageType.Complete);
			}
		});
	}

	final class TccConsumter extends DefaultConsumer {

		public TccConsumter(Channel channel) {
			super(channel);
		}

		@Override
		public void handleDelivery(String consumerTag, final Envelope envelope, BasicProperties properties, byte[] body)
				throws IOException {
			final TransactionInfo info = serializer.deserialize(body);
			executorService.execute(new Runnable() {

				public void run() {
					try {
						info.invoke(beanFactory);
						getChannel().basicAck(envelope.getDeliveryTag(), false);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
