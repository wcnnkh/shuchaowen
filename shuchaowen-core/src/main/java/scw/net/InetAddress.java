package scw.net;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * 使用此类的原因是{@see InetSocketAddress#getHostName()}会反查dns
 * 
 * @author shuchaowen
 *
 */
public class InetAddress implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String host;
	private final int port;
	private InetSocketAddress inetSocketAddress;

	public InetAddress(String host, int port) {
		this(host, port, new InetSocketAddress(host, port));
	}

	public InetAddress(String host, int port, InetSocketAddress inetSocketAddress) {
		this.host = host;
		this.port = port;
		this.inetSocketAddress = inetSocketAddress;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}
}
