/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The NotifyServiceEntity REST Resource
 *
 * @author mohamed eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceRest extends BaseObjectRest<Integer> {
    public static final String NAME = "ldnservice";
    public static final String PLURAL_NAME = "ldnservices";
    public static final String CATEGORY = RestAddressableModel.LDN;

    private String name;
    private String description;
    private String url;
    private String ldnUrl;
    private boolean enabled;
    private BigDecimal score;
    private String lowerIp;
    private String upperIp;

    private List<NotifyServiceInboundPatternRest> notifyServiceInboundPatterns;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLdnUrl() {
        return ldnUrl;
    }

    public void setLdnUrl(String ldnUrl) {
        this.ldnUrl = ldnUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public List<NotifyServiceInboundPatternRest> getNotifyServiceInboundPatterns() {
        return notifyServiceInboundPatterns;
    }

    public void setNotifyServiceInboundPatterns(
        List<NotifyServiceInboundPatternRest> notifyServiceInboundPatterns) {
        this.notifyServiceInboundPatterns = notifyServiceInboundPatterns;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getLowerIp() {
        return lowerIp;
    }

    public void setLowerIp(String lowerIp) {
        this.lowerIp = lowerIp;
    }

    public String getUpperIp() {
        return upperIp;
    }

    public void setUpperIp(String upperIp) {
        this.upperIp = upperIp;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

}
