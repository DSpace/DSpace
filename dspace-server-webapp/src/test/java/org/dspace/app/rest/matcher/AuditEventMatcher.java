/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.dspace.app.audit.AuditEvent;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class AuditEventMatcher {

    private AuditEventMatcher() { }

    public static Matcher<? super Object> matchAuditEvent(AuditEvent audit) {
        return allOf(
                matchProperties(audit),
                matchLinks(audit));
    }

    public static Matcher<? super Object> matchAuditEventFullProjection(AuditEvent audit, boolean missingSubject,
            boolean missingObject, boolean missingEperson) {
        return allOf(
                matchProperties(audit),
                matchLinks(audit),
                matchEmbedds(audit, missingSubject, missingObject, missingEperson));
    }

    public static Matcher<? super Object> matchProperties(AuditEvent audit) {
        return allOf(
            audit.getUuid() != null ?
                hasJsonPath("$.id", is(audit.getUuid().toString())) :
                    hasJsonPath("$.id", Matchers.not(Matchers.empty())),
            hasJsonPath("$.detail", is(audit.getDetail())),
            hasJsonPath("$.subjectUUID", is(uuidStr(audit.getSubjectUUID()))),
            hasJsonPath("$.subjectType", is(audit.getSubjectType())),
            hasJsonPath("$.objectUUID", is(uuidStr(audit.getObjectUUID()))),
            hasJsonPath("$.objectType", is(audit.getObjectType())),
            //we should apply the date formatter to make the check
            //hasJsonPath("$.timeStamp", is(audit.getDatetime())),
            hasJsonPath("$.type", is("auditevent")),
            hasJsonPath("$.uniqueType", is("system.auditevent")));
     }

    public static Matcher<? super Object> matchLinks(AuditEvent audit) {
        if (audit.getUuid() != null) {
            return allOf(
                    hasJsonPath("$._links.self.href", endsWith("/api/system/auditevents/" + audit.getUuid())),
                    hasJsonPath("$._links.object.href",
                            endsWith("/api/system/auditevents/" + audit.getUuid() + "/object")),
                    hasJsonPath("$._links.subject.href",
                            endsWith("/api/system/auditevents/" + audit.getUuid() + "/subject")),
                    hasJsonPath("$._links.eperson.href",
                            endsWith("/api/system/auditevents/" + audit.getUuid() + "/eperson"))
                    );
        } else {
            return allOf(
                    hasJsonPath("$._links.self.href", containsString("/api/system/auditevents/")),
                    hasJsonPath("$._links.object.href", endsWith("/object")),
                    hasJsonPath("$._links.object.href", containsString("/api/system/auditevents/")),
                    hasJsonPath("$._links.subject.href", endsWith("/subject")),
                    hasJsonPath("$._links.subject.href", containsString("/api/system/auditevents/")),
                    hasJsonPath("$._links.eperson.href", endsWith("/eperson")),
                    hasJsonPath("$._links.eperson.href", containsString("/api/system/auditevents/"))
                    );
        }
    }

    public static Matcher<? super Object> matchEmbedds(AuditEvent audit, boolean missingSubject, boolean missingObject,
            boolean missingEperson) {
        return allOf(
                audit.getSubjectUUID() == null || missingSubject ?
                    hasJsonPath("$._embedded.subject", is(nullValue())) :
                        hasJsonPath("$._embedded.subject.id", is(audit.getSubjectUUID().toString())),
                audit.getObjectUUID() == null || missingObject ?
                        hasJsonPath("$._embedded.object", is(nullValue())) :
                            hasJsonPath("$._embedded.object.id", is(audit.getObjectUUID().toString())),
                audit.getEpersonUUID() == null || missingEperson ?
                        hasJsonPath("$._embedded.eperson", is(nullValue())) :
                            hasJsonPath("$._embedded.eperson.id", is(audit.getEpersonUUID().toString())));
    }

    private static String uuidStr(UUID subjectUUID) {
        if (subjectUUID != null) {
            return subjectUUID.toString();
        }
        return null;
    }

}
