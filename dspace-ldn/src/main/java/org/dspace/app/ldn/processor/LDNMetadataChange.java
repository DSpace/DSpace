/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import static org.dspace.app.ldn.LDNMetadataFields.ELEMENT;
import static org.dspace.app.ldn.LDNMetadataFields.SCHEMA;
import static org.dspace.content.Item.ANY;

import java.io.StringWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base instructions for metadata change during notification processing.
 * 
 * @author William Welling
 * @author Stefano Maffei (4Science.com)
 * 
 */
public abstract class LDNMetadataChange {

    private String schema;

    private String element;

    private String language;

    // velocity template with notification as its context
    private String conditionTemplate;

    protected final static Logger log = LogManager.getLogger(LDNMetadataChange.class);

    @Autowired
    protected ItemService itemService;

    /**
     * Default coar schema, notify element, any language, and true condition to
     * apply metadata change.
     */
    public LDNMetadataChange() {
        schema = SCHEMA;
        element = ELEMENT;
        language = ANY;
        conditionTemplate = "true";
    }

    /**
     * @return String
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * @return String
     */
    public String getElement() {
        return element;
    }

    /**
     * @param element
     */
    public void setElement(String element) {
        this.element = element;
    }

    /**
     * @return String
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return String
     */
    public String getConditionTemplate() {
        return conditionTemplate;
    }

    /**
     * @param conditionTemplate
     */
    public void setConditionTemplate(String conditionTemplate) {
        this.conditionTemplate = conditionTemplate;
    }

    /**
     * Render velocity template with provided context.
     *
     * @param velocityContext Velocity Context
     * @param velocityEngine Velocity Engine
     * @param context DSpace Context
     * @param item DSpace Item
     */
    public abstract void doAction(VelocityContext velocityContext, VelocityEngine velocityEngine,
        Context context, Item item) throws Exception;


    /**
     * Render velocity template with provided context.
     *
     * @param context  velocity context
     * @param template template to render
     * @return String results of rendering
     */
    protected String renderTemplate(VelocityContext context, VelocityEngine velocityEngine, String template) {
        StringWriter writer = new StringWriter();
        StringResourceRepository repository = StringResourceLoader.getRepository();
        repository.putStringResource("template", template);
        velocityEngine.getTemplate("template").merge(context, writer);

        return writer.toString();
    }

}
