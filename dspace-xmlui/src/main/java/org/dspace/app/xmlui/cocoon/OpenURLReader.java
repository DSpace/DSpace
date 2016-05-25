/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.xml.sax.SAXException;

/**
 * Simple servlet for open URL support. Presently, simply extracts terms from
 * open URL and redirects to search.
 * 
 * @author Robert Tansley
 * @author Mark Diggory (mdiggory at mire.be)
 * @version $Revision$
 */
public class OpenURLReader extends AbstractReader implements Recyclable {

	private static final String Z39882004 = "Z39.88-2004";

	private static final String Z39882004DC = "info:ofi/fmt:kev:mtx:dc";

	private static final String Z39882004CTX = "info:ofi/fmt:kev:mtx:ctx";

	/** The Cocoon response */
	protected Response response;

	/** The Cocoon request */
	protected Request request;

	/** The Servlet Response */
	protected HttpServletResponse httpResponse;

	protected Context context;

	/** Logger */
	private static Logger log = Logger.getLogger(OpenURLReader.class);

	protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    @Override
	public void generate() throws IOException, SAXException,
			ProcessingException {
	}

	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src,
			Parameters par) throws ProcessingException, SAXException,
			IOException {

		super.setup(resolver, objectModel, src, par);

		try {
			this.httpResponse = (HttpServletResponse) objectModel
					.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
			this.request = ObjectModelHelper.getRequest(objectModel);
			this.response = ObjectModelHelper.getResponse(objectModel);
			this.context = ContextUtil.obtainContext(objectModel);

			if (Z39882004.equals(request.getParameter("url_ver"))) {
				handleZ39882004();
			} else {
				handleLegacy();
			}
		} catch (SQLException sqle) {
			throw new ProcessingException("Unable to resolve OpenURL.", sqle);
		}

	}

	@Override
	public void recycle() {
		super.recycle();
		this.response = null;
		this.request = null;
		this.httpResponse = null;
		this.context = null;
	}

	public void handleLegacy() throws IOException {
		String query = "";

		String title = request.getParameter("title");
		String authorFirst = request.getParameter("aufirst");
		String authorLast = request.getParameter("aulast");

		String logInfo = "";

		if (title != null) {
			query = query + " " + title;
			logInfo = logInfo + "title=\"" + title + "\",";
		}

		if (authorFirst != null) {
			query = query + " " + authorFirst;
			logInfo = logInfo + "aufirst=\"" + authorFirst + "\",";
		}

		if (authorLast != null) {
			query = query + " " + authorLast;
			logInfo = logInfo + "aulast=\"" + authorLast + "\",";
		}

		log.info(LogManager.getHeader(context, "openURL", logInfo
				+ "dspacequery=" + query));

		httpResponse.sendRedirect(httpResponse.encodeRedirectURL(request
				.getContextPath()
				+ "/simple-search?query=" + query));

	}

	private String getFirstHandle(String query) throws IOException {
                
            // Construct a Discovery query
            DiscoverQuery queryArgs = new DiscoverQuery();
            queryArgs.setQuery(query);
            // we want Items only
            queryArgs.setDSpaceObjectFilter(Constants.ITEM);
                
            try
            {
                DiscoverResult queryResults = SearchUtils.getSearchService().search(context, queryArgs);
                List<DSpaceObject> objResults = queryResults.getDspaceObjects();

                if(objResults!=null && !objResults.isEmpty())
                    return objResults.get(0).getHandle();
                else
                    return null;
            }
            catch(SearchServiceException e)
            {
                throw new IOException(e);
            }
	}

	/**
	 * Validate supported formats.
	 *
     * <p>
	 * We can deal with various formats if they exist such as journals and
	 * books, but we currently do not have specific needs to represent
	 * different formats, thus it may be more appropriate to use Dublin Core
	 * here directly.
	 *
     * <p>
	 * {@code rft_val_fmt=info:ofi/fmt:kev:mtx:dc}
	 *
     * <p>
	 * See Dublin Core OpenURL Profile Citation Guidelines:
     * <ul>
	 * <li><a href="http://dublincore.org/documents/dc-citation-guidelines/">Dublin Core Citation Guidelines</a>
	 * <li><a href="http://alcme.oclc.org/openurl/servlet/OAIHandler/extension?verb=GetMetadata&metadataPrefix=mtx&identifier=info:ofi/fmt:kev:mtx:dc">
     * OpenURL KEV format for DC</a>
     * </ul>
	 *
     * <p>
	 * What happens when we use Context Objects of different versions? Do
	 * they exist? {@code ctx_ver=Z39.88-2004}
	 *
     * <p>
	 * COinS will be implemented as:
	 *
     * <pre>{@code
	 * <span class="Z3988" title="ctx_ver=Z39.88-2004&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft.issn=1045-4438">
	 *  <A HREF="http://library.example.edu/?url_ver=Z39.88-2004&ctx_ver=Z39.88-2004&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft.issn=1045-4438">
	 *   Find at Example Library
     *  </A>
     * </span>
     * }</pre>
	 * 
	 * If a {@code ctx_id} is present, use it to resolve the item directly.
	 * Otherwise, use the search mechanism. Our {@code ctx_id}s are going to be
     * local handle identifiers like the following:
	 *
     * <p>
	 * {@code ctx_id=10255/dryad.111}
	 *
     * <p>
	 * Global identifiers will be any other valid {@code dc.identifier} present
	 * within that field. Thus:
	 *
     * <pre>{@code
	 * dc.identifier.uri http://dx.doi.org/10.1080/106351598260806
	 * dc.identifier.uri http://hdl.handle.net/10255/dryad.111
     * }</pre>
	 *
	 * will lead to
	 *
     * <pre>{@code
	 * rft.identifier=http%3A%2F%2Fdx.doi.org%2F10.1080%2F106351598260806
	 * rft.identifier=http%3A%2F%2Fhdl.handle.net%2F10255%2Fdryad.111
     * }</pre>
	 *
	 * and thus be resolvable as well
     *
     * @throws java.io.IOException passed through.
     * @throws org.apache.cocoon.ProcessingException on unknown formats.
	 * @throws SQLException passed through.
	 */
	public void handleZ39882004() throws IOException, ProcessingException, SQLException {
		
		String rft_val_fmt = request.getParameter("rft_val_fmt");
		if (rft_val_fmt != null && !rft_val_fmt.equals(Z39882004DC))
		{
			throw new ProcessingException(
					"DSpace 1.0 OpenURL Service only supports rft_val_fmt="
							+ Z39882004DC);
		}

		String url_ctx_fmt = request.getParameter("url_ctx_fmt");
		if (url_ctx_fmt != null && !url_ctx_fmt.equals(Z39882004CTX))
		{
			throw new ProcessingException(
					"DSpace 1.0 OpenURL Service only supports url_ctx_fmt="
							+ Z39882004CTX);
		}

		/**
		 * First attempt to resolve an rft_id identifier as a handle
		 */
		String[] rft_ids = request.getParameterValues("rft_id");

		if(rft_ids != null)
		{
			for (String rft_id : rft_ids) {
				DSpaceObject obj = handleService.resolveToObject(context, rft_id);
				if (obj != null) {
					httpResponse.sendRedirect(httpResponse
							.encodeRedirectURL(request.getContextPath()
									+ "/handle/" + obj.getHandle()));
					return;
				}
			}
		}
		

		String[] identifiers = request.getParameterValues("rtf.identifier");

		/**
		 * Next attempt to resolve an identifier in search
		 */
		if(identifiers != null)
		{
			for (String identifier : identifiers) {
				String handle = getFirstHandle("identifier: " + identifier);
				if (handle != null) {
					httpResponse.sendRedirect(httpResponse
							.encodeRedirectURL(request.getContextPath()
									+ "/handle/" + handle));
					return;
				}
			}
		}
		

		/**
		 * Otherwise, attempt to full text search for the item
		 */
		StringBuilder queryBuilder = new StringBuilder();

		Enumeration<String> e = request.getParameterNames();

		while (e.hasMoreElements()) {
			String name = e.nextElement();
			if (name.startsWith("rft.")) {
				for (String value : request.getParameterValues(name)) {
					queryBuilder.append(value).append(" ");
				}
			}
		}

        String query = queryBuilder.toString().trim();
		if(query.length() == 0)
		{
			httpResponse.sendError(httpResponse.SC_BAD_REQUEST, "OpenURL Request requires a valid rtf_id, rtf.identifier or other rtf.<dublincore> search fields" );
		}
		
		httpResponse.sendRedirect(httpResponse.encodeRedirectURL(request
				.getContextPath()
				+ "/simple-search?query=" + java.net.URLEncoder.encode(query, request.getCharacterEncoding())));

	}
}
