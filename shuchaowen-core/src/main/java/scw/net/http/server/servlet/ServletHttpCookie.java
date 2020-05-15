package scw.net.http.server.servlet;

import java.util.Date;

import scw.net.http.HttpCookie;

public class ServletHttpCookie implements HttpCookie {
	private javax.servlet.http.Cookie cookie;

	public ServletHttpCookie(javax.servlet.http.Cookie cookie) {
		this.cookie = cookie;
	}

	public ServletHttpCookie(String name, String value) {
		this.cookie = new javax.servlet.http.Cookie(name, value);
	}

	public String getName() {
		return cookie.getName();
	}

	public String getValue() {
		return cookie.getValue();
	}

	public String getPath() {
		return cookie.getPath();
	}

	public boolean isSecure() {
		return cookie.getSecure();
	}

	public String getDomain() {
		return cookie.getDomain();
	}

	public int getMaxAge() {
		return cookie.getMaxAge();
	}

	public Date getExpires() {
		return new Date(System.currentTimeMillis() + cookie.getMaxAge());
	}

	public javax.servlet.http.Cookie getCookie() {
		return cookie;
	}

	public void setCookie(javax.servlet.http.Cookie cookie) {
		this.cookie = cookie;
	}
}