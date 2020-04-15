package scw.servlet.mvc.http;

import javax.servlet.http.HttpSession;

import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.mvc.http.HttpChannel;
import scw.mvc.http.session.HttpChannelUserSessionFactory;
import scw.security.session.UserSession;

public final class HttpServletChannelUserSessionFactory<T> implements HttpChannelUserSessionFactory<T> {
	private static Logger logger = LoggerFactory.getLogger(HttpServletChannelUserSessionFactory.class);
	private String uidAttributeName;

	public HttpServletChannelUserSessionFactory(String uidAttributeName) {
		this.uidAttributeName = uidAttributeName;
	}

	public UserSession<T> getUserSession(HttpChannel httpChannel) {
		if (!(httpChannel instanceof HttpServletChannel)) {
			logger.warn("{} not a HttpServletRequest", httpChannel.getRequest().getController());
			return null;
		}

		HttpServletChannel httpServletChannel = (HttpServletChannel) httpChannel;
		HttpSession httpSession = httpServletChannel.getRequest().getHttpServletRequest().getSession();
		if (httpSession == null) {
			return null;
		}

		return new HttpServletUserSession<T>(httpSession, uidAttributeName);
	}

	public UserSession<T> createUserSession(HttpChannel httpChannel, T uid) {
		if (!(httpChannel instanceof HttpServletChannel)) {
			logger.warn("{} not a HttpServletRequest", httpChannel.getRequest().getController());
			return null;
		}

		HttpServletChannel httpServletChannel = (HttpServletChannel) httpChannel;
		HttpSession httpSession = httpServletChannel.getRequest().getHttpServletRequest().getSession(true);
		if (httpSession == null) {
			return null;
		}

		HttpServletUserSession<T> session = new HttpServletUserSession<T>(httpSession, uidAttributeName);
		session.setUid(uid);
		return session;
	}

}
