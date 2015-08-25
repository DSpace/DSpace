/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.services.impl.set;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.xoai.exceptions.InvalidSetSpecException;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.HandleResolver;
import org.dspace.xoai.services.api.HandleResolverException;
import org.dspace.xoai.services.api.set.SetSpecResolver;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DSpaceSetSpecResolver implements SetSpecResolver {
    private static final String HANDLE_PREFIX = "{handle-prefix}";
    private static final String LOCAL_ID = "{local-id}";
    private static final String DEFAULT_FORMAT = "hdl_" + HANDLE_PREFIX + "_" + LOCAL_ID;

    @Autowired
    private ContextService contextService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private HandleResolver handleResolver;

    @Override
    public String toSetSpec(Community community) throws InvalidSetSpecException {
        String handle = community.getHandle();
        String[] split = handle.split("/");
        if (split.length != 2) throw new InvalidSetSpecException("Invalid handle "+handle);

        return format(getSetSpecFormat(Community.class), split[0], split[1]);
    }

    @Override
    public String toSetSpec(Collection collection) throws InvalidSetSpecException {
        String handle = collection.getHandle();
        String[] split = handle.split("/");
        if (split.length != 2) throw new InvalidSetSpecException("Invalid handle "+handle);

        return String.format(getSetSpecFormat(Community.class), split[0], split[1]);
    }

    @Override
    public DSpaceObject fromSetSpec(String setSpec) throws InvalidSetSpecException {
        String communityPattern = getPattern(Community.class);
        String collectionPattern = getPattern(Collection.class);
        String pattern;
        if (setSpec.matches(communityPattern))
            pattern = communityPattern;
        else if (setSpec.matches(collectionPattern))
            pattern = collectionPattern;
        else
            throw new InvalidSetSpecException("Unknown set spec");


        Matcher matcher = Pattern.compile(pattern).matcher(setSpec);
        String handle_prefix = matcher.group(1);
        String local_id = matcher.group(2);

        try {
            return handleResolver.resolve(handle_prefix + "/" + local_id);
        } catch (HandleResolverException e) {
            throw new InvalidSetSpecException(e);
        }
    }

    private String format(String setSpecFormat, String prefix, String localId) {
        return setSpecFormat.replace(HANDLE_PREFIX, prefix).replace(LOCAL_ID, localId);
    }

    private String getPattern(Class<?> clazz) {
        return "^"+getSetSpecFormat(clazz).replace(HANDLE_PREFIX, "([0-9]+)").replace(LOCAL_ID, "([0-9]+)")+"$";
    }

    private String getSetSpecFormat(Class<?> clazz) {
        String property = configurationService.getProperty("oai", clazz.getSimpleName().toLowerCase() + ".setSpecFormat");
        return property == null ? DEFAULT_FORMAT : property;
    }
}
