package ar.edu.unlp.sedici.dspace.submit.step;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.core.Constants;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.joda.time.DateTime;

import org.dspace.core.ConfigurationManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AutomaticEmbargoStepUtility extends AbstractProcessingStep{
	
    private static Logger log = Logger.getLogger(AutomaticEmbargoStepUtility.class);
	
    public int doProcessing(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
            SQLException, AuthorizeException
	{
		try{
		
			Item item = subInfo.getSubmissionItem().getItem();
			Metadatum embargomtd = getEmbargoMetadata(item);
			//check if the metadata value or authority has set the number of days of embargo.
			String days = "0";
			if(embargomtd != null){
				days = ( ConfigurationManager.getBooleanProperty("submission.embargoMetadataField.valueInAuthority",false))? embargomtd.authority : embargomtd.value;
			}	
			//If no embargo was selected, then don't create a policy
			if(days != null && Integer.parseInt(days) > 0){
				AutomaticEmbargoStepUtility.generateEmbargoPolicy(days, context, item, null);
			}else{
				//remove any embargo policy, if exists
				AuthorizeManager.removeAllPoliciesByDSOAndType(context, item, ResourcePolicy.TYPE_CUSTOM);
			}
			
		} catch (NumberFormatException nfe){
			log.error("Error while processing step "+ this.getClass().getName());
		} catch (Exception e){
			log.error(e.getMessage());
		}
		
		return AutomaticEmbargoStepUtility.STATUS_COMPLETE;
	}
	
	
	public int getNumberOfPages(HttpServletRequest request,
			SubmissionInfo subInfo) throws ServletException {
		//Return 'one page' so it can be processed at least once (see method description in abstract class).
		return 1;
	}
	
	/**
	 * This method generate an embargo resource policy from an amount of days passed. It associates the embargo to 
	 * the EPerson owner of the object.
	 * @param days --> the amount of days of embargo duration
	 * @param context --> the actual DSpace Context
	 * @param object --> the DSpace Object over which applies the embargo
	 * @param object --> optionally defines a group to include in the embargo policy
	 **/
	public static void generateEmbargoPolicy(String days, Context context, DSpaceObject object, String groupID) throws NumberFormatException, AuthorizeException, SQLException{
		int groupIDint;
		int daysInt;
		try{
			groupIDint = (groupID != null)? Integer.parseInt(groupID) : 0;
			daysInt = (days != null)? Integer.parseInt(days) : 0;
		}catch (NumberFormatException nfe){
			log.error("Cannot convert the 'days'="+days+" or 'groupID'="+groupID+" to Integer.", nfe);
			throw nfe;
		}
		AuthorizeManager.removeAllPoliciesByDSOAndType(context, object, ResourcePolicy.TYPE_CUSTOM);
		
		//Getting the end embargo Date
		Date embargoEndDate = calculateEmbargoLiftDate(daysInt,object);
		
		String namePolicy = "Embargo";
		String reason = "Embargo automatically applied over " + object.getTypeText() + " with ID: "+ object.getID();
		
		ResourcePolicy rp = AuthorizeManager.createOrModifyPolicy(null,context,namePolicy,groupIDint, null, embargoEndDate, Constants.READ, reason, object);
		log.debug("Automatic embargo policy created. "+reason);
		rp.update();
	}
	
	/**
	 * Get the metadata element configured to generate the automatic embargo.
	 * @return Metadatum relative to the 'submission.embargoMetadataField' configured.
	 * @thows Exception if the 'submission.embargoMetadataField' is not configured in the DSpace config file.
	 **/
	public static Metadatum getEmbargoMetadata(DSpaceObject dso) throws Exception{
		String metadataField = getEmbargoMetadataField();
		if (metadataField != null){
			Metadatum[] embargoMetadatum = dso.getMetadataByMetadataString(metadataField);
			return embargoMetadatum[0];
		}
		else
			throw new Exception("The property 'submission.embargoMetadataField' must be configured in the DSpace configuration file.");
	}
	
	private static String getEmbargoMetadataField(){
		return ConfigurationManager.getProperty("submission.embargoMetadataField");
	}
	
	/**
	 * Calculate the embargo lift date. Then format the date according to the date format accepted in the database.
	 * @param daysOfEmbargo --> the day of embargo lifting
	 * @param dso --> the object over who calculate the embargo lift date
	 * @return Date of end of embargo
	 **/
	public static Date calculateEmbargoLiftDate(int daysOfEmbargo, DSpaceObject dso){
		Date tmpDate;
		Metadatum dateAccessioned = 
				(dso.getMetadataByMetadataString("dc.date.accessioned").length > 0)? dso.getMetadataByMetadataString("dc.date.accessioned")[0] : null;
		if(dateAccessioned != null){	
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			tmpDate = (dateAccessioned != null)? sdf.parse(dateAccessioned.value,new ParsePosition(0)):new Date();
		}else{
			tmpDate = new Date();
		}	
		DateTime dateTime = new DateTime(tmpDate).plusDays(daysOfEmbargo);
		return dateTime.toDate();
	}
}
