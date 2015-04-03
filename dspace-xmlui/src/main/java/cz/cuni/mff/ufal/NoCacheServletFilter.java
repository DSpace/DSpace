/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class NoCacheServletFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(final ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		httpResponse.setDateHeader("Expires", 0); // Proxies.
	    
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {		
	}

}


