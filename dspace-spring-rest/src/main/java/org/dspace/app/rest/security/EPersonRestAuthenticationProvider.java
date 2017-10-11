package org.dspace.app.rest.security;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;

@Component
public class EPersonRestAuthenticationProvider implements AuthenticationProvider{

    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();


    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        Context context = new Context();
        EPerson ePerson = null;
        try {
            ePerson = ePersonService.findByEmail(context, email);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (ePerson != null) {
            String password = authentication.getCredentials().toString();
            if (ePersonService.checkPassword(context, ePerson, password)) {
                return new UsernamePasswordAuthenticationToken(ePerson.getEmail(), password, new ArrayList<>());
            }
        }
        return null;
    }

    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
