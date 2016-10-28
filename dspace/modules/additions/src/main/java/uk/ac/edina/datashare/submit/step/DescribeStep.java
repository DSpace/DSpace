package uk.ac.edina.datashare.submit.step;
        
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import uk.ac.edina.datashare.utils.Consts;
import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.DateUtils;
import uk.ac.edina.datashare.utils.MetaDataUtil;

/**
 * Override default DSpace DescribeStep to add geo-spatial functionality
 */
public class DescribeStep extends org.dspace.submit.step.DescribeStep
{
	private static Logger LOG = Logger.getLogger(DescribeStep.class);
	
    /**
     * @throws ServletException
     */
    public DescribeStep() throws ServletException
    {
        super();
    }

    /*
     * (non-Javadoc)
     * @see org.dspace.submit.step.DescribeStep#doProcessing(org.dspace.core.Context, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.dspace.app.util.SubmissionInfo)
     */
	public int doProcessing(
            Context context,
            HttpServletRequest request,
            HttpServletResponse response,
            SubmissionInfo subInfo)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
		subInfo.getSubmissionItem().setMultipleTitles(true);
		subInfo.getSubmissionItem().setPublishedBefore(true);
		
        // do normal DSpace processing
        int status = super.doProcessing(context, request, response, subInfo);
        
        if(getCurrentPage(request) == 1)
        {
            Item item = subInfo.getSubmissionItem().getItem();

            // get the country names from the hijacked field
            Object ob = request.getParameter(Consts.SPATIAL_HIJACKED_NAME);
            if(ob != null)
            {
                Metadatum[] vals = DSpaceUtils.getHijackedSpatial(item);
                
                // clearing values prevents duplicates
                DSpaceUtils.clearHijackedSpatial(item);
                for(int i = 0; i < vals.length; i++){
                	processSpatial(request, subInfo, item, vals[i].value);
                }            
            }           

            // process embargo date
            status = processEmbargo(request, item, status);
            
            // process time period
            int year = Util.getIntParameter(request, Consts.START_YEAR);

            if(year > 0)
            {
                status = processTimePeriod(request, item, status);
            }
            else
            {
                MetaDataUtil.clearTemporal(item);
            }
            
            // check if data creator is populated
            status = checkDataCreator(request, subInfo, status);
            
            if(status == STATUS_COMPLETE)
            {
                DSpaceUtils.updateCitation(subInfo);
            }
             
            // commit changes
            subInfo.getSubmissionItem().update();
            context.commit();
        }
        
        return status;
    }
    
    /**
     * Process spatial coverage field.
     * @param request HTTP servlet request.
     * @param subInfo DSpace submission info object.
     * @param item DSpace item.
     * @param country Current value.
     * @throws ServletException
     */
    private void processSpatial(
            HttpServletRequest request,
            SubmissionInfo subInfo,
            Item item,
            String country) throws ServletException
    {
        if(country != null && country.length() > 0)
        {
            // add country abbreviation
        	DSpaceUtils.setHijackedSpatial(item, country);
            
            // now find the country full name
            Collection collection = subInfo.getSubmissionItem().getCollection();
            
            try
            {
                DCInputSet inputSet = getInputsReader().getInputs(collection.getHandle());
                DCInput[] inputs = inputSet.getPageRows(
                        getCurrentPage(request) - 1,
                        subInfo.getSubmissionItem().hasMultipleTitles(),
                        subInfo.getSubmissionItem().isPublishedBefore());

                for(int i = 0; i < inputs.length; i++)
                {
                    String element = inputs[i].getElement();
                    String qualifier = inputs[i].getQualifier();

                    if(element != null &&
                            element.equals(Consts.SPATIAL_HIJACKED_ELEMENT) &&
                            qualifier != null
                            && qualifier.equals(Consts.SPATIAL_HIJACKED_IDENTIFIER))
                    {
                        String countryName = inputs[i].getDisplayString("countries", country);

                        // add country fullname
                        DSpaceUtils.setHijackedSpatial(item, countryName);
                    }
                }
            }
            catch(DCInputsReaderException ex){throw new RuntimeException(ex);}
        } 
    }
    
    /**
     * Process embargo value.
     * @param request HTTP servlet request.
     * @param item DSpace item.
     * @param currentStatus Current processing status.
     * @throws ServletException
     */
	private int processEmbargo(
            HttpServletRequest request,
            Item item,
            int currentStatus)
    {
        int status = currentStatus;
        
        String terms[] = ConfigurationManager.getProperty("embargo.field.terms").split("\\.");
        Metadatum dcValues[] = item.getMetadata(terms[0], terms[1], terms[2], Item.ANY);
        
        if(dcValues.length > 0){
        	Date date = null;
            String value = dcValues[0].value;
            
            try{
            	date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(value);
            	//LOG.info(date);
            }
            catch(ParseException ex){
                try{
                	date = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).parse(value);
                	//LOG.info(date);
                }
                catch(ParseException ex2){
                    try{
                    	date = new SimpleDateFormat("yyyy", Locale.ENGLISH).parse(value);
                    	//LOG.info(date);
                    }
                    catch(ParseException ex3){
                    	LOG.info("Invalid embargo: " + value);
                    	status = Consts.INVALID_EMBARGO_STRING;
                    }
                }
            	
            }

            if(status == currentStatus && date != null){
            	GregorianCalendar max = new GregorianCalendar();
            	max.setTime(new Date());
            	
            	if(date.before(max.getTime())){
            		LOG.info("Embargo must be in the future: " + value);
            		status = Consts.EMBARGO_IN_THE_PAST;
            	}
            	
            	max.add(Calendar.YEAR, 5);  
            	
            	if(date.after(max.getTime())){
            		LOG.info("Embargo must not be more than 5 years in the future: " + value);
            		status = Consts.EMBARGO_TOO_FAR_IN_FUTURE;
            	}
            }
        }
        
        if(status != currentStatus){
            addErrorField(request, Consts.EMBARGO_FIELD_NAME);
        }
        
        return status;
    }
    
    /**
     * Process spatial coverage field.
     * @param request HTTP servlet request.
     * @param subInfo DSpace submission info object.
     * @param item DSpace item.
     * @param currentStatus Current processing status.
     * @throws ServletException
     */
    private int processTimePeriod(
            HttpServletRequest request,
            Item item,
            int currentStatus)
    {
        int status = currentStatus;
        
        // build start date
        StringBuffer buf = new StringBuffer();
        
        status = buildDate(
                request,
                buf,
                status,
                Consts.START_YEAR,
                Consts.START_MONTH,
                Consts.START_DAY);
        
        String start = buf.toString();
        
        // now do end date
        int endYear = Util.getIntParameter(request, Consts.END_YEAR);
        String end = null;
        
        if(endYear > 0)
        {
            buf = new StringBuffer();
            status = buildDate(
                request,
                buf,
                status,
                Consts.END_YEAR,
                Consts.END_MONTH,
                Consts.END_DAY);
            
            end = buf.toString();
        }
        
        String timePeriod = DSpaceUtils.encodeTimePeriod(start, end);
            
        // finally set metadata value in DSpace
        MetaDataUtil.setTemporal(item, timePeriod);
            
        // check for errors
        if(end == null)
        {
            status = Consts.TIME_PERIOD_END_MISSING;
        }
        else
        {
            try
            {
                // turn into dates and do some error handling
                Date startDate = DateUtils.getDate(start);
                Date endDate = DateUtils.getDate(end);
                
                if(endDate.before(startDate))
                {
                    status = Consts.TIME_PERIOD_START_BEFORE_END;
                }
                else if(endDate.after(new Date()))
                {
                    status = Consts.TIME_PERIOD_FUTURE_DATE;
                }
            }
            catch(ParseException ex)
            {
                throw new RuntimeException(ex);
            }
        }
        
        return status;
    }
    
    /**
     * Build a date string from DSpace date control.
     * @param request HTTP request.
     * @param buf The buffer to add string to.
     * @param currentStatus Current processing status.
     * @param yearControl Date Year control name.
     * @param monthControl Date Month control name. 
     * @param dayControl Date day control name.
     * @return processing status.
     */
    private int buildDate(
            HttpServletRequest request,
            StringBuffer buf,
            int currentStatus,
            String yearControl,
            String monthControl,
            String dayControl)
    {
        int status = currentStatus;
        
        int iYear = Util.getIntParameter(request, yearControl);
        String yearStr = Integer.toString(iYear);
        
        if(yearStr != null)
        {
            if(iYear < 1000)
            {
                // prefix 0 digit before years before 1000
                for(int i = yearStr.length(); i < 4; i++)
                {
                    yearStr = "0" + yearStr;
                }
            }

            // append year to date string
            buf.append(yearStr);
      
            // now do month
            String strMonth = request.getParameter(monthControl);

            if(strMonth != null && strMonth.length() == 2)
            {
                // as month is a controlled dropdown no error checking is required
                buf.append("-");
                buf.append(strMonth);

                // now do day
                int iDay = Util.getIntParameter(request, dayControl);
                
                if(iDay != -1)
                {
                    String strDay = Integer.toString(iDay);
                    
                    if(strDay.length() == 1)
                    {
                        // prefix single digit with 0
                        strDay = "0" + strDay;
                    }

                    buf.append("-");
                    buf.append(strDay);

                    // now check if day is valid
                    Calendar cal = new GregorianCalendar(iYear, Integer.parseInt(strMonth) - 1, 1);
                    
                    if(iDay > cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    {
                        status = Consts.TIME_PERIOD_INVALID_DAY;
                    }
                }
            }
        }    

        return status;
    }
    
    /**
     * Add a new spatial coverage value to an item's metadata.
     * @param item The item to add the value to. 
     * @param place The new spatial value to be added.
     */
    @SuppressWarnings("unused")
	private void addSpatialValue(Item item, String place)
    {
        // check if country has already been added
        Metadatum[] values = MetaDataUtil.getSpatial(item);
        
        boolean found = false;
        for(int i = 0; i < values.length; i++)
        {
            if(place.equals(values[i].value))
            {
                found = true;
                break;
            }
        }
        
        if(!found && !place.equals("none"))
        {
            // add new entry
            MetaDataUtil.setSpatial(item, place, false);
        }
    }
    
    /**
     * Check whether data creator has a value. If not Publisher should be
     * mandatory.
     * @param request HTTP servlet request.
     * @param subInfo DSpace ingest persistence object.
     * @return status value.
     */
    @SuppressWarnings({ "rawtypes" })
    private int checkDataCreator(HttpServletRequest request, SubmissionInfo subInfo , int currentStatus)
    {
        int status = currentStatus;
        
        // if data creator is defined remove publisher as mandatory field
        if(MetaDataUtil.getCreator(subInfo) != null)
        {
            // if as a result of removing the mandatory publisher there
            // are no errors set status to complete
            if((removePublisher(request) == 0) &&
                    (status == STATUS_MISSING_REQUIRED_FIELDS))
            {
                status = STATUS_COMPLETE;
            }
        }
   
        // don't show publisher as a mandatory field
        // if there is more than one error
        List errors = getErrorFields(request);
        if(errors != null && errors.size() > 1)
        {
            removePublisher(request);
        }
        
        return status;
    }
    
    /**
     * Remove publisher as mandatory field.
     * @param request HTTP servlet request.
     * @return The number of errors left in the error field list.
     */
    @SuppressWarnings({ "rawtypes" })
    private int removePublisher(HttpServletRequest request)
    {
        int size = 0;
        List errors = getErrorFields(request);
        
        if(errors != null)
        {
            for (Object error : errors)
            {
                if(error.toString().equals("dc_publisher"))
                {
                    errors.remove(error);
                    break;
                }
            }
            
            size = errors.size();
        }

        return size;
    }
    
    /**
     * Build citation base on:
     * Data Creator(s): Family Name, Given Name/Initials, Family Name,
     * Given Name/Initials. (Date Accessioned - YEAR ONLY). Title, Time Period
     * [Item Type]. Data Publisher.
     * @param subInfo DSpace ingest persistence object.
     */
    /*@SuppressWarnings("deprecation")
    private void updateCitation(SubmissionInfo subInfo)
    {
        StringBuffer buffer = new StringBuffer(200);
        
        org.dspace.content.DCValue surnames[] = MetaDataUtil.getCreators(subInfo);
        boolean creatorGiven = surnames.length > 0;
        
        if(creatorGiven)
        {
            // add creators
            for(int i = 0; i < surnames.length; i++)
            {
                if(i > 0)
                {
                    buffer.append("; ");
                }
                
                buffer.append(surnames[i].value);
            }
            
            buffer.append(". ");
        }
        else
        {
            appendPublisher(buffer, subInfo);
            buffer.append(" ");
        }

        // add current year 
        buffer.append("(");
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        buffer.append(calendar.get(Calendar.YEAR));
        buffer.append("). ");
        
        // add title
        buffer.append(MetaDataUtil.getTitle(subInfo));
        buffer.append(", ");
        
        // add time period
        String timePeriod = MetaDataUtil.getTemporal(subInfo);
        if(timePeriod != null)
        {
            String dates[] = DSpaceUtils.decodeTimePeriod(timePeriod);

            String from =  dates[0].substring(0, 4);
            String to = dates[1].substring(0, 4);
            
            if(from.equals(to))
            {
                // if both years are the same just use one
                timePeriod = from;
            }
            else
            {
                timePeriod = from + "-" + to;
            }
            
            if(timePeriod != null)
            {
                buffer.append(timePeriod);
                buffer.append(" ");
            }
        }

        // add item type
        buffer.append("[");
        buffer.append(MetaDataUtil.getType(subInfo));
        buffer.append("].");
        
        // append publisher if creator is specified 
        if(creatorGiven)
        {
            appendPublisher(buffer, subInfo);
        }
        
        // finally update citation
        MetaDataUtil.setCitation(subInfo, buffer.toString());
    }*/
    
    /**
     * Helper method for appending publisher to a string buffer.
     * @param buffer The buffer to append the publisher to.
     * @param subInfo DSpace ingest persistence object.
     */
    /*private void appendPublisher(StringBuffer buffer, SubmissionInfo subInfo)
    {
        String publisher = MetaDataUtil.getPublisher(subInfo);
        if(publisher != null)
        {
            buffer.append(" ");
            buffer.append(publisher);
            buffer.append(".");   
        }
    }*/
}
