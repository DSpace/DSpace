/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.matcher;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.DSpaceObject;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class SubscribeMatcher extends BaseMatcher<Subscription> {

    private final DSpaceObject dso;
    private final EPerson eperson;
    private final List<SubscriptionParameter> parameters;
    private final String type;

    private SubscribeMatcher(DSpaceObject dso, EPerson eperson, String type, List<SubscriptionParameter> parameters) {
        this.dso = dso;
        this.eperson = eperson;
        this.parameters = parameters;
        this.type = type;
    }

    public static SubscribeMatcher matches(DSpaceObject dso, EPerson ePerson, String type,
                                           List<SubscriptionParameter> parameters) {
        return new SubscribeMatcher(dso, ePerson, type, parameters);
    }

    @Override
    public boolean matches(Object subscription) {
        Subscription s = (Subscription) subscription;
        return s.getEPerson().equals(eperson)
            && s.getDSpaceObject().equals(dso)
            && s.getSubscriptionType().equals(type)
            && checkParameters(s.getSubscriptionParameterList());
    }

    private Boolean checkParameters(List<SubscriptionParameter> parameters) {
        if (parameters.size() != this.parameters.size()) {
            return false;
        }
        // FIXME: for check purpose we rely on name and value. Evaluate to extend or refactor this part
        for (int i = 0; i < parameters.size(); i++) {
            SubscriptionParameter parameter = parameters.get(i);
            SubscriptionParameter match = this.parameters.get(i);
            boolean differentName = !parameter.getName().equals((match.getName()));
            if (differentName) {
                return false;
            }
            boolean differentValue = !parameter.getValue().equals((match.getValue()));
            if (differentValue) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        String subscription = String.format("Type: %s, eperson: %s, dso: %s, params: %s",
                                      type, eperson.getID(), dso.getID(), parameters.stream()
                                                                                    .map(p -> "{ name: " + p.getName() +
                                                                                        ", value: " + p.getValue() +
                                                                                        "}")
                                                                                    .collect(Collectors.joining(", ")));
        description.appendText("Subscription matching: " + subscription);
    }
}
