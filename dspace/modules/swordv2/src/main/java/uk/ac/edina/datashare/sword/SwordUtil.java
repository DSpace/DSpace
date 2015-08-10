package uk.ac.edina.datashare.sword;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.sword2.DSpaceSwordException;

import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.MetaDataUtil;
import uk.ac.edina.datashare.utils.MetadataChecker;

public class SwordUtil {
    private static final Logger LOG = Logger.getLogger(SwordUtil.class);
    
    /**
     * Complete a datashare sword deposit.
     * @param context DSpace context.
     * @param item DSpace item.
     * @throws DSpaceSwordException
     */
    public static void complete(Context context, Item item) throws DSpaceSwordException
    {
        try{
            DSpaceUtils.completeDeposit(context, item, BinaryIngester.class.toString());
            
            // send depositor agreement email
            Email mail = Email.getEmail(
                    I18nUtil.getEmailFilename(context.getCurrentLocale(), "sword_deposit"));
            mail.addArgument(MetaDataUtil.getTitle(item));
            EPerson user = context.getCurrentUser();
            String url = ConfigurationManager.getProperty("dspace.url") +
                    "/deposit-agree?item=" + item.getID();
            mail.addArgument(url);
            mail.addRecipient(user.getEmail());
            mail.send();
        }
        catch(Exception ex){
            LOG.warn("Problem with deposit: " + ex.getClass() + " : " + ex.getMessage());
            throw new DSpaceSwordException(ex);
        }
    }
    
    /**
     * Is metadata valid in sword item?
     * @param context DSpace context.
     * @param item DSpace item.
     * @throws PackageValidationException
     * @throws SQLException
     */
    public static void isMetadataValid(Context context, Item item) throws PackageValidationException, SQLException
    {
        try{
            new MetadataChecker().isValid(context, item);
        }
        catch(IllegalStateException ex){
            throw new PackageValidationException(ex);
        }
    }
}
