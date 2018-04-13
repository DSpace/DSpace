/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.jaxb.model.message;

import java.util.Collection;
import java.util.Map;

/**
 * 
 * @author Will Simpson
 * 
 */
public interface ActivitiesContainer {

    Map<String, ? extends Activity> retrieveActivitiesAsMap();

    Collection<? extends Activity> retrieveActivities();

}
