package scw.net.http.client;

import java.io.IOException;
import java.net.URI;

import scw.net.http.Method;

public interface ClientHttpRequestFactory {
	ClientHttpRequest createRequest(URI uri, Method httpMethod) throws IOException;
}
