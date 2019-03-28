package scw.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import scw.common.ByteArray;
import scw.net.http.HttpException;
import scw.net.response.ByteArrayResponse;

public final class NetworkUtils {
	private NetworkUtils() {
	};

	public static <T> T execute(URLConnection urlConnection, Request request, Response<T> response) throws Throwable {
		request.request(urlConnection);
		return response.response(urlConnection);
	}

	public static <T> T execute(URL url, Proxy proxy, Request request, Response<T> response) {
		URLConnection urlConnection = null;
		try {
			if (proxy == null) {
				urlConnection = url.openConnection();
			} else {
				urlConnection = url.openConnection(proxy);
			}

			return execute(urlConnection, request, response);
		} catch (Throwable e) {
			if(urlConnection == null){
				throw new RuntimeException(url.toString(), e);
			}else{
				if(urlConnection instanceof HttpURLConnection){
					try {
						int code = ((HttpURLConnection) urlConnection).getResponseCode();
						String message = ((HttpURLConnection) urlConnection).getResponseMessage();
						throw new HttpException(url.toString(), code, message, e);
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}else{
					throw new RuntimeException(url.toString(), e);
				}
			}
		} finally {
			if (urlConnection != null) {
				if (urlConnection instanceof HttpURLConnection) {
					((HttpURLConnection) urlConnection).disconnect();
				}
			}
		}
	}

	public static <T> T execute(String url, Proxy proxy, Request request, Response<T> response) {
		URL u = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			new RuntimeException(e);
		}

		if (u == null) {
			throw new NullPointerException(url);
		}

		return execute(u, proxy, request, response);
	}

	public static <T> T execute(AbstractUrlRequest request, Response<T> response) {
		return execute(request.getURL(), request.getProxy(), request, response);
	}

	public static ByteArray execute(AbstractUrlRequest request) {
		return execute(request, new ByteArrayResponse());
	}
}
