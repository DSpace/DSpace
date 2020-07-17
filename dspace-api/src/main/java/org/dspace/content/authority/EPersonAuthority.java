/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.util.UUIDUtils;

/**
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class EPersonAuthority implements ChoiceAuthority {
    private static final Logger log = Logger.getLogger(EPersonAuthority.class);

    /**
     * the name assigned to the specific instance by the PluginService, @see
     * {@link NameAwarePlugin}
     **/
    private String authorityName;

    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Override
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 2, locale);
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Context context = null;
        if (limit <= 0) {
            limit = 20;
        }
        context = new Context();
        List<EPerson> ePersons = null;
        try {
            ePersons = ePersonService.search(context, text, start, limit);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        List<Choice> choiceList = new ArrayList<Choice>();
        for (EPerson eperson : ePersons) {
            choiceList.add(new Choice(eperson.getID().toString(), eperson.getFullName(), eperson.getFullName()));
        }
        Choice[] results = new Choice[choiceList.size()];
        results = choiceList.toArray(results);
        return new Choices(results, start, ePersons.size(), Choices.CF_AMBIGUOUS, ePersons.size() > (start + limit), 0);
    }

    @Override
    public String getLabel(String key, String locale) {

        UUID uuid = UUIDUtils.fromString(key);
        if (uuid == null) {
            return null;
        }

        Context context = new Context();
        try {
            EPerson ePerson = ePersonService.find(context, uuid);
            return ePerson != null ? ePerson.getName() : null;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.authorityName = name;
    }
}