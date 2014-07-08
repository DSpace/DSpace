/**
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree and available
 * online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.io.IOException;
import java.io.StringWriter;
import org.apache.log4j.Logger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.SiteHomeProcessor;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.Verifier;
import org.jdom.output.XMLOutputter;

/**
 * This class add sets home metadata tags the request attributes to use in the
 * site home page implementing the SiteHomeProcessor.
 *
 *
 * @author David Andrés Maznzano Herrera <damanzano>
 */
public class HomeMetadataProcessor implements SiteHomeProcessor {

    /**
     * log4j logger
     */
    private static Logger log = Logger
            .getLogger(HomeMetadataProcessor.class);

    private static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    /**
     * blank constructor - does nothing.
     *
     */
    public HomeMetadataProcessor() {

    }

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response) throws PluginException,
            AuthorizeException {

        // Get the homepage metadata content from configuration files
        String creatorsString="";
        String descriptionString = "";
        String keywordsString = "";
        String subjectsString = "";
        String subSpaString = "";
        String contributorsString = "";

        creatorsString = ConfigurationManager.getProperty("home-metadata", "webui.homepage.creators");
        descriptionString = ConfigurationManager.getProperty("home-metadata", "webui.homepage.description");
        keywordsString = ConfigurationManager.getProperty("home-metadata", "webui.homepage.keywords");
        subjectsString = ConfigurationManager.getProperty("home-metadata", "webui.homepage.subjects");
        subSpaString = ConfigurationManager.getProperty("home-metadata", "webui.homepage.subjects.spa");
        contributorsString = ConfigurationManager.getProperty("home-metadata", "webui.homepage.collaborators");

        String[] keywords = keywordsString == null ? new String[0] : keywordsString.split("\\s*,\\s*");
        log.debug("keywors: " + keywords.length);
        String[] subjects = subjectsString == null ? new String[0] : subjectsString.split("\\s*,\\s*");
        log.debug("subjects: " + subjects.length);
        String[] subjectsSpa = subSpaString == null ? new String[0] : subSpaString.split("\\s*,\\s*");
        log.debug("subjects: " + subjects.length);
        String[] contributors = contributorsString == null ? new String[0] : contributorsString.split("\\s*,\\s*");
        log.debug("keywors: " + contributors.length);

        List<Element> metas = new ArrayList<Element>();
        String headMetadata = "";
        
        // Process creators
        Element c = new Element("meta", XHTML_NAMESPACE);
        c.setAttribute("name", "dc.creator");
        if (creatorsString != null && !creatorsString.equals("")) {
            String reason = Verifier.checkCharacterData(creatorsString);
            if (reason == null) {
                c.setAttribute("content", creatorsString);
            }
            metas.add(c);
        }
        
        //Process key words
        Element k = new Element("meta", XHTML_NAMESPACE);
        k.setAttribute("name", "keywords");
        if (keywordsString != null && !keywordsString.equals("")) {
            String reason = Verifier.checkCharacterData(keywordsString);
            if (reason == null) {
                k.setAttribute("content", keywordsString);
            }
            metas.add(k);
        }
        
        // Process Description
        Element d = new Element("meta", XHTML_NAMESPACE);
        d.setAttribute("name", "description");
        if (descriptionString != null && !descriptionString.equals("")) {
            String reason = Verifier.checkCharacterData(descriptionString);
            if (reason == null) {
                d.setAttribute("content", descriptionString);
            }
            metas.add(d);
        }

        // Process subjects
        for (String subject : subjects) {
            Element e = new Element("meta", XHTML_NAMESPACE);
            e.setAttribute("name", "dc.subject");
            if (subject != null && !subject.equals("")) {
                String reason = Verifier.checkCharacterData(subject);
                if (reason == null) {
                    e.setAttribute("content", subject);
                }
                e.setAttribute("lang", "eng", Namespace.XML_NAMESPACE);
                metas.add(e);
            }
        }

        // Process subjects Spa
        for (String subject : subjectsSpa) {
            Element e = new Element("meta", XHTML_NAMESPACE);
            e.setAttribute("name", "dc.subject");
            if (subject != null && !subject.equals("")) {
                String reason = Verifier.checkCharacterData(subject);
                if (reason == null) {
                    e.setAttribute("content", subject);
                }
                e.setAttribute("lang", "spa", Namespace.XML_NAMESPACE);
                metas.add(e);
            }
        }

        // Process contributors
        for (String contributor : contributors) {
            Element e = new Element("meta", XHTML_NAMESPACE);
            e.setAttribute("name", "dc.contributor");
            if (contributor != null && !contributor.equals("")) {
                String reason = Verifier.checkCharacterData(contributor);
                if (reason == null) {
                    e.setAttribute("content", contributor);
                }
                metas.add(e);
            }
        }

        try {
            StringWriter sw = new StringWriter();
            XMLOutputter xmlo = new XMLOutputter();
            xmlo.output(new Text("\n"), sw);

            for (Element e : metas) {
                // FIXME: we unset the Namespace so it's not printed.
                // This is fairly yucky, but means the same crosswalk should
                // work for Manakin as well as the JSP-based UI.
                e.setNamespace(null);
                xmlo.output(e, sw);
                xmlo.output(new Text("\n"), sw);
            }

            headMetadata = sw.toString();
        } catch (IOException e2) {
            throw new PluginException(e2.getMessage(), e2);
        }
        request.setAttribute("dspace.layout.head", headMetadata);
        //request.setAttribute("communities", communities);
    }

}
