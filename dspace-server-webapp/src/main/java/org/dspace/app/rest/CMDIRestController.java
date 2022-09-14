/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/* Created for LINDAT/CLARIAH-CZ (UFAL) */

package org.dspace.app.rest;

import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import com.hp.hpl.jena.rdf.model.Model;
import com.lyncode.xoai.dataprovider.OAIDataProvider;
import com.lyncode.xoai.dataprovider.OAIRequestParameters;
import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.exceptions.InvalidContextException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.oaipmh.OAIPMH;
import org.dspace.core.Context;
import org.dspace.xoai.services.api.config.XOAIManagerResolver;
import org.dspace.xoai.services.api.config.XOAIManagerResolverException;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.xoai.IdentifyResolver;
import org.dspace.xoai.services.api.xoai.ItemRepositoryResolver;
import org.dspace.xoai.services.api.xoai.SetRepositoryResolver;
import org.dspace.xoai.services.impl.xoai.DSpaceResumptionTokenFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creates endpoint /cmdi/oai-metadata that returns metadata in cmdi format.
 */
@RestController
@RequestMapping("/cmdi")
public class CMDIRestController {

    @Autowired
    private ContextService contextService;

    @Autowired
    private XOAIManagerResolver xoaiManagerResolver;

    @Autowired
    private IdentifyResolver identifyResolver;

    @Autowired
    private SetRepositoryResolver setRepositoryResolver;

    @Autowired
    private ItemRepositoryResolver itemRepositoryResolver;

    private final DSpaceResumptionTokenFormatter resumptionTokenFormat = new DSpaceResumptionTokenFormatter();

    /**
     * This is not an oai endpoint. It's here only to expose the cmdi metadata
     */
    @RequestMapping("/oai-metadata")
    public String getCmdiMetadata(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        Context context = null;
        try {
            request.setCharacterEncoding("UTF-8");
            context = contextService.getContext();

            XOAIManager manager = xoaiManagerResolver.getManager();

            OAIDataProvider dataProvider = new OAIDataProvider(manager, "request",
                    identifyResolver.getIdentify(),
                    setRepositoryResolver.getSetRepository(),
                    itemRepositoryResolver.getItemRepository(),
                    resumptionTokenFormat);

            OutputStream out = response.getOutputStream();

            // adding some defaults for /cite requests this will make the URL simple
            // only handle and metadataPrefix will be required
            Map<String, List<String>> parameterMap = buildParametersMap(request);
            if (!parameterMap.containsKey("verb")) {
                parameterMap.put("verb", asList("GetRecord"));
            }
            if (!parameterMap.containsKey("metadataPrefix")) {
                parameterMap.put("metadataPrefix", asList("cmdi"));
            } else {
                List<String> mp = parameterMap.get("metadataPrefix");
                List<String> lcMP = new ArrayList<String>();
                for (String m : mp) {
                    lcMP.add(m.toLowerCase());
                }
                parameterMap.put("metadataPrefix", lcMP);
            }
            if (!parameterMap.containsKey("identifier")) {
                parameterMap.put("identifier", asList("oai:" + request.getServerName() + ":" + request
                        .getParameter("handle")));
                parameterMap.remove("handle");
            }
            /////////////////////////////////////////////////////////////////////////

            OAIRequestParameters parameters = new OAIRequestParameters(parameterMap);

            response.setContentType("application/xml");

            OAIPMH oaipmh = dataProvider.handle(parameters);

            XmlOutputContext xmlOutContext = XmlOutputContext.emptyContext(out);
            xmlOutContext.getWriter().writeStartDocument();

            //Try to obtain just the metadata, if that fails return "normal" response
            try {
                oaipmh.getInfo().getGetRecord().getRecord().getMetadata().write(xmlOutContext);
            } catch (Exception e) {
                oaipmh.write(xmlOutContext);
            }

            xmlOutContext.getWriter().writeEndDocument();
            xmlOutContext.getWriter().flush();
            xmlOutContext.getWriter().close();

            out.flush();
            out.close();
        } catch (InvalidContextException e) {
            return indexAction(response, model);
        } catch (ContextServiceException | WritingXmlException | XMLStreamException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error while writing the output. For more information visit the log files.");
        } catch (XOAIManagerResolverException e) {
            throw new ServletException("OAI 2.0 wasn't correctly initialized," +
                    " please check the log for previous errors", e);
        } catch (OAIException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error. For more information visit the log files.");
        } finally {
            closeContext(context);
        }

        return null; // response without content
    }

    private String indexAction(HttpServletResponse response, Model model) {

        return null;
    }

    private void closeContext(Context context) {
        if (Objects.nonNull(context) && context.isValid()) {
            context.abort();
        }
    }

    private Map<String, List<String>> buildParametersMap(HttpServletRequest request) {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String[] values = request.getParameterValues(name);
            map.put(name, asList(values));
        }
        return map;
    }

    /**
     * Returns contexts available
     * @throws ServletException if it can't load XOAI manager
     */
    @RequestMapping("/")
    public String indexAction(HttpServletResponse response, org.springframework.ui.Model model)
            throws ServletException {
        try {
            XOAIManager manager = xoaiManagerResolver.getManager();
            model.addAttribute("contexts", manager.getContextManager().getContexts());
            response.setStatus(SC_BAD_REQUEST);
        } catch (XOAIManagerResolverException e) {
            throw new ServletException("Unable to load XOAI manager, please, try again.", e);
            // No message
        }
        return "index";
    }
}
