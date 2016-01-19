/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.XMLUtil;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;

/**
 * A Tag to load and display controlled vocabularies
 * 
 * @author Miguel Ferreira
 * @version $Revision$
 * 
 */
public class ControlledVocabularyTag extends TagSupport
{
    // path to the jsp that outputs the results of this tag
    private static final String CONTROLLEDVOCABULARY_JSPTAG
            = "/controlledvocabulary/controlledvocabularyTag.jsp";

    // the log
    private static final Logger log = Logger.getLogger(ControlledVocabularyTag.class);

    // a tag attribute that contains the words used to trim the vocabulary tree
    private String filter;

    // a tag attribute that activates multiple selection of vocabulary terms
    private boolean allowMultipleSelection;

    // a tag attribute that specifies the vocabulary to be displayed
    private String vocabulary;

    /**
     * Process tag
     */
    @Override
    public int doStartTag() throws JspException
    {
        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        String vocabulariesPath = ConfigurationManager
                .getProperty("dspace.dir")
                + "/config/controlled-vocabularies/";
        String addonBaseDirectory = pageContext.getServletContext()
                .getRealPath("")
                + "/controlledvocabulary/";
        String vocabularyPrunningXSLT = addonBaseDirectory
                + "vocabularyprune.xsl";
        String controlledVocabulary2HtmlXSLT = addonBaseDirectory
                + "vocabulary2html.xsl";

        // Load vocabularies on startup
        Map<String, Document> controlledVocabularies
                = (Map<String, Document>) pageContext.getServletContext()
                        .getAttribute("controlledvocabulary.controlledVocabularies");
        if (controlledVocabularies == null)
        {
            controlledVocabularies = loadControlledVocabularies(vocabulariesPath);
            pageContext.getServletContext().setAttribute("controlledvocabulary.controlledVocabularies", controlledVocabularies);
        }

        try
        {
            Map<String, Document> prunnedVocabularies = needsFiltering() ?
                    filterVocabularies(controlledVocabularies, vocabularyPrunningXSLT)
                    : controlledVocabularies;

            String html = "";
            if (vocabulary != null && !vocabulary.equals(""))
            {
                html = renderVocabularyAsHTML(prunnedVocabularies.get(vocabulary + ".xml"),
                        controlledVocabulary2HtmlXSLT,
                        isAllowMultipleSelection(), request.getContextPath());
            }
            else
            {
                html = renderVocabulariesAsHTML(prunnedVocabularies,
                        controlledVocabulary2HtmlXSLT,
                        isAllowMultipleSelection(), request.getContextPath());
            }
            request.getSession().setAttribute(
                    "controlledvocabulary.vocabularyHTML", html);

            pageContext.include(CONTROLLEDVOCABULARY_JSPTAG);

        }
        catch (Exception e)
        {
            log.warn("Exception", e);
        }

        return SKIP_BODY;
    }

    /**
     * End processing tag
     */
    @Override
    public int doEndTag()
    {
        return EVAL_PAGE;
    }

    /**
     * Do we gave a filter to apply to the controlled vocabularies?
     * 
     * @return true if a filter was provided.
     */
    private boolean needsFiltering()
    {
        return getFilter() != null && getFilter().length() > 0;
    }

    /**
     * Converts a XML Vocabulary to a HTML tree
     * 
     * @param vocabularies
     *            A hashtable with all the XML taxonomies/vocabularies loaded as
     *            values
     * @param xslt
     *            the filename of the stylesheet to apply the XML taxonomies
     * @param allowMultipleSelection
     *            include checkboxes next to the taxonomy terms
     * @param contextPath
     *            The context path
     * @return the HTML that represents the vocabularies
     */
    private String renderVocabulariesAsHTML(Map<String, Document> vocabularies,
            String xslt, boolean allowMultipleSelection, String contextPath)
    {
        StringBuilder result = new StringBuilder();
        Iterator<Document> iter = vocabularies.values().iterator();
        while (iter.hasNext())
        {
            Document controlledVocabularyXML = iter.next();
            result.append(renderVocabularyAsHTML(controlledVocabularyXML, xslt,
                    allowMultipleSelection, contextPath));
        }
        return result.toString();
    }

    /**
     * Applies a filter to the vocabularies, i.e. it prunes the trees by
     * removing all the branches that do not contain the words in the filter.
     * 
     * @param vocabularies
     *            A hashtable with all the XML taxonomies/vocabularies loaded as
     *            values
     * @param vocabularyPrunningXSLT
     *            the filename of the stylesheet that trimms the taxonomies
     * @return An hashtable with all the filtered vocabularies
     */
    private Map<String, Document> filterVocabularies(Map<String, Document> vocabularies, String vocabularyPrunningXSLT)
    {
        Map<String, Document> prunnedVocabularies = new HashMap<>();
        for (Map.Entry<String, Document> entry : vocabularies.entrySet())
        {
            prunnedVocabularies.put(entry.getKey(), filterVocabulary(entry.getValue(), vocabularyPrunningXSLT, getFilter()));
        }
        return prunnedVocabularies;
    }

    /**
     * Renders a taxonomy as HTML by applying a stylesheet.
     * 
     * @param vocabulary
     *            The XML document representing a taxonomy
     * @param controlledVocabulary2HtmlXSLT
     *            The filename of the stylesheet that converts the taxonomy to
     *            HTML
     * @param allowMultipleSelection
     *            include checkboxes next to the taxonomy terms
     * @param contextPath
     *            The context path
     * @return the provided taxonomy as HTML.
     */
    public String renderVocabularyAsHTML(Document vocabulary,
            String controlledVocabulary2HtmlXSLT,
            boolean allowMultipleSelection, String contextPath)
    {
        if (vocabulary == null)
        {
            return "";
        }

        String result = "";
        try
        {

            Map<String, String> parameters = new HashMap<>();
            parameters.put("allowMultipleSelection", allowMultipleSelection ? "yes" : "no");
            parameters.put("contextPath", contextPath);
            result = XMLUtil.transformDocumentAsString(vocabulary, parameters, controlledVocabulary2HtmlXSLT);
        }
        catch (Exception e)
        {
            log.error("Error rendering HTML", e);
        }
        return result;
    }

    /**
     * Applies a filter to the provided vocabulary, i.e. it prunes the tree by
     * removing all the branches that do not contain the words in the filter.
     * 
     * @param vocabulary
     *            The vocabulary to be trimmed
     * @param vocabularyPrunningXSLT
     *            The filename of the stylesheet that trims the vocabulary
     * @param filter
     *            The filter to be applied
     * @return The trimmed vocabulary.
     */
    public Document filterVocabulary(Document vocabulary, String vocabularyPrunningXSLT, String filter)
    {
        if (vocabulary == null)
        {
            return null;
        }

        try
        {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("filter", filter);
            return XMLUtil.transformDocument(vocabulary, parameters, vocabularyPrunningXSLT);
        }
        catch (Exception e)
        {
            log.error("Error filtering vocabulary", e);
            return null;
        }

    }

    /**
     * Loads into memory all the vocabularies found in the given directory. All
     * files with .xml extension are considered to be controlled vocabularies.
     * 
     * @param directory
     *            where the files are positioned
     * @return an hashtable with the filenames of the vocabularies as keys and
     *         the XML documents representing the vocabularies as values.
     */
    private static Map<String, Document> loadControlledVocabularies(String directory)
    {
        Map<String, Document> controlledVocabularies = new HashMap<>();
        File dir = new File(directory);

        FilenameFilter filter = new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".xml");
            }
        };
        String[] children = dir.list(filter);

        if (children != null && children.length > 0)
        {
            for (int i = 0; i < children.length; i++)
            {
                String filename = children[i];

                try
                {
                    Document controlledVocabulary = XMLUtil.loadXML(directory + filename);
                    controlledVocabularies.put(filename, controlledVocabulary);
                    log.warn("Loaded vocabulary: " + filename);
                }
                catch (Exception e)
                {
                    log.warn("Failed to load vocabulary from " + filename, e);
                }
            }
        }
        else
        {
            log.warn("Could not find any vocabularies...");
        }
        return controlledVocabularies;

    }

    /**
     * Gets the filter provided as parameter to the tag
     * 
     * @return the filter
     */
    public String getFilter()
    {
        return filter;
    }

    /**
     * Sets the filter
     * 
     * @param filter
     *            the filter
     */
    public void setFilter(String filter)
    {
        this.filter = filter;
    }

    /**
     * Returns the value of the multiple selection parameter
     * 
     * @return true if the multiple selection was selected
     */
    public boolean isAllowMultipleSelection()
    {
        return allowMultipleSelection;
    }

    /**
     * Defines if we want to be able to select multiple terms of the taxonomy
     * 
     * @param allowMultipleSelection
     *            true if we want to be able to select more than on term
     */
    public void setAllowMultipleSelection(boolean allowMultipleSelection)
    {
        this.allowMultipleSelection = allowMultipleSelection;
    }

    /**
     * Gets the name of the vocabulary to be displayed
     * 
     * @return the name of the vocabulary
     */
    public String getVocabulary()
    {
        return vocabulary;
    }

    /**
     * Sets the name of the vocabulary to be displayed. If no name is provided,
     * all vocabularies loaded will be rendered to the output
     * 
     * @param vocabulary
     *            the name of the vocabulary to be selected
     */
    public void setVocabulary(String vocabulary)
    {
        this.vocabulary = vocabulary;
    }

}
