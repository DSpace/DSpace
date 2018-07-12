/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.util.ArrayList;
import java.util.List;

import org.dspace.eperson.EPerson;

/**
 * Simple DTO to transfer data about the corresponding author for the Request
 * Copy feature
 *
 * @author Andrea Bollini
 */
public class RequestItemAuthor {
    private final String fullName;
    private final String email;

    public RequestItemAuthor(String fullName, String email) {
        super();
        this.fullName = fullName;
        this.email = email;
    }

    public RequestItemAuthor(EPerson ePerson) {
        super();
        this.fullName = ePerson.getFullName();
        this.email = ePerson.getEmail();
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }
    /**
     * Build a comma-list of addresses from a list of request recipients.
     * @param recipients those to receive the request.
     * @return addresses of the recipients, separated by ", ".
     */
    public static String listAddresses(List<RequestItemAuthor> recipients) {
        List<String> addresses = new ArrayList(recipients.size());
        for (RequestItemAuthor recipient : recipients) {
            addresses.add(recipient.getEmail());
        }
        return String.join(", ", addresses);
    }

    /**
     * Build a comma-list of full names from a list of request recipients.
     * @param recipients those to receive the request.
     * @return names of the recipients, separated by ", ".
     */
    public static String listNames(List<RequestItemAuthor> recipients) {
        List<String> names = new ArrayList(recipients.size());
        for (RequestItemAuthor recipient : recipients) {
            names.add(recipient.getFullName());
        }
        return String.join(", ", names);
    }
}
