/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used to handle/determine status of REST API.
 * Mainly to know your authentication status
 *
 */
@XmlRootElement(name = "report")
public class Report
{
    private String nickname;
    private String url;

    public Report() {
    	setNickname("na");
    	setUrl("");
    }

    
    public Report(String nickname, String url) {
        setNickname(nickname);
        setUrl(url);
    }
    public String getUrl()
    {
        return this.url;
    }
    public String getNickname()
    {
        return this.nickname;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }
}
