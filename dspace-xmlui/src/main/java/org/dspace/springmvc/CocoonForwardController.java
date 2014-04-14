/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.springmvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.*;


/**
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */

@Controller(value = "cocoonForwardController")
public class CocoonForwardController {

    private static final Logger log = LoggerFactory.getLogger(CocoonForwardController.class);

    @RequestMapping
    public ModelAndView forwardRequest(HttpServletRequest request, HttpServletResponse response) throws SQLException {
        log.debug("CocoonForwardController!!!!!");
        return new ModelAndView(new CocoonView());
    }

}
