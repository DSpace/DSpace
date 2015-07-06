/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.controller;

import com.lyncode.xoai.dataprovider.OAIDataProvider;
import com.lyncode.xoai.dataprovider.OAIRequestParameters;
import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.exceptions.InvalidContextException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.xoai.services.api.cache.XOAICacheService;
import org.dspace.xoai.services.api.config.XOAIManagerResolver;
import org.dspace.xoai.services.api.config.XOAIManagerResolverException;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.xoai.IdentifyResolver;
import org.dspace.xoai.services.api.xoai.ItemRepositoryResolver;
import org.dspace.xoai.services.api.xoai.SetRepositoryResolver;
import org.dspace.xoai.services.impl.xoai.DSpaceResumptionTokenFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.log4j.Logger.getLogger;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@Controller
public class DSpaceOAIDataProvider
{
    private static final Logger log = getLogger(DSpaceOAIDataProvider.class);

    @Autowired XOAICacheService cacheService;
    @Autowired ContextService contextService;
    @Autowired XOAIManagerResolver xoaiManagerResolver;
    @Autowired ItemRepositoryResolver itemRepositoryResolver;
    @Autowired IdentifyResolver identifyResolver;
    @Autowired SetRepositoryResolver setRepositoryResolver;

    private DSpaceResumptionTokenFormatter resumptionTokenFormat = new DSpaceResumptionTokenFormatter();

    @RequestMapping("/")
    public String indexAction (HttpServletResponse response, Model model) throws ServletException {
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

    @RequestMapping("/{context}")
    public String contextAction (Model model, HttpServletRequest request, HttpServletResponse response, @PathVariable("context") String xoaiContext) throws IOException, ServletException {
        Context context = null;
        try {
            request.setCharacterEncoding("UTF-8");
            context = contextService.getContext();

            XOAIManager manager = xoaiManagerResolver.getManager();

            OAIDataProvider dataProvider = new OAIDataProvider(manager, xoaiContext,
                    identifyResolver.getIdentify(),
                    setRepositoryResolver.getSetRepository(),
                    itemRepositoryResolver.getItemRepository(),
                    resumptionTokenFormat);

            OutputStream out = response.getOutputStream();
            OAIRequestParameters parameters = new OAIRequestParameters(buildParametersMap(request));

            response.setContentType("application/xml");
            response.setCharacterEncoding("UTF-8");

            String identification = xoaiContext + parameters.requestID();

            if (cacheService.isActive()) {
                if (!cacheService.hasCache(identification))
                    cacheService.store(identification, dataProvider.handle(parameters));

                cacheService.handle(identification, out);
            } else dataProvider.handle(parameters, out);


            out.flush();
            out.close();

            closeContext(context);

        } catch (InvalidContextException e) {
            log.debug(e.getMessage(), e);
            return indexAction(response, model);
        } catch (ContextServiceException e) {
            log.error(e.getMessage(), e);
            closeContext(context);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error while writing the output. For more information visit the log files.");
        } catch (XOAIManagerResolverException e) {
            throw new ServletException("OAI 2.0 wasn't correctly initialized, please check the log for previous errors", e);
        } catch (OAIException e) {
            log.error(e.getMessage(), e);
            closeContext(context);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error. For more information visit the log files.");
        } catch (WritingXmlException e) {
            log.error(e.getMessage(), e);
            closeContext(context);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error while writing the output. For more information visit the log files.");
        } catch (XMLStreamException e) {
            log.error(e.getMessage(), e);
            closeContext(context);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unexpected error while writing the output. For more information visit the log files.");
        } finally {
            closeContext(context);
        }

        return null; // response without content
    }

    private void closeContext(Context context) {
        if (context != null && context.isValid())
            context.abort();
    }

	private Map<String, List<String>> buildParametersMap(
			HttpServletRequest request) {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		Enumeration names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String[] values = request.getParameterValues(name);
			map.put(name, asList(values));
		}
		return map;
	}

}
