/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Database entity representation of the webApp table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name="webapp")
public class WebApp implements ReloadableEntity<Integer> {


    @Id
    @Column(name="webapp_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="webapp_seq")
    @SequenceGenerator(name="webapp_seq", sequenceName="webapp_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "appname", unique = true, length = 32)
    private String appName;

    @Column(name = "url")
    private String url;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "started")
    private Date started;

    @Column(name = "isui")
    private Integer isui;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.app.util.service.WebAppService#create(Context, String, String, Date, int)}
     */
    protected WebApp()
    {

    }

    public Integer getID() {
        return id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Integer getIsui() {
        return isui;
    }

    public void setIsui(Integer isui) {
        this.isui = isui;
    }
}
