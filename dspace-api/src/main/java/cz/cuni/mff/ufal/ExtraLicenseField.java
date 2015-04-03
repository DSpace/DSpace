/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;


public enum ExtraLicenseField {
	
	/* 
	 * For each item of this enum define two keys in messages.xml
	 * one with the prefix xmlui.ExtraLicenseField.LicenseForm.[ENUM_ITEM] for LicenseForm labels 
	 * second with the prefix xmlui.ExtraLicenseField.submission.[ENUM_ITEM] to display labels in LicenseStep of submission
	 * 
	 */
	
	SEND_TOKEN (null, false, "", new SendEmailAction()),	
	NAME (new RequiredValidator(), true, "Name is required."),
	DOB (new RequiredValidator(), true, "Date of Birth is required."),
	ADDRESS (new RequiredValidator(), true, "Address is required."),
	COUNTRY (new RequiredValidator(), true, "Country is required."),
	EXTRA_EMAIL (new EmailValidator(), true, "Please enter a valid email address.", null),
	ORGANIZATION (null, true, "Please enter organization.");
	
	private Validator validator = null;
	private Action action = null;
	private boolean metadata = true;
	private String errorMsg = "";
	
	private ExtraLicenseField(Validator validator) {
		this.validator = validator;
	}
	
	private ExtraLicenseField(Validator validator, boolean metadata) {
		this.validator = validator;
		this.metadata = metadata;
	}


	private ExtraLicenseField(Validator validator, boolean metadata, String errorMsg) {
		this.validator = validator;
		this.metadata = metadata;
		this.errorMsg = errorMsg;
	}
	
	private ExtraLicenseField(Validator validator, boolean metadata, String errorMsg, Action action) {
		this.validator = validator;
		this.metadata = metadata;
		this.errorMsg = errorMsg;
		this.action = action;
	}

	public boolean performAction(Map objectModel) throws Exception {
		if(this.action!=null) {
			return this.action.perform(objectModel);
		}
		return true;
	}

	public boolean isMetadata() {
		return this.metadata;
	}

	public boolean validate(String value) {
		if(this.validator != null) {
			return this.validator.validate(value);
		}
		return true;
	}
	
	public String getErrorMessage() {
		return this.errorMsg;
	}
	
}


interface Validator {
	
	public boolean validate(String value);
	
}

interface Action {
	
	public boolean perform(Map objectModel) throws Exception;
	
}


class SendEmailAction implements Action {

	private static final Logger log = cz.cuni.mff.ufal.Logger.getLogger(ExtraLicenseField.class);
	
	@Override
	public boolean perform(Map objectModel) throws Exception {
		Context context = (Context)objectModel.get("context");
		int bitID = (Integer)objectModel.get("bitstreamId");
		String handle = (String)objectModel.get("handle");
		String ip = (String)objectModel.get("ip");
		Map<String, String> extraMetadata = (Map<String, String>)objectModel.get("extraMetadata");
        boolean allzip = (Boolean)objectModel.get("allzip");
		
		EPerson eperson = context.getCurrentUser();
        Locale locale = context.getCurrentLocale();
		Bitstream bitstream = null;
		if(allzip) {
			Item item = (Item) HandleManager.resolveToObject(context, handle);
			Bundle[] originals = item.getBundles("ORIGINAL");
			for (Bundle original : originals) {
				for(Bitstream b : original.getBitstreams()) {
					bitstream = b; // take any of the bitstream
					break;
				}
				if(bitstream != null) break;
			}
		} else {
			bitstream = Bitstream.find(context, bitID);
		}
        
        String token = null;
        
        IFunctionalities manager = DSpaceApi.getFunctionalityManager();
        manager.openSession();
        token = manager.getToken(eperson.getID(), bitstream.getID());

        String base = ConfigurationManager.getProperty("dspace.url");
		StringBuffer link = null;
		if(allzip) {
			link = new StringBuffer().append(base)
					  .append(base.endsWith("/") ? "" : "/")
					  .append("handle/")
					  .append(bitstream.getParentObject().getHandle())
					  .append("/allzip?");
		} else {
			link = new StringBuffer().append(base)
					  .append(base.endsWith("/") ? "" : "/")
					  .append("bitstream/handle/")
					  .append(bitstream.getParentObject().getHandle())
					  .append("/")
					  .append(URLEncoder.encode(bitstream.getName(), "UTF8"))					
					  .append("?sequence=").append(bitstream.getSequenceID())
					  .append("&");
		}

		Email email2User = Email.getEmail(I18nUtil.getEmailFilename(locale, "download_link"));
        email2User.addRecipient(eperson.getEmail());
        if(extraMetadata.containsKey(ExtraLicenseField.EXTRA_EMAIL.toString())) {
        	String exEmail = extraMetadata.get(ExtraLicenseField.EXTRA_EMAIL.toString());
        	if(exEmail!=null && !exEmail.isEmpty() && !exEmail.equals(eperson.getEmail())) {
        		email2User.addRecipient(exEmail);
        	}
        }
        if(allzip) {
        	email2User.addArgument("for requested files");
        } else {
        	email2User.addArgument(bitstream.getName());
        }
		email2User.addArgument(link.toString() + "dtoken=" + token);
        List<LicenseDefinition> licenses = manager.getLicensesToAgree(eperson.getID(), bitstream.getID());
		for(LicenseDefinition license : licenses) {
			email2User.addArgument(license.getDefinition());
		}		
		
    	Email email2Admin = Email.getEmail(I18nUtil.getEmailFilename(locale, "download_link_admin"));        
        String ccAdmin = ConfigurationManager.getProperty("lr", "lr.download.email.cc");
        if(ccAdmin != null && !ccAdmin.isEmpty()){
        	email2Admin.addRecipient(ccAdmin);
            if(allzip) {
            	email2Admin.addArgument("all files requested");
            } else {
            	email2Admin.addArgument(bitstream.getName());
            }        	
        	// admin will not receive the token        	
        	email2Admin.addArgument(link.toString());
    		for(LicenseDefinition license : licenses) {
    			email2Admin.addArgument(license.getDefinition());
    		}        	
        	email2Admin.addArgument(eperson.getFullName());
        	email2Admin.addArgument(eperson.getEmail());
        	StringBuffer exdata = new StringBuffer();
        	for(String key : extraMetadata.keySet()) {
        		exdata.append(key).append(": ").append(extraMetadata.get(key).toString());
        		exdata.append(", ");
        	}
        	exdata.append("IP: ").append(ip);
        	email2Admin.addArgument(exdata.toString());
        }
		        
        try{            
			email2User.send();
			email2Admin.send();						
        } catch (MessagingException e){
        	log.error(e.toString());
        	manager.closeSession();
        	return false;
        }
        manager.closeSession();
		return true;
	}
	
}

class EmailValidator implements Validator {

	@Override
	public boolean validate(String value) {
		try{ 
			InternetAddress exEmail = new InternetAddress(value);
			exEmail.validate();
		} catch(AddressException e) {
			return false;
		}
		return true;
	}
	
}


class RequiredValidator implements Validator {

	@Override
	public boolean validate(String value) {
		if(value==null || value.trim().isEmpty()) {
			return false;
		}
		return true;
	}
	
}
