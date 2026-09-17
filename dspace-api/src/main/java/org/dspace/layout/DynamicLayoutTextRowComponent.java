/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * Implementation of {@link DynamicLayoutSectionComponent} that gives back
 * a simple line of text, that can either be, for example, html or plain text
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DynamicLayoutTextRowComponent implements DynamicLayoutSectionComponent {

    private Integer order = 0;
    private String contentType;
    private String content;
    private String style;


    @Override
    public String getStyle() {
        return style;
    }

    /**
     * Returns the content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the content type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the order.
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * Sets the order.
     */
    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * Sets the content type.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Sets the content.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Sets the style.
     */
    public void setStyle(String style) {
        this.style = style;
    }
}
