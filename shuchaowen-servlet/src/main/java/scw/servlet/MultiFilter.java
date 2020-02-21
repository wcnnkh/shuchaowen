package scw.servlet;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class MultiFilter extends LinkedList<Filter> implements Filter {
	private static final long serialVersionUID = 1L;

	public void init(FilterConfig filterConfig) throws ServletException {
		for (Filter filter : this) {
			filter.init(filterConfig);
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		FilterChain filterChain = new ServletFilterChain(this, chain);
		filterChain.doFilter(request, response);
	}

	public void destroy() {
		for (Filter filter : this) {
			filter.destroy();
		}
	}

}
