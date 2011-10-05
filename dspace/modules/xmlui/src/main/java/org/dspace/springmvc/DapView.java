package org.dspace.springmvc;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.PluginManager;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.Map;


import org.jdom.output.Format;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/20/11
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */

public class DapView implements View {

	private static final Logger LOGGER = LoggerFactory.getLogger(DapView.class);

    public static final String DRYAD_CROSSWALK = "DRYAD-V3";

    public static final String DC_TERMS_NAMESPACE = "http://purl.org/dc/terms/";

    public String getContentType() {

        return "application/xml";
    }

    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {


        DSpaceObject item = (DSpaceObject)request.getAttribute(ResourceIdentifierController.DSPACE_OBJECT);

        DisseminationCrosswalk xWalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(DisseminationCrosswalk.class,DRYAD_CROSSWALK);

        try {
            if (!xWalk.canDisseminate(item)) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("xWalk says item cannot be disseminated: " + item.getHandle());
                }
            }

            Element result = xWalk.disseminateElement(item);
            Namespace dcTermsNS = Namespace.getNamespace(DC_TERMS_NAMESPACE);
            Namespace dryadNS = result.getNamespace();
            Element file = result.getChild("DryadDataFile", dryadNS);
            Element idElem;

            if (file != null) {
                result = file;
            }

            idElem = result.getChild("identifier", dcTermsNS);

            // add the MN identifier suffix for metadata records
            if (idElem != null) {
                idElem.setText(idElem.getText() + "/dap");
            }

            OutputStream aOutputStream = response.getOutputStream();

            new XMLOutputter(Format.getPrettyFormat()).output(result, aOutputStream);

            aOutputStream.close();

        } catch (AuthorizeException details) {
            // We've disabled authorization for this context
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Shouldn't see this exception!");
            }
        } catch (CrosswalkException details) {
            LOGGER.error(details.getMessage(), details);

            // programming error
            throw new RuntimeException(details);
        }
    }
}
