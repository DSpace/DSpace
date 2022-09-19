/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.external;

/**
 * The `external/Handle` REST Resource
 */
public class HandleRest {

    private String handle;
    private String url;
    private String title;
    private String repository;
    private String submitdate;
    private String reportemail;
    private String subprefix;

    public HandleRest() {
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getSubmitdate() {
        return submitdate;
    }

    public void setSubmitdate(String submitdate) {
        this.submitdate = submitdate;
    }

    public String getReportemail() {
        return reportemail;
    }

    public void setReportemail(String reportemail) {
        this.reportemail = reportemail;
    }

    public String getSubprefix() {
        return subprefix;
    }

    public void setSubprefix(String subprefix) {
        this.subprefix = subprefix;
    }
}
