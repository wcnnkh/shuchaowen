package scw.application.mq;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AmqpParametersConsumer {
	public String routingKey();

	public String queueName();

	public boolean durable() default true;

	public boolean exclusive() default true;

	public boolean autoDelete() default false;
	
	public String paramsExchange();
}
