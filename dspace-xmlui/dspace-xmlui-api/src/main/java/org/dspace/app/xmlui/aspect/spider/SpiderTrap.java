/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.spider;

import java.io.*;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.util.IPTable;
import org.dspace.statistics.util.SpiderDetector;
import org.xml.sax.SAXException;


/**
 * Create a simple contact us page. Fancier contact us pages should be handled by the theme.
 *
 * @author Mini Pillai
 */
public class SpiderTrap extends AbstractDSpaceTransformer {

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_title = message("xmlui.ArtifactBrowser.Spider.title");
    private static final Message T_feedback_label =
            message("xmlui.ArtifactBrowser.Contact.feedback_label");

    private static final Message T_feedback_link =
            message("xmlui.ArtifactBrowser.Contact.feedback_link");

    private static IPTable table = null;
    private static Logger log = Logger.getLogger(SpiderTrap.class);


    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException {

        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent("");

    }


    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);
        Division spider = body.addDivision("contact", "primary");
        List list = spider.addList("contact");
        String content = generateRandomContent();
        list.addItem(content);

        String comment = "";
        if(request.getHeader("User-Agent") != null)
        {
            comment += request.getHeader("User-Agent");
        }

        if (SolrLogger.isUseProxies() && request.getHeader("X-Forwarded-For") != null) {
            /* This header is a comma delimited list */
            for (String xfip : request.getHeader("X-Forwarded-For").split(",")) {
                if (SpiderDetector.isSpider(xfip) || isSpider(xfip)) {
                    log.info("This ip already exists");
                } else {
                    //this is a new IP address, add it to the IPTable table variable.
                    addToFile(xfip, comment);
                }
            }
        }
        else
        {
            if (SpiderDetector.isSpider(request.getRemoteAddr()) || isSpider(request.getRemoteAddr())) {
                log.info("This ip already exists");
            }
            else {
                addToFile(request.getRemoteAddr(), comment);
            }
        }
    }

    public void addToFile(String ip, String comment) {

        if (table == null) {
            table = new IPTable();
        }

        String filePath = ConfigurationManager.getProperty("dspace.dir");

        try {
            File spidersFile = new File(filePath, "config/spiders/spidertrap-misc.txt");

            if (!spidersFile.exists()) {
                spidersFile.createNewFile();
                log.info("Spider file created");

            }
            writeToFile(spidersFile, ip, comment);
            table.add(ip);
            SpiderDetector.addSpiderIpAddress(ip);
            log.info("Loaded Spider IP file: " + spidersFile);


        } catch (Exception e) {
            log.error("Error Loading Spiders:" + e.getMessage(), e);
        }
    }

    public void writeToFile(File file, String ip, String comment) {
        try {
            FileWriter fstream = new FileWriter(file,true);
            if (comment != null && !comment.isEmpty()) {
                fstream.write("# " + comment + "\n");
            }
            fstream.write(ip);
            fstream.write("\n");
            fstream.close();
        } catch (Exception e) {
            log.error("Error: " + e.getMessage());
        }

    }

    public String generateRandomContent()
    {
        SecureRandom random = new SecureRandom();
        String str = new BigInteger(130, random).toString(32);
        return str;
    }





    /**
     * Check individual IP is a spider.
     *
     * @param ip String
     * @return if is spider IP
     */
    public static boolean isSpider(String ip) {

        if (table == null) {
            return false;
        }

        try {
            if (table.contains(ip)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;


    }
}
