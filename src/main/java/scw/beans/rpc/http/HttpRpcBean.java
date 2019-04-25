package scw.beans.rpc.http;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;

import scw.aop.Invoker;
import scw.beans.BeanFactory;
import scw.beans.BeanUtils;
import scw.common.utils.SignUtils;
import scw.common.utils.XUtils;
import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.net.AbstractResponse;
import scw.net.NetworkUtils;
import scw.net.http.HttpRequest;

public final class HttpRpcBean extends AbstractInterfaceProxyBean {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private final String host;
	private final String signStr;
	private final Charset charset;
	private final BeanFactory beanFactory;

	public HttpRpcBean(BeanFactory beanFactory, Class<?> interfaceClass, String host, String signStr, Charset charset)
			throws Exception {
		super(interfaceClass);
		this.beanFactory = beanFactory;
		this.host = host;
		this.signStr = signStr;
		this.charset = charset;
	}

	@SuppressWarnings("unchecked")
	public <T> T newInstance() {
		Object newProxyInstance = Proxy.newProxyInstance(getType().getClassLoader(), new Class[] { getType() },
				new InvocationHandler() {

					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						HttpConsumerInvoker httpConsumerInvoker = new HttpConsumerInvoker(method);
						return httpConsumerInvoker.invoke(args);
					}
				});
		return (T) BeanUtils.proxyInterface(beanFactory, getType(), newProxyInstance);
	}

	final class HttpConsumerInvoker implements Invoker {
		private Method method;

		public HttpConsumerInvoker(Method method) {
			this.method = method;
		}

		public Object invoke(Object... args) throws Throwable {
			long cts = System.currentTimeMillis();
			final Message message = new Message(method, args);
			message.setAttribute("t", cts);
			message.setAttribute("sign", SignUtils.md5Str((cts + signStr).getBytes(charset)));

			HttpRequest request = new HttpRequest(scw.net.http.enums.Method.POST, host) {
				@Override
				public void doOutput(OutputStream os) throws Throwable {
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(message);
				}
			};

			try {
				return NetworkUtils.execute(request, new AbstractResponse<Object>() {

					@Override
					public Object doInput(InputStream is) throws Throwable {
						ObjectInputStream ois = null;
						try {
							ois = new ObjectInputStream(is);
							return ois.readObject();
						} finally {
							XUtils.close(ois);
						}
					}
				});
			} catch (Throwable e) {
				logger.error(message.getMessageKey());
				throw e;
			}
		}
	}
}
