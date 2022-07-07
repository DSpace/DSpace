/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.dspace.app.rest.RestResourceController;

/**
 * The Researcher Profile REST resource.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@LinksRest(links = {
        @LinkRest(name = ResearcherProfileRest.ITEM, method = "getItem"),
        @LinkRest(name = ResearcherProfileRest.EPERSON, method = "getEPerson")
})
public class ResearcherProfileRest extends BaseObjectRest<UUID> {

    private static final long serialVersionUID = 1L;
    public static final String CATEGORY = RestModel.EPERSON;
    public static final String NAME = "profile";

    public static final String ITEM = "item";
    public static final String EPERSON = "eperson";

    private boolean visible;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String orcid;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OrcidSynchronizationRest orcidSynchronization;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public OrcidSynchronizationRest getOrcidSynchronization() {
        return orcidSynchronization;
    }

    public void setOrcidSynchronization(OrcidSynchronizationRest orcidSynchronization) {
        this.orcidSynchronization = orcidSynchronization;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<?> getController() {
        return RestResourceController.class;
    }

    /**
     * Inner class to model ORCID synchronization preferences and mode.
     *
     * @author Luca Giamminonni (luca.giamminonni at 4science.it)
     *
     */
    public static class OrcidSynchronizationRest {

        private String mode;

        private String publicationsPreference;

        private String fundingsPreference;

        private List<String> profilePreferences;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public List<String> getProfilePreferences() {
            return profilePreferences;
        }

        public void setProfilePreferences(List<String> profilePreferences) {
            this.profilePreferences = profilePreferences;
        }

        public String getPublicationsPreference() {
            return publicationsPreference;
        }

        public void setPublicationsPreference(String publicationsPreference) {
            this.publicationsPreference = publicationsPreference;
        }

        public String getFundingsPreference() {
            return fundingsPreference;
        }

        public void setFundingsPreference(String fundingsPreference) {
            this.fundingsPreference = fundingsPreference;
        }

    }

}