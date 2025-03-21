package org.dspace.app.bulkedit.util;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class BulkEditUtil {
    /**
     * The prefix of the authority controlled field
     */
    protected static final String AC_PREFIX = "authority.controlled.";

    @Autowired
    protected ConfigurationService configurationService;

    /**
     * The authority controlled fields
     */
    private Set<String> authorityControlledFields;

    /**
     * Set authority controlled fields
     */
    protected void initialiseAuthorityControlledFields() {
        authorityControlledFields = new HashSet<>();
        Enumeration propertyNames = configurationService.getProperties().propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = ((String) propertyNames.nextElement()).trim();
            if (key.startsWith(AC_PREFIX)
                && configurationService.getBooleanProperty(key, false)) {
                authorityControlledFields.add(key.substring(AC_PREFIX.length()));
            }
        }
    }

    public Set<String> getAuthorityControlledFields() {
        if (authorityControlledFields == null) {
            initialiseAuthorityControlledFields();
        }
        return authorityControlledFields;
    }

    public boolean isAuthorityControlledField(String field) {
        return getAuthorityControlledFields().contains(field);
    }

    public static BulkEditUtil getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("bulkEditUtil", BulkEditUtil.class);
    }
}
