package uk.ac.edina.datashare.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.DCInput;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Field;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.xml.sax.SAXException;

import uk.ac.edina.datashare.utils.Consts;
import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.MetaDataUtil;

/**
 * Override DSpace describe step for adding javascripts.
 */
public class DescribeStep extends org.dspace.app.xmlui.aspect.submission.submit.DescribeStep
{
	//private static Logger LOG = Logger.getLogger(DescribeStep.class);
	
    private static final Message EMABGO_LABEL =
        message("embargo.control.label");
    private static final Message TIME_PERIOD_LABEL =
        message("timeperiod.control.label");
    
    /**
     * Needs default constructor due to throws ServletException clause.
     * @throws ServletException
     */
    public DescribeStep() throws ServletException
    {
        super();
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.aspect.submission.AbstractStep#setup(org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
    @SuppressWarnings({ "rawtypes" })
    public void setup(
            SourceResolver resolver,
            Map objectModel,
            String src,
            Parameters parameters) 
        throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, parameters);
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.aspect.submission.submit.DescribeStep#addBody(org.dspace.app.xmlui.wing.element.Body)
     */
    public void addBody(Body body) throws SAXException, WingException,
        UIException, SQLException, IOException, AuthorizeException
    {  
    	if(DSpaceUtils.getDepositor(submissionInfo).length() == 0){
    		// pre-populate depositor with user name
    		DSpaceUtils.setDepositor(submissionInfo, context.getCurrentUser());
    	}
    	
    	submission.setMultipleTitles(true);
    	submission.setPublishedBefore(true);
    	
        // do DSpace body
        super.addBody(body);

        // add a footnote
        Para para = body.addDivision("footnote").addPara();
        para.addContent("Entries marked with a ");
        para.addHighlight("important-text").addContent("*");
        para.addContent(" are mandatory");
    }
    
    /**
     * Override method to add time period date control before control buttons.
     * @param form The form control is added to.
     */
    public void addControlButtons(List form) throws WingException
    {
        if(getPage() == 1)
        { 
            // add time period
            addTimePeriod(form);
        }
        
        // now really add control buttons
        super.addControlButtons(form);
    }
    
    /**
     * Add embargo option to form.
     * @param form DSpace describe form.
     * @throws WingException
     */
    @SuppressWarnings("unused")
    private void addEmbargo(List form) throws WingException
    {
        /*Composite fullDate = form.addItem().addComposite(
                Consts.EMBARGO_FIELD_NAME, "submit-date");
        Text year =    fullDate.addText(Consts.EMBARGO_YEAR);
        Select month = fullDate.addSelect(Consts.EMBARGO_MONTH);
        Text day =     fullDate.addText(Consts.EMBARGO_DAY);

        // Set up the full field
        fullDate.setLabel(EMABGO_LABEL);
        fullDate.setHelp(message("embargo.control.message"));
        
        setupDateControl(year, month, day, DSpaceUtils.getEmbargoValue(submissionInfo));
        
        if(this.errorFields.contains(Consts.EMBARGO_FIELD_NAME))
        {
            fullDate.addError(message("embargo.control.error"));
        }*/
        
        Text text = form.addItem().addText(Consts.EMBARGO_FIELD_NAME, "submit-date");
        text.setLabel(EMABGO_LABEL);
        text.setHelp(message("embargo.control.message"));
        
        /*if(this.errorFields.contains(Consts.EMBARGO_FIELD_NAME))
        {
            //text.addError(message("embargo.control.error"));
            
        }*/
    }
    
    /**
     * Add Time Period control to form.
     * @param form DSpace describe form.
     * @throws WingException
     */
    private void addTimePeriod(List form) throws WingException
    {
        Item item = form.addItem("time-period", "ds-form-time-period");
  
        Composite fullDate = item.addComposite(
                Consts.TEMPORAL_FIELD_NAME,
                "from-date");
        
        fullDate.setLabel(TIME_PERIOD_LABEL);
        fullDate.setHelp(message("timeperiod.control.help"));
        
        final String DUMMY_LABEL = "dummy-label";
        Text label = fullDate.addText("from-label", DUMMY_LABEL);
        label.setDisabled();
        label.setLabel("From:");
  
        // create date from control
        Text yearFrom =    fullDate.addText(Consts.START_YEAR);
        Select monthFrom = fullDate.addSelect(Consts.START_MONTH);
        Text dayFrom =     fullDate.addText(Consts.START_DAY);
        
        String temporal = MetaDataUtil.getTemporal(submissionInfo);
        String dates[] = null;
        
        if(temporal != null)
        {
            dates = DSpaceUtils.decodeTimePeriod(temporal);
        }
        
        // set up from date control
        setupDateControl(
                yearFrom,
                monthFrom,
                dayFrom,
                (dates == null) ? null : dates[0]);
        
        label = fullDate.addText("to-label", DUMMY_LABEL);
        label.setLabel("To:");
        
        // create date to control
        Text yearTo    = fullDate.addText(Consts.END_YEAR);
        Select monthTo = fullDate.addSelect(Consts.END_MONTH);
        Text dayTo     = fullDate.addText(Consts.END_DAY);
        
        // set up to date control
        setupDateControl(
                yearTo,
                monthTo,
                dayTo,
                (dates == null) ? null : dates[1]);
        
        switch(errorFlag)
        {
            case Consts.TIME_PERIOD_END_MISSING:
            {
                fullDate.addError("Time period must have an end date.");
                break;
            }
            case Consts.TIME_PERIOD_START_BEFORE_END:
            {
                fullDate.addError("End date must be before start.");
                break;
            }
            case Consts.TIME_PERIOD_FUTURE_DATE:
            {
                fullDate.addError("Time period date must be in the past.");
                break;
            }
            case Consts.TIME_PERIOD_INVALID_DAY:
            {
                fullDate.addError("Invalid day.");
                break;
            }
        }
    }
    
    /**
     * Set up DSpace date control with current value
     * @param year Year text field.
     * @param month Month drop down.
     * @param day Day Text field.
     * @param currentValue Value to be displayed in the control, in the form
     * YYY-MM-DD.
     * @throws WingException
     */
    private void setupDateControl(Text year, Select month, Text day, String currentValue) throws WingException
    {
        // first do standard date set up
        setupDateControl(year, month, day);
        
        if(currentValue != null)
        {
            if(currentValue.length() == 10)
            {
                String dayStr = currentValue.substring(8, 10);
                day.setValue(dayStr);
            }
            
            if(currentValue.length() > 5)
            {
                String monthStr = currentValue.substring(5, 7);
                month.setOptionSelected(monthStr);
            }
            
            String yearStr = currentValue.substring(0, 4);
            year.setValue(yearStr);
        }
    }
    
    /**
     * Set up DSpace date control with current value
     * @param year Year text field.
     * @param month Month drop down.
     * @param day Day Text field.
     * @param currentValue Value to be displayed in the control, in a date
     * format.
     * @throws WingException
     */
    /*private void setupDateControl(Text year, Select month, Text day, DCDate currentValue) throws WingException
    {
        // first do standard date set up
        setupDateControl(year, month, day);
        
        // set current value
        if(currentValue != null)
        {
            year.setValue(String.valueOf(currentValue.getYear()));
            month.setOptionSelected(intToString(currentValue.getMonth()));
            
            if(currentValue.getDay() != -1)
            {
                day.setValue(intToString(currentValue.getDay()));
            }
            else
            {
                day.setValue("");
            }
        }
    }*/
    
    /**
     * Set up generic DSpace date control.
     * @param year Year text field.
     * @param month Month drop down.
     * @param day Day Text field.
     * @throws WingException
     */
    private void setupDateControl(Text year, Select month, Text day) throws WingException
    {
        // Setup the year field
        year.setLabel(T_year);
        year.setSize(4, 4);

        // Setup the month field
        month.setLabel(T_month);
        month.addOption(0, "");
        
        for (int i = 1; i < 13; i++) 
        {
            month.addOption(
                    intToString(i),
                    org.dspace.content.DCDate.getMonthName(i, Locale.getDefault()));
        }

        // Setup the day field
        day.setLabel(T_day);
        day.setSize(2, 2);
    }
    
    /**
     * Convert an integer to a string prefixing an value less than 10 with a '0' 
     * @param i The int to convert.
     * @return The string representation.
     */
    private String intToString(int i)
    {
        String str = Integer.toString(i);
        
        if(i < 10)
        {
            str = "0" + str;
        }
        
        return str;
    }
    
    /*
     * (non-Javadoc)
     * @see org.dspace.app.xmlui.aspect.submission.submit.DescribeStep#addReviewSection(org.dspace.app.xmlui.wing.element.List)
     */
    @Override
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        List describeSection = super.addReviewSection(reviewList);
        
        if(getPage() == 1)
        {
            DCDate value = DSpaceUtils.getEmbargoValue(submissionInfo);
            
            describeSection.addLabel("Citation");
            describeSection.addItem(MetaDataUtil.getCitation(submissionInfo));
            
            if(value != null)
            {
                describeSection.addLabel(EMABGO_LABEL);
                describeSection.addItem(value.toString()); 
            }
            
            String temporal = MetaDataUtil.getTemporal(submissionInfo);
            
            if(temporal != null)
            {
                describeSection.addLabel(TIME_PERIOD_LABEL);
                describeSection.addItem(temporal);
            }
        }

        return describeSection;
    }    
    
    /**
     * Datashare specific error messages.
     * @param dcInput
     * @param field
     * @throws WingException
     */
    @Override
    protected void setFieldError(DCInput dcInput, Field field) throws WingException{
        switch(errorFlag){
            case Consts.EMBARGO_IN_THE_PAST:{
                field.addError(message("embargo.control.past"));
                break;
            }
            case Consts.EMBARGO_TOO_FAR_IN_FUTURE:{
                field.addError(message("embargo.control.future"));
                break;
            }
            case Consts.INVALID_EMBARGO_STRING:{
                field.addError(message("embargo.control.invalid"));
                break;
            }
            default:{
                super.setFieldError(dcInput, field);
            }
        }
    }

}
