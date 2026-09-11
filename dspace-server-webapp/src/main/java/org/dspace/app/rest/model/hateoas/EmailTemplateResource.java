/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.EmailTemplateRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * HAL Resource wrapper for {@link EmailTemplateRest}.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
@RelNameDSpaceResource(EmailTemplateRest.NAME)
public class EmailTemplateResource extends DSpaceResource<EmailTemplateRest> {

    /**
     * Constructs a HAL resource wrapping the given email template REST object.
     *
     * @param content the email template REST object to wrap
     * @param utils   the REST utilities used to build resource links
     */
    public EmailTemplateResource(EmailTemplateRest content, Utils utils) {
        super(content, utils);
    }
}
