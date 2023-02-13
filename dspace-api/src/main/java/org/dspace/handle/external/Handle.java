/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.external;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.dspace.handle.external.ExternalHandleConstants.MAGIC_BEAN;

import java.util.Objects;
import java.util.UUID;

import org.dspace.handle.HandlePlugin;

/**
 * The external Handle which contains the url with the `@magicLindat` string. That string is parsed to the
 * attributes.
 * Created by
 * @author okosarko on 13.10.15.
 * Modified by
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class Handle {

    private String handle;
    public String url;
    public String title;
    public String repository;
    public String submitdate;
    public String reportemail;
    public String subprefix;
    public String datasetName;
    public String datasetVersion;
    public String query;
    public String token;

    public Handle(){

    }

    public Handle(String handle, String url, String title, String repository, String submitdate, String reportemail,
                  String datasetName, String datasetVersion, String query, String token, String subprefix) {
        this.handle = handle;
        this.url = url;
        this.title = title;
        this.repository = repository;
        this.submitdate = submitdate;
        this.reportemail = reportemail;
        this.datasetName = datasetName;
        this.datasetVersion = datasetVersion;
        this.query = query;
        this.token = token;
        this.subprefix = subprefix;
    }

    /**
     * Constructor which parse the magicURL to the attributes
     * @param handle
     * @param magicURL
     */
    public Handle(String handle, String magicURL) {
        this.handle = handle;
        //similar to HandlePlugin
        String[] splits = magicURL.split(MAGIC_BEAN,10);
        this.url = splits[splits.length - 1];
        this.title = splits[1];
        this.repository = splits[2];
        this.submitdate = splits[3];
        this.reportemail = splits[4];
        if (isNotBlank(splits[5])) {
            this.datasetName = splits[5];
        }
        if (isNotBlank(splits[6])) {
            this.datasetVersion = splits[6];
        }
        if (isNotBlank(splits[7])) {
            this.query = splits[7];
        }
        if (isNotBlank(splits[8])) {
            this.token = splits[8];
        }
        this.subprefix = handle.split("/",2)[1].split("-",2)[0];
    }

    /**
     * From the attributes generate the url with `@magicLindat` string
     * @return url with the `@magicLindat` string
     */
    public String getMagicUrl() {
        return this.getMagicUrl(this.title, this.submitdate, this.reportemail, this.datasetName, this.datasetVersion,
                this.query, this.url);
    }

    /**
     * From the attributes generate the url with `@magicLindat` string
     * @return url with the `@magicLindat` string
     */
    public String getMagicUrl(String title, String submitdate, String reportemail, String datasetName,
                              String datasetVersion, String query, String url) {
        String magicURL = "";
        String token = UUID.randomUUID().toString();
        String[] magicURLProps = new String[] {title, HandlePlugin.getRepositoryName(), submitdate, reportemail,
            datasetName, datasetVersion, query, token, url};
        for (String part : magicURLProps) {
            if (isBlank(part)) {
                //optional dataset etc...
                part = "";
            }
            magicURL += MAGIC_BEAN + part;
        }
        return magicURL;
    }

    /**
     * It the `handle` attribute is null return the CanonicalHandlePrefix
     * @return `handle` attribute value or the CanonicalHandlePrefix loaded from the configuration
     */
    public String getHandle() {
        return Objects.isNull(handle) ? null : HandlePlugin.getCanonicalHandlePrefix() + handle;
    }

    /**
     * Remove the CanonicalHandlePrefix from the `handle` attribute
     * @param handle
     */
    public void setHandle(String handle) {
        this.handle = handle.replace(HandlePlugin.getCanonicalHandlePrefix(),"");
    }
}
