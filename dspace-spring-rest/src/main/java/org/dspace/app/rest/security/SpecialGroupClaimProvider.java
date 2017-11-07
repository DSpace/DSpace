package org.dspace.app.rest.security;

import com.nimbusds.jwt.JWTClaimsSet;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SpecialGroupClaimProvider implements JWTClaimProvider {

    public static final String SPECIAL_GROUPS = "sg";

    @Autowired
    private AuthenticationService authenticationService;

    public String getKey() {
        return SPECIAL_GROUPS;
    }

    public Object getValue(Context context, HttpServletRequest request) {
        List<Group> groups = new ArrayList<>();
        try {
            groups = authenticationService.getSpecialGroups(context, request);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        List<String> groupIds = groups.stream().map(group -> group.getID().toString()).collect(Collectors.toList());
        return groupIds;
    }

    public void parseClaim(Context context, HttpServletRequest request, JWTClaimsSet jwtClaimsSet) {
        try {
            List<String> groupIds = jwtClaimsSet.getStringListClaim(SPECIAL_GROUPS);
            for (String groupId : groupIds) {
                context.setSpecialGroup(UUID.fromString(groupId));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
