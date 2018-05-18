/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.service;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.ObjectUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.cris.integration.statistics.AStatComponentService;
import org.dspace.app.cris.integration.statistics.CrisStatComponentsService;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.StatSubscriptionViewBean;
import org.dspace.app.cris.statistics.SummaryStatBean;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;

public class StatSubscribeService
{
    private ApplicationService as;

    private SolrLogger statsLogger;
    
    public StatSubscribeService(ApplicationService as)
    {
        this.as = as;
    }

    public void clearAll(EPerson e)
    {
        as.deleteStatSubscriptionsByEPersonID(e.getID());
    }

    public List<StatSubscriptionViewBean> getSubscriptions(Context context,
            EPerson e) throws SQLException
    {
        List<StatSubscription> statSubs = as
                .getAllStatSubscriptionByEPersonID(e.getID());
        List<StatSubscriptionViewBean> result = new ArrayList<StatSubscriptionViewBean>();

        int type = -1;
        String identifier = null;

        DSpaceObject dso = null;
        List<Integer> freqs = new ArrayList<Integer>();
        String objectName = null;
        for (StatSubscription statSub : statSubs)
        {

            DSpaceObject currDSO;
            if (statSub.getTypeDef() < 9)
            {
                currDSO = HandleManager.resolveToObject(context,
                        statSub.getUid());
            }
            else
            {
                currDSO = as.getEntityByUUID(statSub.getUid());
            }

            if (ObjectUtils.equals(currDSO, dso))
            {
                freqs.add(statSub.getFreq());
            }
            else
            {
                if (identifier != null)
                {
                    StatSubscriptionViewBean statViewBean = new StatSubscriptionViewBean();
                    List<Integer> tmpFreqs = new ArrayList<Integer>();
                    tmpFreqs.addAll(freqs);
                    statViewBean.setFreqs(tmpFreqs);
                    statViewBean.setObject(dso);
                    statViewBean.setObjectName(objectName);
                    statViewBean.setId(identifier);
                    statViewBean.setType(type);                    
                    result.add(statViewBean);
                }
                freqs.clear();
                freqs.add(statSub.getFreq());

                type = currDSO.getType();
                identifier = statSub.getUid();
                objectName = currDSO.getName();
                dso = currDSO;

            }
        }
        if (identifier != null)
        {
            StatSubscriptionViewBean statViewBean = new StatSubscriptionViewBean();
            statViewBean.setFreqs(freqs);
            statViewBean.setObject(dso);
            statViewBean.setObjectName(objectName);
            statViewBean.setId(identifier);
            statViewBean.setType(type);            
            result.add(statViewBean);
        }
        return result;
    }

    public void subscribeUUID(EPerson e, String uuid, int[] freqs, int type)
    {
        if (freqs == null || freqs.length == 0)
        {
            unsubscribeUUID(e, uuid);
            return;
        }
        Set<Integer> freqsToAdd = new HashSet<Integer>();
        for (int freq : freqs)
        {
            switch (freq)
            {
            case StatSubscription.FREQUENCY_DAILY:
            case StatSubscription.FREQUENCY_WEEKLY:
            case StatSubscription.FREQUENCY_MONTHLY:
                freqsToAdd.add(freq);
                break;

            default:
                return;
            }
        }

        List<StatSubscription> rpsubs = as
                .getStatSubscriptionByEPersonIDAndUID(e.getID(), uuid);
        for (StatSubscription rpsub : rpsubs)
        {
            // check the freq already subscribed, if present in the new least
            // keep it otherwise we need to remove it
            if (!freqsToAdd.remove(rpsub.getFreq()))
            {
                as.delete(StatSubscription.class, rpsub.getId());
            }
        }

        // add any new freqs
        for (Integer nfreq : freqsToAdd)
        {
            StatSubscription newStatSub = new StatSubscription();
            newStatSub.setUid(uuid);
            newStatSub.setTypeDef(type);
            newStatSub.setFreq(nfreq);
            newStatSub.setEpersonID(e.getID());
            as.saveOrUpdate(StatSubscription.class, newStatSub);
        }
    }

    public void unsubscribeUUID(EPerson e, String uuid)
    {
        List<StatSubscription> rpsubs = as
                .getStatSubscriptionByEPersonIDAndUID(e.getID(), uuid);
        for (StatSubscription rpsub : rpsubs)
        {
            as.delete(StatSubscription.class, rpsub.getId());
        }
    }

    public SummaryStatBean getStatBean(Context context, String uuid,
            int type, int freq, int num) throws SolrServerException,
            SQLException
    {
        if (uuid == null)
        {
            throw new IllegalArgumentException(
                    "UUID not specified");
        }

        SummaryStatBean statBean = new SummaryStatBean();
        statBean.setFreq(freq);
        String dateType = null;
        String dateStart = null;
        String dateEnd = null;
        int gap = 1;
        context.setAutoCommit(false);
        for (int i = 0; i < num; i++)
        {
            switch (freq)
            {
            case StatSubscription.FREQUENCY_DAILY:
                dateType = "DAY";
                dateStart = "-" + (num - i);
                dateEnd = "-" + (num - 1 - i);
                break;

            case StatSubscription.FREQUENCY_WEEKLY:
                Calendar c = Calendar.getInstance();
                int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

                // se domenica allora vale 1
                // se lunedi' vale vale 2, etc
                // sabato 7
                dateType = "DAY";
                gap = 7;
                dateStart = "-"
                        + (7 * (num - i) + (dayOfWeek - 1 > 0 ? dayOfWeek - 1
                                : dayOfWeek - 1 + 7));
                dateEnd = "-"
                        + (7 * (num - i - 1) + (dayOfWeek - 1 > 0 ? dayOfWeek - 1
                                : dayOfWeek - 1 + 7));
                break;

            case StatSubscription.FREQUENCY_MONTHLY:
                dateType = "MONTH";
                dateStart = "-" + (num - i);
                dateEnd = "-" + (num - 1 - i);
                break;
            
            case StatSubscription.FREQUENCY_YEAR:
                dateType = "YEAR";
                dateStart = "-" + (num - i);
                dateEnd = "-" + (num - 1 - i);
                break; 
            default:
                throw new IllegalArgumentException("Unknow frequency " + freq);
            }
            
            Researcher researcher = new Researcher();
            Map<String, Map<String,ObjectCount[]>> selectedObject = new HashMap<String, Map<String,ObjectCount[]>>();
            Map<String, Map<String,ObjectCount[]>> topObject = new TreeMap<String, Map<String,ObjectCount[]>>();
            if (type >= 9)
            {

                ACrisObject object = as.getEntityByUUID(uuid);
                String persistentIdentifier = ResearcherPageUtils
                        .getPersistentIdentifier(object);

                statBean.setObject(object);
                statBean.setObjectName(object.getName());

                statBean.setObjectURL(ConfigurationManager
                        .getProperty("dspace.url")
                        + "/cris/"
                        + object.getPublicPath() + "/" + persistentIdentifier);
                statBean.setStatURL(ConfigurationManager
                        .getProperty("dspace.url")
                        + "/cris/stats/"
                        + object.getPublicPath()
                        + ".html?id="
                        + object.getUuid());

                statBean.setType(object.getType());
                                
                selectedObject = new HashMap<String, Map<String,ObjectCount[]>>();
                
                CrisStatComponentsService statsComponentsService = null;
                switch (type)
                {
                case 9:
                    statsComponentsService = researcher.getRPStatsComponents();
                    break;
                case 10:
                    statsComponentsService = researcher.getPJStatsComponents();
                    break;
                case 11:
                    statsComponentsService = researcher.getOUStatsComponents();
                    break;
                default:
                    statsComponentsService = researcher.getDOStatsComponents();
                    break;
                }
                selectedObject.put(AStatComponentService._SELECTED_OBJECT, statsComponentsService.getSelectedObjectComponent().getStatsViewComponent().queryFacetDate(statsLogger, object, dateType, dateStart, dateEnd, gap, context));
                selectedObject.get(AStatComponentService._SELECTED_OBJECT).putAll(statsComponentsService.getSelectedObjectComponent().getStatsDownloadComponent().queryFacetDate(statsLogger, object, dateType, dateStart, dateEnd, gap, context));                
                for(String key : statsComponentsService.getComponents().keySet()) {
                    IStatsComponent dual = statsComponentsService.getComponents().get(key).getStatsViewComponent();
                    topObject.put(key, dual.queryFacetDate(statsLogger, object, dateType, dateStart, dateEnd, gap, context));
                    dual = statsComponentsService.getComponents().get(key).getStatsDownloadComponent();
                    topObject.get(key).putAll(dual.queryFacetDate(statsLogger, object, dateType, dateStart, dateEnd, gap, context));
                }
            }
            else 
            {
                
                DSpaceObject dso = HandleManager.resolveToObject(context,
                        uuid);
                if (dso == null)
                {
                    throw new IllegalArgumentException("Invalid handle: "
                            + uuid + " DSpace object not found");
                }

                statBean.setObject(dso);
                statBean.setObjectName(dso.getName());
                statBean.setObjectURL(ConfigurationManager
                        .getProperty("dspace.url") + "/handle/" + uuid);
                statBean.setType(dso.getType());
                switch (dso.getType())
                {
                case Constants.ITEM:
                    
                    StatComponentsService serviceItem = researcher.getItemStatsComponents();
                    retrieveResults(dateType, dateStart, dateEnd, gap,
                            selectedObject, topObject, dso, serviceItem, context);
                
                    statBean.setStatURL(ConfigurationManager
                            .getProperty("dspace.url")
                            + "/cris/stats/item.html?handle=" + uuid);
                    break;

                case Constants.COLLECTION:

                    StatComponentsService serviceColl = researcher.getCollectionStatsComponents();
                    retrieveResults(dateType, dateStart, dateEnd, gap,
                            selectedObject, topObject, dso, serviceColl, context);
                  
                    statBean.setStatURL(ConfigurationManager
                            .getProperty("dspace.url")
                            + "/cris/stats/collection.html?handle=" + uuid);
                    break;

                case Constants.COMMUNITY:
                    
                    StatComponentsService serviceComm = researcher.getCommunityStatsComponents();
                    retrieveResults(dateType, dateStart, dateEnd, gap,
                            selectedObject, topObject, dso, serviceComm, context);
                    
                    statBean.setStatURL(ConfigurationManager
                            .getProperty("dspace.url")
                            + "/cris/stats/community.html?handle=" + uuid);
                    break;

                default:
                    throw new IllegalArgumentException("Invalid handle: "
                            + uuid + " DSpace object of type: "
                            + dso.getType() + " is not allowed");
                }
            }
            addStatDataBean(statBean, selectedObject, topObject);
        }

        return statBean;
    }

    private void retrieveResults(String dateType, String dateStart,
            String dateEnd, int gap,
            Map<String, Map<String, ObjectCount[]>> selectedObject,
            Map<String, Map<String, ObjectCount[]>> topObject,
            DSpaceObject dso, StatComponentsService serviceItem, Context context)
            throws SolrServerException
    {
        selectedObject.put(AStatComponentService._SELECTED_OBJECT, serviceItem.getSelectedObjectComponent().queryFacetDate(statsLogger, dso, dateType, dateStart, dateEnd, gap, context));
                
        for(String key : serviceItem.getComponents().keySet()) {
            if(key.equals(AStatComponentService._SELECTED_OBJECT)) {
                continue;
            }
            IStatsComponent dual = serviceItem.getComponents().get(key);
            topObject.put(key, dual.queryFacetDate(statsLogger, dso, dateType, dateStart, dateEnd, gap, context));                        
        }
    }

    public List<StatSubscription> getAllStatSubscriptionByFreq(int freq)
    {
        return as.getAllStatSubscriptionByFreq(freq);
    }

    public List<StatSubscription> getStatSubscriptionByFreqAndType(int freq, int type)
    {
        return as.getStatSubscriptionByFreqAndType(freq,type);
    }
    

    private void addStatDataBean(SummaryStatBean statBean, Map<String,Map<String,ObjectCount[]>> selected,
            Map<String,Map<String,ObjectCount[]>> other)
    {
        if (statBean.getData() == null)
        {
            statBean.setData(new ArrayList<SummaryStatBean.StatDataBean>());
        }
        SummaryStatBean.StatDataBean statDataBean = statBean.new StatDataBean();
        statBean.getData().add(statDataBean);

        Map<String, ObjectCount[]> selectedObjectMap = selected.get(AStatComponentService._SELECTED_OBJECT);
        if(selected!=null && !selected.isEmpty()) {
            statDataBean.setPeriodSelectedView(selectedObjectMap.get("view")[0].getCount());
            statDataBean.setTotalSelectedView(selectedObjectMap.get("view")[1].getCount());            
            if(selectedObjectMap.containsKey("download")) {
                statDataBean.setPeriodSelectedDownload(selectedObjectMap.get("download")[0].getCount());
                statDataBean.setTotalSelectedDownload(selectedObjectMap.get("download")[1].getCount());
            }
            else {
                statDataBean.setShowSelectedObjectDownload(false);
            }
        }
        
        for(String key : other.keySet()) {
            Map<String,ObjectCount[]> topObjectMap = other.get(key);
            List<Long> viewPeriodAndTotalList = new ArrayList<Long>();
            if(topObjectMap.containsKey("view")) {
                viewPeriodAndTotalList.add(topObjectMap.get("view")[0].getCount());
                viewPeriodAndTotalList.add(topObjectMap.get("view")[1].getCount());
            }
            statDataBean.getPeriodAndTotalTopView().put(key, viewPeriodAndTotalList);
            List<Long> downloadPeriodandTotalList = new ArrayList<Long>();
            if(topObjectMap.containsKey("download")) {
                downloadPeriodandTotalList.add(topObjectMap.get("download")[0].getCount());
                downloadPeriodandTotalList.add(topObjectMap.get("download")[1].getCount());
            }            
            statDataBean.getPeriodAndTotalTopDownload().put(key, downloadPeriodandTotalList);
        }
        String dateformatString = null;
        switch (statBean.getFreq())
        {
        case StatSubscription.FREQUENCY_DAILY:
        case StatSubscription.FREQUENCY_WEEKLY:
            dateformatString = "dd-MM-yyyy";
            break;

        case StatSubscription.FREQUENCY_MONTHLY:
            dateformatString = "MMMM yyyy";
            break;

        case StatSubscription.FREQUENCY_YEAR:
            dateformatString = "yyyy";
            break;
            
        }

        // no so good, but we need to retrieve the date object from its string
        // representation
        SimpleDateFormat simpleFormat = new SimpleDateFormat(dateformatString);

        Date date = null;
        try
        {
            date = simpleFormat.parse(selectedObjectMap.get("view")[0].getValue());
            if (statBean.getFreq() == StatSubscription.FREQUENCY_WEEKLY)
            {
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                c.add(Calendar.DAY_OF_MONTH, 7);
                date = c.getTime();
            }
        }
        catch (ParseException e)
        {
            // this should never happen as value is automatically generated
            // using the same simple date format
            // see (RP)SolrLogger.getDateView
        }
        statDataBean.setDate(date);
    }

    public SolrLogger getStatsLogger()
    {
        return statsLogger;
    }

    public void setStatsLogger(SolrLogger statsLogger)
    {
        this.statsLogger = statsLogger;
    }

    public ApplicationService getAs()
    {
        return as;
    }

}
