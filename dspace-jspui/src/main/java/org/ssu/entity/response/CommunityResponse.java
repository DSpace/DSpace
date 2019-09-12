package org.ssu.entity.response;

import org.dspace.content.Collection;
import org.dspace.content.Community;

import java.util.List;
import java.util.Map;

public class CommunityResponse {
    private List<Community> communities;
    private Map<String, List<Community>> commMap;
    private Boolean isAdmin;

    private CommunityResponse(Builder builder) {
        communities = builder.communities;
        commMap = builder.commMap;
        isAdmin = builder.isAdmin;
    }

    public List<Community> getCommunities() {
        return communities;
    }

    public Map<String, List<Community>> getCommMap() {
        return commMap;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }


    public static final class Builder {
        private List<Community> communities;
        private Map<String, List<Community>> commMap;
        private Boolean isAdmin;

        public Builder() {
        }

        public Builder(CommunityResponse copy) {
            this.communities = copy.getCommunities();
            this.commMap = copy.getCommMap();
            this.isAdmin = copy.getIsAdmin();
        }

        public Builder withCommunities(List<Community> communities) {
            this.communities = communities;
            return this;
        }

        public Builder withCommMap(Map<String, List<Community>> commMap) {
            this.commMap = commMap;
            return this;
        }

        public Builder withIsAdmin(Boolean isAdmin) {
            this.isAdmin = isAdmin;
            return this;
        }

        public CommunityResponse build() {
            return new CommunityResponse(this);
        }
    }
}
