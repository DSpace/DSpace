package org.ssu.service;

import org.apache.commons.lang.StringUtils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EpersonService {
    private static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    public List<EPerson> getLatestRegisteredUsers(Context context, int limit) throws SQLException {
        return ePersonService.findAll(context, EPerson.ID)
                .stream()
                .sorted(Comparator.comparing(EPerson::getLegacyId).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean updateUserProfile(Context context, EPerson eperson, HttpServletRequest request) throws SQLException {
        String lastName = request.getParameter("last_name");
        String firstName = request.getParameter("first_name");
        String phone = request.getParameter("phone");
        String language = request.getParameter("language");
        eperson.setFirstName(context, firstName);
        eperson.setLastName(context, lastName);
        ePersonService.setMetadataSingleValue(context, eperson, "eperson", "phone", null, null, phone);
        eperson.setLanguage(context, language);
        String position = request.getParameter("position");
        try {
            Integer chair = Integer.valueOf(request.getParameter("chair_id"));
            eperson.setChairId(chair);
        } catch (NumberFormatException ex) {

        }
        eperson.setPosition(position);

        return (!StringUtils.isEmpty(lastName) && !StringUtils.isEmpty(firstName));
    }

    public boolean confirmAndSetPassword(EPerson eperson, HttpServletRequest request) {
        String password = request.getParameter("password");
        String passwordConfirm = request.getParameter("password_confirm");

        if ((password == null) || (password.length() < 6) || !password.equals(passwordConfirm)) {
            return false;
        } else {
            ePersonService.setPassword(eperson, password);
            return true;
        }
    }
}
