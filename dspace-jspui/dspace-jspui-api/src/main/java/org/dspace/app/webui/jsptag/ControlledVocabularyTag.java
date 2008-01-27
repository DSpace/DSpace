/*
 * ControlledVocabularyTag.java
 * 
 * Version: $Revision$
 * 
 * Date: $Date$
 * 
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts Institute of
 * Technology. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: -
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of the Hewlett-Packard Company nor
 * the name of the Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.dspace.app.webui.jsptag;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

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
    private static final String CONTROLLEDVOCABULARY_JSPTAG = "/controlledvocabulary/controlledvocabularyTag.jsp";

    // the log
    private static Logger log = Logger.getLogger(ControlledVocabularyTag.class);

    // a tag attribute that contains the words used to trim the vocabulary tree
    private String filter;

    // a tag attribute that activates multiple selection of vocabulary terms
    private boolean allowMultipleSelection;

    // a tag attribute that specifies the vocabulary to be displayed
    private String vocabulary;

    // an hashtable containing all the loaded vocabularies
    public Hashtable controlledVocabularies;

    /**
     * Process tag
     */
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
        controlledVocabularies = (Hashtable) pageContext.getServletContext()
                .getAttribute("controlledvocabulary.controlledVocabularies");
        if (controlledVocabularies == null)
        {
            controlledVocabularies = loadControlledVocabularies(vocabulariesPath);
            pageContext.getServletContext().setAttribute(
                    "controlledvocabulary.controlledVocabularies",
                    controlledVocabularies);
        }

        try
        {
            Hashtable prunnedVocabularies = needsFiltering() ? filterVocabularies(
                    controlledVocabularies, vocabularyPrunningXSLT)
                    : controlledVocabularies;

            String html = "";
            if (vocabulary != null && !vocabulary.equals(""))
            {
                html = renderVocabularyAsHTML((Document) prunnedVocabularies
                        .get(vocabulary + ".xml"),
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
    private String renderVocabulariesAsHTML(Hashtable vocabularies,
            String xslt, boolean allowMultipleSelection, String contextPath)
    {
        String result = "";
        Iterator iter = vocabularies.values().iterator();
        while (iter.hasNext())
        {
            Document controlledVocabularyXML = (Document) iter.next();
            result += renderVocabularyAsHTML(controlledVocabularyXML, xslt,
                    allowMultipleSelection, contextPath);
        }
        return result;
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
    private Hashtable filterVocabularies(Hashtable vocabularies,
            String vocabularyPrunningXSLT)
    {
        Hashtable prunnedVocabularies = new Hashtable();
        Enumeration enumeration = vocabularies.keys();
        while (enumeration.hasMoreElements())
        {
            String controlledVocabularyKey = (String) enumeration.nextElement();
            Document controlledVocabulary = (Document) vocabularies
                    .get(controlledVocabularyKey);
            prunnedVocabularies.put(controlledVocabularyKey, filterVocabulary(
                    controlledVocabulary, vocabularyPrunningXSLT, getFilter()));
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
            return "";

        String result = "";
        try
        {

            Hashtable parameters = new Hashtable();
            parameters.put("allowMultipleSelection",
                    allowMultipleSelection ? "yes" : "no");
            parameters.put("contextPath", contextPath);
            result = XMLUtil.transformDocumentAsString(vocabulary, parameters,
                    controlledVocabulary2HtmlXSLT);
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
    public Document filterVocabulary(Document vocabulary,
            String vocabularyPrunningXSLT, String filter)
    {
        if (vocabulary == null)
            return null;

        try
        {
            Hashtable parameters = new Hashtable();
            parameters.put("filter", filter);
            Document prunnedVocabulary = XMLUtil.transformDocument(vocabulary,
                    parameters, vocabularyPrunningXSLT);
            return prunnedVocabulary;
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
    private static Hashtable loadControlledVocabularies(String directory)
    {
        Hashtable controlledVocabularies = new Hashtable();
        File dir = new File(directory);

        FilenameFilter filter = new FilenameFilter()
        {
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
                    Document controlledVocabulary = XMLUtil.loadXML(directory
                            + filename);
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
