/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class MissingAttributeTestFilter implements Filter {
	
	private List<String> hideAttributesNamed = new ArrayList<String>();

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(final ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request) {
	        @Override
	        public String getHeader(String name) {
	        	if(hideAttributesNamed.contains(name.toLowerCase()))
	        		return null;
	            return super.getHeader(name);
	        }
	        
	        @Override
	        public Enumeration getHeaderNames() {
	        	Vector<String> headerNames = new Vector<String>();
	        	Enumeration allNames = super.getHeaderNames();
	        	while(allNames.hasMoreElements()) {
	        		String name = (String)allNames.nextElement();
	        		if(!hideAttributesNamed.contains(name.toLowerCase()))
	        			headerNames.add(name);
	        	}
	        	return headerNames.elements();
	        }
	    };

		chain.doFilter(wrapper, response);
	}

	@Override
	public void init(FilterConfig fc) throws ServletException {		
		    String toHide = fc.getInitParameter("hide").toLowerCase();
		    hideAttributesNamed = Arrays.asList(toHide.split(","));
	}

}


