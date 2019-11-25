/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.AuthorityEntryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.Choice;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Choice in the DSpace API data
 * model and the REST data model.
 *
 * TODO please do not use this convert but use the wrapper {@link AuthorityUtils#convertEntry(Choice, String, String)}
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class AuthorityEntryRestConverter implements DSpaceConverter<Choice, AuthorityEntryRest> {

    @Override
    public AuthorityEntryRest convert(Choice choice, Projection projection) {
        AuthorityEntryRest entry = new AuthorityEntryRest();
        entry.setProjection(projection);
        entry.setValue(choice.value);
        entry.setDisplay(choice.label);
        entry.setId(choice.authority);
        entry.setOtherInformation(choice.extras);
        entry.setSelectable(choice.selectable);
        return entry;
    }

    @Override
    public Class<Choice> getModelClass() {
        return Choice.class;
    }
}
