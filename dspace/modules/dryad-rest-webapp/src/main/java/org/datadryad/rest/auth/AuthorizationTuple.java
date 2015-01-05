/*
 */
package org.datadryad.rest.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Encapsulates the 3 things we need to check to determine if a request is authorized
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AuthorizationTuple {
    public final Integer ePersonId;
    public final String httpMethod;
    public final String path;

    public AuthorizationTuple(Integer ePersonId, String httpMethod, String path) {
        this.ePersonId = ePersonId;
        this.httpMethod = httpMethod;
        this.path = path;
    }

    public final Boolean isComplete() {
        if(this.ePersonId == null) {
            return false;
        }
        if(this.httpMethod == null) {
            return false;
        }
        if(this.path == null) {
            return false;
        }
        return true;
    }

    public final List<String> getPathComponents() {
        if(this.path == null) {
            return new ArrayList<String>();
        }
        return Arrays.asList(StringUtils.split(path, '/'));
    }

    public Boolean containsPath(AuthorizationTuple otherTuple) {
        List<String> pathComponents = getPathComponents();
        List<String> otherPathComponents = otherTuple.getPathComponents();
        if(pathComponents.equals(otherPathComponents)) {
            // this.path equals other.path
            return Boolean.TRUE;
        } else if(pathComponents.size() < otherPathComponents.size()) {
            // this.path is less specific (fewer path components) than other
            List<String> subList = otherPathComponents.subList(0, pathComponents.size());
            return pathComponents.equals(subList);
        } else {
            // this.path is more specific (more path components) than other
            return Boolean.FALSE;
        }
    }
}
