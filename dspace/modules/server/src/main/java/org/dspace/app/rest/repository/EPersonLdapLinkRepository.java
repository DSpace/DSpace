package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import edu.umd.lib.dspace.authenticate.LdapService;
import edu.umd.lib.dspace.authenticate.impl.Ldap;
import edu.umd.lib.dspace.authenticate.impl.LdapServiceImpl;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.LdapRest;
import org.dspace.app.rest.model.UnitRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the direct Ldap subresource of an individual eperson.
 */
@Component(EPersonRest.CATEGORY + "." + EPersonRest.NAME + "." + EPersonRest.LDAP)
public class EPersonLdapLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {
    private static final Logger log = LoggerFactory.getLogger(EPersonLdapLinkRepository.class);

    @Autowired
    EPersonService epersonService;

    /**
     * Returns a (possibly null) LdapRest object containing LDAP information for
     * the given EPerson.
     */
    @PreAuthorize("hasPermission(#epersonId, 'EPERSON', 'READ')")
    public LdapRest getLdap(@Nullable HttpServletRequest request,
            UUID epersonId,
            @Nullable Pageable optionalPageable,
            Projection projection) {
        try {
            Context context = obtainContext();
            EPerson eperson = epersonService.find(context, epersonId);
            if (eperson == null) {
                throw new ResourceNotFoundException("No such eperson: " + epersonId);
            }

            String netId = eperson.getNetid();
            if (netId == null) {
                return null;
            }

            try (LdapService ldapService = new LdapServiceImpl(context)) {
                log.debug("Querying LDAP for netId={}", netId);
                Ldap ldap = ldapService.queryLdap(netId);

                if (ldap == null) {
                    log.debug("No LDAP information found for netId={}", netId);
                    return null;
                }

                log.debug("LDAP information found for netID={}", netId);

                LdapRest ldapRest = new LdapRest();

                ldapRest.setFirstName(ldap.getFirstName());
                ldapRest.setLastName(ldap.getLastName());
                ldapRest.setPhone(ldap.getPhone());
                ldapRest.setEmail(ldap.getEmail());

                ldapRest.setIsFaculty(ldap.isFaculty());
                ldapRest.setUmAppointments(ldap.getAttributeAll("umappointment"));

                List<GroupRest> groupList = ldap.getGroups(context).stream()
                    .map(g -> (GroupRest) converter.toRest(g, projection)).collect(Collectors.toList());
                ldapRest.setGroups(groupList);

                List<UnitRest> matchedUnitsList = ldap.getMatchedUnits(context).stream()
                    .map(u -> (UnitRest) converter.toRest(u, projection)).collect(Collectors.toList());
                ldapRest.setMatchedUnits(matchedUnitsList);

                ldapRest.setUnmatchedUnits(ldap.getUnmatchedUnits(context));

                return ldapRest;
            } catch (NamingException ne) {
                log.error("Exception accessing LDAP. netId=" + netId, ne);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
