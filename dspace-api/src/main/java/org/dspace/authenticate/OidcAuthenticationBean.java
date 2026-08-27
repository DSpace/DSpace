/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *In: RequestCopy
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;


import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static org.apache.commons.lang.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authenticate.oidc.OidcClient;
import org.dspace.authenticate.oidc.model.OidcTokenResponseDTO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

// UM Changes
import java.util.UUID;
import org.dspace.eperson.service.GroupService;
import java.io.*;
import java.net.*;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.eperson.factory.EPersonServiceFactory;
import java.util.ArrayList;
import java.util.Collections;

import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.service.ClientInfoService;

/**
 * OpenID Connect Authentication for DSpace.
 *
 * This implementation doesn't allow/needs to register user, which may be holder
 * by the openID authentication server.
 *
 * @link   https://openid.net/developers/specs/
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class OidcAuthenticationBean implements AuthenticationMethod {

    protected ClientInfoService clientInfoService;

    public static final String OIDC_AUTH_ATTRIBUTE = "oidc";

    private final static String LOGIN_PAGE_URL_FORMAT = "%s?client_id=%s&response_type=code&scope=%s&redirect_uri=%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(OidcAuthenticationBean.class);

    private static final String OIDC_AUTHENTICATED = "oidc.authenticated";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OidcClient oidcClient;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return canSelfRegister();
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
      try
            {
        clientInfoService = CoreServiceFactory.getInstance().getClientInfoService();

        String defaultUUID = "00000000-0000-1000-a000-000000000000";
        UUID bioId = UUID.fromString(defaultUUID);
        UUID umId = UUID.fromString(defaultUUID);
        UUID bentOnlyId = UUID.fromString(defaultUUID);
        int count = 0;

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        // UM Change
        // The way swordv2 works now, the code will come here, and in that case request will be null.
        if ( request == null )
        {
            return Collections.emptyList();
        }
        // This is what you should use on production.
        String addr = request.getRemoteAddr();

        // This is the one you should use local machine.
        String addr3 = request.getHeader("X-Forwarded-For");

        // Get the user's IP address
        String addr2 = clientInfoService.getClientIp(request);

        String referer = request.getHeader("referer");

        // Define the IP address that should trigger an error
        String problematicIpAddress = "10.255.12.30";
        
        // Check if the client's IP address matches the problematic IP, for debugging.
        if (problematicIpAddress.equals(addr)) {
            // Throw a runtime exception to generate a stack trace
            //throw new RuntimeException("Access attempt from problematic IP address: " + addr);
        }

                if ( addr == null )
                {
                    return Collections.emptyList();
                }

                if ( isBioUser( request, addr ) )
                    {
                        Group bioGroup = groupService.findByName(context, "Bio Users");
                        if (bioGroup != null) {
                            bioId = bioGroup.getID();  // This is safe, as bioGroup exists
                            count++;
                        }
                    }

                if ( isBentleyOnlyUser( context, request, addr ) )
                    {
                        Group bentOnlyGroup = groupService.findByName(context, "Bentley Only Users");
                        if (bentOnlyGroup  != null) {
                            // Append to list of elligible groups
                            bentOnlyId = bentOnlyGroup.getID();
                            count++;
                        }
                    }

                // If logged in and has access.
                // OR is at a UM Address
                if (hasUMPriviledges(context) || isUMUser(request, addr))
                    {
                        Group umGroup = groupService.findByName(context, "UM Users");
                        if (umGroup != null) {
                            // Append to list of elligible groups
                            umId = umGroup.getID();
                            count++;
                        }

                    }

                if ( (bioId.compareTo(UUID.fromString(defaultUUID))==1) && (umId.compareTo(UUID.fromString(defaultUUID))==1) && (bentOnlyId.compareTo(UUID.fromString(defaultUUID))==1) )
                    {
                        return Collections.emptyList();
                    }

                UUID[] groupIds = new UUID[count];
                int newcount = 0;
                if ( !bioId.equals(UUID.fromString(defaultUUID)) )
                    {
                        groupIds[newcount] = bioId;
                        newcount++;
                    }
                if ( !bentOnlyId.equals(UUID.fromString(defaultUUID)) )
                    {
                        groupIds[newcount] = bentOnlyId;
                        newcount++;
                    }
                if ( !umId.equals(UUID.fromString(defaultUUID)) )
                    {
                        groupIds[newcount] = umId;
                    }

                List<Group> specialGroups = new ArrayList<Group>();
                for(int i = 0; i < groupIds.length; i++)
                {
                    Group g =  EPersonServiceFactory.getInstance().getGroupService().find(context, groupIds[i]);;
                    specialGroups.add ( g );
                }

                return specialGroups;

            }
        catch(SQLException sqle)
            {
                return Collections.emptyList();
            }

    }


    public static boolean isUMUser(HttpServletRequest request, String addr)
    {
       //String addr = request.getHeader("X-Forwarded-For");
       //String addr = request.getRemoteAddr();

       String ips = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getProperty("ip.umIPs");
       final String[] umIPs = ips.split("\\|");

        if ( addr == null )
        {
            return false;
        }

        for (int i = 0; i < umIPs.length; i++)
        {
            if (addr.startsWith(umIPs[i]))
            {
                return true;
            }
        }

        return false;
    }


    private static boolean isWithinRange(String addr, String[] range) {
      String[] rangeStart = range[0].split("\\.");
      String[] rangeEnd = range[1].split("\\.");
      String[] addrParts = addr.split("\\.");

      for (int i = 0; i < 4; i++) {
        int start = Integer.parseInt(rangeStart[i]);
        int end = Integer.parseInt(rangeEnd[i]);
        int current = Integer.parseInt(addrParts[i]);

        if (current < start || current > end) {
            return false;
        }
      }
      return true;
    }

    public static boolean isBioUser(HttpServletRequest request, String addr)
    {


        if ( addr == null )
        {
            return false;
        }

        String ips1 = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                     .getProperty("ip.bioIPsRange1");

        String ips2 = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                     .getProperty("ip.bioIPsRange2");

        final String[] bioIPsRange1 = ips1.split("\\|");
        final String[] bioIPsRange2 = ips2.split("\\|");       

        String[] range1 = {bioIPsRange1[0], bioIPsRange1[1]};
        String[] range2 = {bioIPsRange2[0], bioIPsRange2[1]};

        return isWithinRange(addr, range1) || isWithinRange(addr, range2);  
    }

   public static boolean isBentleyOnlyUser(Context context, HttpServletRequest request, String addr)
    {
       //String addr = request.getRemoteAddr();
       //String addr = request.getHeader("X-Forwarded-For");

       String ips = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                  .getProperty("ip.BentleyOnlyIPs");

       final String[] BentleyOnlyIPs = ips.split("\\|");

        if ( addr == null )
        {
            return false;
        }

        int count = 0;
        for (int i = 0; i < BentleyOnlyIPs.length; i++)
        {
            while ( count < 128 )
            {
                if (addr.equals( BentleyOnlyIPs[i] + Integer.toString(count) ) )

                {
                    return true;
                }
                count = count + 1;
            }
        }


        return false;
    }

    public static boolean hasUMPriviledges(Context context)
    {

        String api_key = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getProperty("api.user.key");

        try
        {
        EPerson eperson = context.getCurrentUser();
        String email = "noemail@umich.edu";
        if ( eperson != null )
            {
                email = eperson.getEmail ();
            }
        else
            {
                //LOGGER.info ("OIDC: hasUMPriviledges(false) email not found; email =" + email);
                return false;
            }

        // http://www.unix.org.ua/orelly/java-ent/jnut/ch04_02.htm  good page about
        // manipulating strings.
        // Now emove the @xxxx.xxx from the email
        int pos = email.indexOf('@');
        if ( pos > 0 )
            {
                String userid = email.substring(0,pos); // Extract the userid
                String request_url = "https://api-na.hosted.exlibrisgroup.com/almaws/v1/users/" + userid + "?apikey=" + api_key;

                URL url = new URL(request_url);

                // Get an input stream for reading
                InputStream in = url.openStream();

                // Create a buffered input stream for efficency
                BufferedInputStream bufIn = new BufferedInputStream(in);

                StringBuffer ReturnedValue = new StringBuffer("");
                for (;;)
                    {
                        int data = bufIn.read();

                        // Check for EOF
                        if (data == -1)
                            {break;}
                        else
                            {
                                ReturnedValue.append ( (char) data );
                            }
                    }
                String ResponseValue = ReturnedValue.toString();
                int pos2 = ResponseValue.indexOf("Error in Verification");
                if ( pos2 > 0 )
                    {
                        //LOGGER.info ("OIDC: hasUMPriviledges(false) ERROR with verification; email =" + email);
                        return false;
                    }
                else
                    {
                        // Now check for:
                        //  <z303-budget>UMAA - Ann Arbor
                        //  <z303-budget>UMFL - Flint
                        //  <z303-budget>UMDB - Dearborn
                        int posUM = ResponseValue.indexOf("UMAA</campus_code>");
                        int posFL = ResponseValue.indexOf("UMFL</campus_code>");
                        int posDB = ResponseValue.indexOf("UMDB</campus_code>");
                        if ( ( posUM > 0 ) || ( posFL > 0 ) || ( posDB > 0 ) )
                        {
                            // Has UM permissions
                            LOGGER.info ("OIDC: hasUMPriviledges(true) UM Person; email =" + email);
                            return true;
                        }
                        else
                        {
                            LOGGER.info ("OIDC: hasUMPriviledges(false) Not a UM Person; email =" + email);
                            return false;
                        }
                    }
            }

        }
        catch (MalformedURLException mue)
        {
            //LOGGER.info ("OIDC: hasUMPriviledges Invalid URL");
            System.err.println ("Invalid URL");
        }
        catch (IOException ioe)
        {
            //LOGGER.info ("OIDC: hasUMPriviledges I/O Error");
            System.err.println ("I/O Error - " + ioe);
        }

        //LOGGER.info ("OIDC: hasUMPriviledges(false) does not have UM Priviledges");
        return false;
    }

    @Override
    public String getName() {
        return OIDC_AUTH_ATTRIBUTE;
    }

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {

        if (request == null) {
            LOGGER.warn("Unable to authenticate using OIDC because the request object is null.");
            return BAD_ARGS;
        }

        if (request.getAttribute(OIDC_AUTH_ATTRIBUTE) == null) {
            return NO_SUCH_USER;
        }

        String code = (String) request.getParameter("code");
        if (StringUtils.isEmpty(code)) {
            LOGGER.warn("The incoming request has not code parameter");
            return NO_SUCH_USER;
        }

        return authenticateWithOidc(context, code, request);
    }

    private int authenticateWithOidc(Context context, String code, HttpServletRequest request) throws SQLException {

        OidcTokenResponseDTO accessToken = getOidcAccessToken(code);
        if (accessToken == null) {
            LOGGER.warn("No access token retrieved by code");
            return NO_SUCH_USER;
        }

        Map<String, Object> userInfo = getOidcUserInfo(accessToken.getAccessToken());

        String email = getAttributeAsString(userInfo, getEmailAttribute());
        if (StringUtils.isBlank(email)) {
            LOGGER.warn("No email found in the user info attributes");
            return NO_SUCH_USER;
        }

        EPerson ePerson = ePersonService.findByEmail(context, email);
        if (ePerson != null) {
            request.setAttribute(OIDC_AUTHENTICATED, true);
            return ePerson.canLogIn() ? logInEPerson(context, ePerson) : BAD_ARGS;
        }

        // if self registration is disabled, warn about this failure to find a matching eperson
        if (! canSelfRegister()) {
            LOGGER.warn("Self registration is currently disabled for OIDC, and no ePerson could be found for email: {}",
                email);
        }

        return canSelfRegister() ? registerNewEPerson(context, userInfo, email) : NO_SUCH_USER;
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {

        String authorizeUrl = configurationService.getProperty("authentication-oidc.authorize-endpoint");
        String clientId = configurationService.getProperty("authentication-oidc.client-id");
        String clientSecret = configurationService.getProperty("authentication-oidc.client-secret");
        String redirectUri = configurationService.getProperty("authentication-oidc.redirect-url");
        String tokenUrl = configurationService.getProperty("authentication-oidc.token-endpoint");
        String userInfoUrl = configurationService.getProperty("authentication-oidc.user-info-endpoint");
        String[] defaultScopes =
            new String[] {
                "openid", "email", "profile"
            };
        String scopes = String.join(" ", configurationService.getArrayProperty("authentication-oidc.scopes",
            defaultScopes));

        if (isAnyBlank(authorizeUrl, clientId, redirectUri, clientSecret, tokenUrl, userInfoUrl)) {
            LOGGER.error("Missing mandatory configuration properties for OidcAuthenticationBean");

            // prepare a Map of the properties which can not have sane defaults, but are still required
            final Map<String, String> map = Map.of("authorizeUrl", authorizeUrl, "clientId", clientId, "redirectUri",
                redirectUri, "clientSecret", clientSecret, "tokenUrl", tokenUrl, "userInfoUrl", userInfoUrl);
            final Iterator<Entry<String, String>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                final Entry<String, String> entry = iterator.next();

                if (isBlank(entry.getValue())) {
                    LOGGER.error(" * {} is missing", entry.getKey());
                }
            }
            return "";
        }

        try {
            //LOGGER.warn("OIDC: LOGIN ==> " + format(LOGIN_PAGE_URL_FORMAT, authorizeUrl, clientId, scopes, encode(redirectUri, "UTF-8")));
            return format(LOGIN_PAGE_URL_FORMAT, authorizeUrl, clientId, scopes, encode(redirectUri, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            //LOGGER.error(e.getMessage(), e);
            return "";
        }

    }

    private int logInEPerson(Context context, EPerson ePerson) {
        context.setCurrentUser(ePerson);
        return SUCCESS;
    }

    private int registerNewEPerson(Context context, Map<String, Object> userInfo, String email) throws SQLException {
        try {

            context.turnOffAuthorisationSystem();

            EPerson eperson = ePersonService.create(context);

            eperson.setNetid(email);
            eperson.setEmail(email);

            String firstName = getAttributeAsString(userInfo, getFirstNameAttribute());
            if (firstName != null) {
                eperson.setFirstName(context, firstName);
            }

            String lastName = getAttributeAsString(userInfo, getLastNameAttribute());
            if (lastName != null) {
                eperson.setLastName(context, lastName);
            }

            eperson.setCanLogIn(true);
            eperson.setSelfRegistered(true);

            ePersonService.update(context, eperson);
            context.setCurrentUser(eperson);
            context.dispatchEvents();

            return SUCCESS;

        } catch (Exception ex) {
            LOGGER.error("An error occurs registering a new EPerson from OIDC", ex);
            return NO_SUCH_USER;
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private OidcTokenResponseDTO getOidcAccessToken(String code) {
        try {


            //LOGGER.error("OIDC:  Trying to get oidc access token with this code = " + code);
            return oidcClient.getAccessToken(code);
        } catch (Exception ex) {
            //LOGGER.error("An error occurs retriving the OIDC access_token", ex);
            return null;
        }
    }

    private Map<String, Object> getOidcUserInfo(String accessToken) {
        try {
            return oidcClient.getUserInfo(accessToken);
        } catch (Exception ex) {
            //LOGGER.error("An error occurs retriving the OIDC user info", ex);
            return Map.of();
        }
    }

    private String getAttributeAsString(Map<String, Object> userInfo, String attribute) {
        if (isBlank(attribute)) {
            return null;
        }
        return userInfo.containsKey(attribute) ? String.valueOf(userInfo.get(attribute)) : null;
    }

    private String getEmailAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.email", "email");
    }

    private String getFirstNameAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.first-name", "given_name");
    }

    private String getLastNameAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.last-name", "family_name");
    }

    private boolean canSelfRegister() {
        String canSelfRegister = configurationService.getProperty("authentication-oidc.can-self-register", "true");
        if (isBlank(canSelfRegister)) {
            return true;
        }
        return toBoolean(canSelfRegister);
    }

    public OidcClient getOidcClient() {
        return this.oidcClient;
    }

    public void setOidcClient(OidcClient oidcClient) {
        this.oidcClient = oidcClient;
    }

    @Override
    public boolean isUsed(final Context context, final HttpServletRequest request) {
        if (request != null &&
                context.getCurrentUser() != null &&
                request.getAttribute(OIDC_AUTHENTICATED) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canChangePassword(Context context, EPerson ePerson, String currentPassword) {
        return false;
    }

}
