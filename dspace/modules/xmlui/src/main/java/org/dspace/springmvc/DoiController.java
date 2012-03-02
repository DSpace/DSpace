package org.dspace.springmvc;


import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.doi.DOI;
import org.dspace.doi.Minter;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;



/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 8/29/11
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */

@Controller
@RequestMapping("/doi")
public class DoiController {

    private static final Logger log = LoggerFactory.getLogger(DoiController.class);

    @RequestMapping("/doi")
    public void doiLookup(HttpServletRequest request, HttpServletResponse response) {
        PrintWriter writer = null;
        try {

            log.warn("Inside Spring Controller!!! ");

            writer = response.getWriter();
            Minter myMinter = new Minter();
            String doiID = request.getParameter("lookup");
            DOI doi = myMinter.getKnownDOI(doiID);
            if(doi != null){

                if(doi.getInternalIdentifier().toString().contains("handle")){
                    //return handle: http://datadryad.org/handle/10255/dryad.20
                    writer.println(doi.getInternalIdentifier().toString());
                }
                else{
                    Context context = new Context();
                    context.turnOffAuthorisationSystem();
                    DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);
                    Item item = (Item)dis.resolve(context, doi.toString());

                    DCValue[] uris = item.getMetadata("dc.identifier.uri");
                    if(uris!=null && uris.length > 0){

                        String canonicalPrefix = ConfigurationManager.getProperty("handle.canonical.prefix");
                        String dryadURL = ConfigurationManager.getProperty("dryad.url");

                        String uri = uris[0].value;

                        // dryad.url = http://datadryad.org
                        // handle.canonical.prefix = http://hdl.handle.net/
                        uri = uri.replace(canonicalPrefix, dryadURL + "/handle/");
                        writer.println(uri);
                    }
                    else{
                        writer.println("DOI not present.");
                    }
                }
                // return resource: http://localhost:8100/resource/doi:10.5061/dryad.20
                // writer.println(doi.getTargetURL().toString());








            }
            else{
                writer.println("DOI not present.");
            }

        } catch (Exception e) {
                writer.println("DOI not present.");
        }finally{
            writer.close();
        }

    }



}
