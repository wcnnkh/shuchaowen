package scw.application.consumer;

import scw.core.Parameters;
import scw.core.aop.Invoker;

@SuppressWarnings("rawtypes")
public class MqMethodConsumer implements scw.core.Consumer {
	private Invoker invoker;

	public MqMethodConsumer(Invoker invoker) {
		this.invoker = invoker;
	}

	public void consume(Object message) {
		if (message == null) {
			return;
		}

		try {
			if (message instanceof Parameters) {
				invoker.invoke(((Parameters) message).getParameters());
			} else {
				if (message.getClass().isArray()) {
					invoker.invoke((Object[]) message);
				} else {
					invoker.invoke(message);
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}