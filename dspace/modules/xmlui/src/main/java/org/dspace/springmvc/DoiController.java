package org.dspace.springmvc;


import org.dspace.doi.DOI;
import org.dspace.doi.Minter;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(DoiController.class);

    @RequestMapping("/doi")
    public void doiLookup(HttpServletRequest request, HttpServletResponse response) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            Minter myMinter = new Minter();
            String doiID = request.getParameter("lookup");
            DOI doi = myMinter.getKnownDOI(doiID);
            if(doi != null){

                // return handle: http://datadryad.org/handle/10255/dryad.20
                //writer.println(doi.getInternalIdentifier().toString());

                // return resource: http://localhost:8100/resource/doi:10.5061/dryad.20
                writer.println(doi.getTargetURL().toString());
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


    @RequestMapping("/doi/**")
    public void doiLookup_(HttpServletRequest request, HttpServletResponse response) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            Minter myMinter = new Minter();
            String doiID = request.getParameter("lookup");
            DOI doi = myMinter.getKnownDOI(doiID);
            if(doi != null){

                // return handle: http://datadryad.org/handle/10255/dryad.20
                //writer.println(doi.getInternalIdentifier().toString());

                // return resource: http://localhost:8100/resource/doi:10.5061/dryad.20
                writer.println(doi.getTargetURL().toString());
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
