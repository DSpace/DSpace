package uk.ac.edina.datashare.eperson;

import java.io.IOException;
import java.sql.SQLException;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.AccountManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import uk.ac.edina.datashare.db.DbQuery;

/**
 * DataShare account manager. Specialised for getting the uun of the user.
 */
public class DataShareAccountManager extends AccountManager
{
    /** log4j log */
    private static Logger log = Logger.getLogger(DataShareAccountManager.class);
    
    public static void log(String message)
    {
        log.info("** " + message);
    }
    
    /**
     * 
     * @param context DSpace context
     * @param email Email Address
     * @param uun University user name
     * @throws SQLException
     * @throws IOException
     * @throws MessagingException
     * @throws AuthorizeException
     */
    public static void sendInfo(Context context, String email, String uun)
        throws SQLException, IOException, MessagingException, AuthorizeException
    {
        // See if a registration token already exists for this user
        TableRow rd = DatabaseManager.findByUnique(context, "registrationdata",
                "email", email);

        // If it already exists, just re-issue it
        if (rd == null)
        {
            rd = DatabaseManager.create(context, "RegistrationData");

            rd.setColumn("token", Utils.generateHexKey());

            // don't set expiration date any more
            //            rd.setColumn("expires", getDefaultExpirationDate());
            rd.setColumn("email", email);

            /* DATASHARE code begin */
            rd.setColumn("uun", uun);
            /* DATASHARE code end */

            DatabaseManager.update(context, rd);

            // This is a potential problem -- if we create the callback
            // and then crash, registration will get SNAFU-ed.
            // So FIRST leave some breadcrumbs
            if (log.isDebugEnabled())
            {
                log.debug("Created callback "
                        + rd.getIntColumn("registrationdata_id")
                        + " with token " + rd.getStringColumn("token")
                        + " with email \"" + email + "\"");
            }       
        }
        log.info("-----> " + email + ": " + uun);

        /* DATASHARE code begin */
        //sendEmail(context, email, true, rd);
        // this will kick off the email
        AccountManager.sendRegistrationInfo(context, email);
        /* DATASHARE code end */
    }
    
    /* DATASHARE code begin */
    /**
     * Fetch the registration details for a given token
     * @param context The DSpace context
     * @param token Registration token
     * @return The registration details
     */
    public static RegistrationDetails getRegistrationDetails(
            Context context,
            String token)
    {
        return DbQuery.fetchRegistrationDetails(context, token);
    }
    /* DATASHARE code end */
}
