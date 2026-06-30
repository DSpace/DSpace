/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

/**
 * Implementation of {@link CrisLayoutSectionComponent} that gives back
 * a simple line of text, that can either be, for example, html or plain text
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CrisLayoutTextRowComponent implements CrisLayoutSectionComponent {

    private Integer order = 0;
    private String contentType;
    private String content;
    private String style;


    @Override
    public String getStyle() {
        return style;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}
