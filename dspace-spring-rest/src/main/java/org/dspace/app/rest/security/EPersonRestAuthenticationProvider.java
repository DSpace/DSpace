package org.dspace.app.rest.security;

import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class EPersonRestAuthenticationProvider implements AuthenticationProvider{

    protected AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    private static final Logger log = LoggerFactory.getLogger(EPersonRestAuthenticationProvider.class);

    @Autowired
    private HttpServletRequest request;


//    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
//        String email = authentication.getName();
//        Context context = new Context();
//        EPerson ePerson = null;
//        try {
//            ePerson = ePersonService.findByEmail(context, email);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        if (ePerson != null) {
//            String password = authentication.getCredentials().toString();
//            if (ePersonService.checkPassword(context, ePerson, password)) {
//                return new UsernamePasswordAuthenticationToken(ePerson.getEmail(), password, new ArrayList<>());
//            }
//        }
//        return null;
//    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Context context = null;

        try {
            context = new Context();
            String name = authentication.getName();
            String password = authentication.getCredentials().toString();
            HttpServletRequest httpServletRequest = request;
            List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();


            int implicitStatus = authenticationService.authenticateImplicit(context, null, null, null, httpServletRequest);

            if (implicitStatus == AuthenticationMethod.SUCCESS) {
                log.info(LogManager.getHeader(context, "login", "type=implicit"));
                addSpecialGroupsToGrantedAuthorityList(context, httpServletRequest, grantedAuthorities);
                return new UsernamePasswordAuthenticationToken(name, password, grantedAuthorities);
            } else {
                int authenticateResult = authenticationService.authenticate(context, name, password, null, httpServletRequest);
                if (AuthenticationMethod.SUCCESS == authenticateResult) {
                    addSpecialGroupsToGrantedAuthorityList(context, httpServletRequest, grantedAuthorities);
                    log.info(LogManager
                            .getHeader(context, "login", "type=explicit"));

                    return new UsernamePasswordAuthenticationToken(name, password, grantedAuthorities);
                } else {
                    log.info(LogManager.getHeader(context, "failed_login", "email="
                            + name + ", result="
                            + authenticateResult));
                    throw new BadCredentialsException("Login failed");
                }
            }
        } catch (BadCredentialsException e)
        {
            throw e;
        } catch (Exception e) {
            log.error("Error while authenticating in the rest api", e);
        } finally {
            if (context != null && context.isValid()) {
                try {
                    context.complete();
                } catch (SQLException e) {
                    log.error(e.getMessage() + " occurred while trying to close", e);
                }
            }
        }

        return null;
    }

    protected void addSpecialGroupsToGrantedAuthorityList(Context context, HttpServletRequest httpServletRequest, List<SimpleGrantedAuthority> grantedAuthorities) throws SQLException {
        List<Group> groups = authenticationService.getSpecialGroups(context, httpServletRequest);
        for (Group group : groups) {
            grantedAuthorities.add(new SimpleGrantedAuthority(group.getName()));
        }
    }


    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
