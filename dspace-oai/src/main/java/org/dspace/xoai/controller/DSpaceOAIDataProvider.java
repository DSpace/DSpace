/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.controller;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static java.util.Arrays.asList;
import static org.apache.logging.log4j.LogManager.getLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.lyncode.xoai.dataprovider.OAIDataProvider;
import com.lyncode.xoai.dataprovider.OAIRequestParameters;
import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.exceptions.InvalidContextException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.exceptions.WritingXmlException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
@Controller
// Use the configured "oai.path" for all requests, or "/oai" by default
@RequestMapping("/${oai.path:oai}")
// Only enable this controller if "oai.enabled=true"
@ConditionalOnProperty("oai.enabled")
public class DSpaceOAIDataProvider {
    private static final Logger log = getLogger(DSpaceOAIDataProvider.class);

    private Transformer htmlTransformer = null;

    @Autowired
    XOAICacheService cacheService;
    @Autowired
    ContextService contextService;
    @Autowired
    XOAIManagerResolver xoaiManagerResolver;
    @Autowired
    ItemRepositoryResolver itemRepositoryResolver;
    @Autowired
    IdentifyResolver identifyResolver;
    @Autowired
    SetRepositoryResolver setRepositoryResolver;

    private DSpaceResumptionTokenFormatter resumptionTokenFormat = new DSpaceResumptionTokenFormatter();

    @PostConstruct
    public void setUpHTMLTransformer() {
        try {
            XOAIManager manager = xoaiManagerResolver.getManager();
            if (manager.hasStyleSheet()) {
                ResourceLoader resourceLoader = new DefaultResourceLoader();
                String styleSheetPath = manager.getStyleSheet();
                Resource styleSheetResource = resourceLoader.getResource("classpath:" + styleSheetPath);
                Source htmlTransformSource
                    = new StreamSource(new ByteArrayInputStream(styleSheetResource.getContentAsByteArray()));
                TransformerFactory transformerFactory
                    = TransformerFactory.newInstance();
                htmlTransformer = transformerFactory.newTransformer(htmlTransformSource);
            }
        } catch (Exception e) {
            log.warn("Could not set up HTML transformer for OAI-PMH app: " + e.toString());
        }
    }

    @RequestMapping("")
    public void index(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.sendRedirect(request.getRequestURI() + "/");
    }

    @RequestMapping({"/"})
    public String indexAction(HttpServletResponse response, Model model) throws ServletException {
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
    public String contextAction(Model model, HttpServletRequest request, HttpServletResponse response,
            @PathVariable("context") String xoaiContext)
            throws IOException, ServletException, TransformerException {
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

            boolean shouldServeAsHTML = false;
            List<MediaType> acceptMediaTypes = MediaType.parseMediaTypes(request.getHeader("Accept"));
            if (htmlTransformer != null) {
                response.addHeader("Vary", "Accept");
                for (MediaType acceptMediaType : acceptMediaTypes) {
                    if (acceptMediaType.includes(MediaType.TEXT_XML) ||
                            acceptMediaType.includes(MediaType.APPLICATION_XML)) {
                        break;
                    } else if (acceptMediaType.includes(MediaType.TEXT_HTML)) {
                        shouldServeAsHTML = true;
                    }
                }
            }

            if (shouldServeAsHTML) {
                response.setContentType("text/html");
                out = new ByteArrayOutputStream();
            } else {
                response.setContentType("text/xml");
            }
            response.setCharacterEncoding("UTF-8");

            String identification = xoaiContext + parameters.requestID();

            if (cacheService.isActive()) {
                if (!cacheService.hasCache(identification)) {
                    cacheService.store(identification, dataProvider.handle(parameters));
                }

                cacheService.handle(identification, out);
            } else {
                dataProvider.handle(parameters, out);
            }

            if (shouldServeAsHTML) {
                OutputStream responseOut = response.getOutputStream();
                Source source = new StreamSource(new ByteArrayInputStream(((ByteArrayOutputStream) out).toByteArray()));
                Result result = new StreamResult(responseOut);
                htmlTransformer.transform(source, result);
                out = responseOut;
            }

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
            throw new ServletException("OAI 2.0 wasn't correctly initialized, please check the log for previous errors",
                                       e);
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
        if (context != null && context.isValid()) {
            context.abort();
        }
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
