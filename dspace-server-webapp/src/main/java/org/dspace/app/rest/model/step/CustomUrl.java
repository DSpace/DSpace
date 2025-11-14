/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Implementation of {@link SectionData} related to the custom URL section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrl implements SectionData {

    private static final long serialVersionUID = 6257806753115590282L;

    private String url;

    @JsonProperty("redirected-urls")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> redirectedUrls;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getRedirectedUrls() {
        return redirectedUrls;
    }

    public void setRedirectedUrls(List<String> redirectedUrls) {
        this.redirectedUrls = redirectedUrls;
    }

}
