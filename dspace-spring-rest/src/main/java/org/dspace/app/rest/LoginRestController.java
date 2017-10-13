package org.dspace.app.rest;


import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Collection;

@RestController
@RequestMapping("/status")
public class LoginRestController {

    @Autowired
    private HttpServletRequest request;

    @RequestMapping(method = RequestMethod.GET)
    public String status() throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        //context.getDBConnection().setAutoCommit(false); // Disable autocommit.

        //TODO this is the implementation from Resource.createContext()
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null)
        {
            Collection<SimpleGrantedAuthority> specialGroups = (Collection<SimpleGrantedAuthority>) authentication.getAuthorities();
            for (SimpleGrantedAuthority grantedAuthority : specialGroups) {
                context.setSpecialGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, grantedAuthority.getAuthority()).getID());
            }
            context.setCurrentUser(EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, authentication.getName()));
        }
        EPerson current = context.getCurrentUser();
        return "EPerson: " + current.getEmail() + "\nFull name: " + current.getFullName();
    }

}
