package scw.net;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import scw.core.Assert;
import scw.core.utils.StringUtils;
import scw.io.IOUtils;
import scw.net.message.InputMessage;
import scw.net.message.OutputMessage;
import scw.net.message.CacheURLConnectionInputMessage;
import scw.net.message.converter.DefaultMessageConverterChain;
import scw.net.message.converter.MessageConverter;
import scw.net.message.converter.MessageConverterChain;
import scw.net.mime.MimeType;
import scw.net.ssl.TrustAllManager;

public final class NetworkUtils {
	private NetworkUtils() {
	};

	private static final URLConnectionResponseCallback<InputMessage> MESSAGE_RESPONSE = new URLConnectionResponseCallback<InputMessage>() {

		public InputMessage response(URLConnection urlConnection) throws Throwable {
			return new CacheURLConnectionInputMessage(urlConnection);
		}
	};
	/**
	 * 一个信任所有的ssl socket factory <br/>
	 * 注意:在初始化失败后可能为空
	 */
	public static final SSLSocketFactory TRUSE_ALL_SSL_SOCKET_FACTORY;

	static {
		// 创建一个信任所有的
		javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
		javax.net.ssl.TrustManager tm = new TrustAllManager();
		trustAllCerts[0] = tm;
		javax.net.ssl.SSLContext sc = null;
		try {
			sc = javax.net.ssl.SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, null);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
		TRUSE_ALL_SSL_SOCKET_FACTORY = sc == null ? null : sc.getSocketFactory();
	}

	public static <T> T execute(URLConnection urlConnection, URLConnectionRequestCallback uRLConnectionRequestCallback,
			URLConnectionResponseCallback<T> response) throws Throwable {
		uRLConnectionRequestCallback.request(urlConnection);
		return response.response(urlConnection);
	}

	public static InputMessage execute(URLConnection urlConnection, URLConnectionRequestCallback uRLConnectionRequestCallback) throws Throwable {
		return execute(urlConnection, uRLConnectionRequestCallback, MESSAGE_RESPONSE);
	}

	public static <T> T execute(URL url, Proxy proxy, URLConnectionRequestCallback uRLConnectionRequestCallback, URLConnectionResponseCallback<T> response) {
		Assert.argumentNotNull(url, "url");
		Assert.argumentNotNull(url, "request");
		Assert.argumentNotNull(url, "response");

		URLConnection urlConnection = null;
		try {
			if (proxy == null) {
				urlConnection = url.openConnection();
			} else {
				urlConnection = url.openConnection(proxy);
			}

			return execute(urlConnection, uRLConnectionRequestCallback, response);
		} catch (Throwable e) {
			throw new RuntimeException(url.toString(), e);
		} finally {
			if (urlConnection != null) {
				if (urlConnection instanceof HttpURLConnection) {
					((HttpURLConnection) urlConnection).disconnect();
				}
			}
		}
	}

	public static InputMessage execute(URL url, Proxy proxy, URLConnectionRequestCallback uRLConnectionRequestCallback) {
		return execute(url, proxy, uRLConnectionRequestCallback, MESSAGE_RESPONSE);
	}

	public static <T> T execute(String url, Proxy proxy, URLConnectionRequestCallback uRLConnectionRequestCallback,
			URLConnectionResponseCallback<T> response) {
		URL u = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			new RuntimeException(e);
		}

		if (u == null) {
			throw new NullPointerException(url);
		}

		return execute(u, proxy, uRLConnectionRequestCallback, response);
	}

	public static InputMessage execute(String url, Proxy proxy, URLConnectionRequestCallback uRLConnectionRequestCallback) {
		return execute(url, proxy, uRLConnectionRequestCallback, MESSAGE_RESPONSE);
	}

	public static <T> T execute(URLRequestCallback request, URLConnectionResponseCallback<T> response) {
		return execute(request.getURL(), request.getProxy(), request, response);
	}

	public static InputMessage execute(URLRequestCallback request) {
		return execute(request, MESSAGE_RESPONSE);
	}

	public static List<InetSocketAddress> parseInetSocketAddressList(String address) {
		List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
		String[] arr = StringUtils.commonSplit(address);
		for (String a : arr) {
			String[] vs = a.split(":");
			String h = vs[0];
			int port = 11211;
			if (vs.length == 2) {
				port = Integer.parseInt(vs[1]);
			}

			addresses.add(new InetSocketAddress(h, port));
		}
		return addresses;
	}

	public static boolean checkPortCccupied(InetAddress inetAddress, int port) {
		Socket socket = null;
		try {
			socket = new Socket(inetAddress, port);
			return true;
		} catch (IOException e) {
			// ignore
		} finally {
			IOUtils.close(false, socket);
		}
		return false;
	}

	/**
	 * 检查端口号占用
	 * 
	 * @param host
	 * @param port
	 * @return
	 */
	public static boolean checkPortCccupied(String host, int port) {
		Socket socket = null;
		try {
			socket = new Socket(host, port);
			return true;
		} catch (IOException e) {
			// ignore
		} finally {
			IOUtils.close(false, socket);
		}
		return false;
	}

	/**
	 * 检查本地端口是否被占用
	 * 
	 * @param port
	 * @return
	 */
	public static boolean checkLocalPortCccupied(int port) {
		return checkPortCccupied("127.0.0.1", port);
	}

	public static Object read(Type type, InputMessage inputMessage, Collection<MessageConverter> messageConverters)
			throws IOException {
		MessageConverterChain chain = new DefaultMessageConverterChain(messageConverters, null);
		return chain.read(type, inputMessage);
	}

	public static void write(Object body, MimeType contentType, OutputMessage outputMessage,
			Collection<MessageConverter> messageConverters) throws IOException {
		MessageConverterChain chain = new DefaultMessageConverterChain(messageConverters, null);
		chain.write(body, contentType, outputMessage);
	}
}
