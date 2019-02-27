/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.web.tag;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.integration.ICRISComponent;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.BoxDynamicObject;
import org.dspace.app.cris.model.jdyna.BoxOrganizationUnit;
import org.dspace.app.cris.model.jdyna.BoxProject;
import org.dspace.app.cris.model.jdyna.BoxResearcherPage;
import org.dspace.app.cris.model.jdyna.DecoratorRPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRPTypeNested;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.RPTypeNestedObject;
import org.dspace.app.cris.model.jdyna.TabDynamicObject;
import org.dspace.app.cris.model.jdyna.TabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.TabProject;
import org.dspace.app.cris.model.jdyna.TabResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.cris.dto.AllMonthsStatsDTO;
import org.dspace.core.ConfigurationManager;

import it.cilea.osd.jdyna.components.IBeanSubComponent;
import it.cilea.osd.jdyna.components.IComponent;
import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.ADecoratorTypeDefinition;
import it.cilea.osd.jdyna.model.ANestedObject;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.AccessLevelConstants;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.IPropertiesDefinition;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.web.Box;

public class ResearcherTagLibraryFunctions
{

    private static ApplicationService applicationService;

    /**
     * log4j category
     */
    public static final Log log = LogFactory
            .getLog(ResearcherTagLibraryFunctions.class);

    public static boolean isGroupFieldsHidden(ResearcherPage anagraficaObject,
            String logicGroup)
    {
        return isGroupFieldsHidden(anagraficaObject.getDynamicField(),
                logicGroup);
    }

    private static boolean isGroupFieldsHidden(
            RPAdditionalFieldStorage anagraficaObject, String logicGroup)
    {
        boolean result = true;
        String dspaceProperty = "researcherpage.containables.box.logicgrouped."
                + logicGroup;
        log.debug("Get from configuration additional containables object : "
                + dspaceProperty);
        String confContainables = ConfigurationManager.getProperty(
                CrisConstants.CFG_MODULE, dspaceProperty);
        if (confContainables != null && !confContainables.isEmpty())
        {
            String[] listConfContainables = confContainables.split(",");
            for (String cont : listConfContainables)
            {
                cont = cont.trim();
                for (RPProperty p : anagraficaObject.getAnagrafica4view().get(
                        cont))
                {
                    boolean resultPiece = checkDynamicVisibility(
                            anagraficaObject, p.getTypo().getShortName(), p
                                    .getTypo().getRendering(), p.getTypo());

                    if (resultPiece == false)
                    {
                        return false;
                    }
                }
            }
        }
        return result;

    }

    public static boolean isGroupFieldsHiddenWithStructural(
            ResearcherPage researcher, String logicGroup)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException
    {
        boolean result = true;
        String dspaceProperty = "researcherpage.containables.box.logicgrouped."
                + logicGroup + ".structural";
        log.debug("Get from configuration additional containables object : "
                + dspaceProperty);
        String confContainables = ConfigurationManager.getProperty(
                CrisConstants.CFG_MODULE, dspaceProperty);

        if (confContainables != null && !confContainables.isEmpty())
        {
            String[] listConfContainables = confContainables.split(",");
            for (String cont : listConfContainables)
            {
                cont = cont.trim();
                Method[] methods = researcher.getClass().getMethods();
                Object field = null;
                Method method = null;
                for (Method m : methods)
                {
                    if (m.getName().toLowerCase()
                            .equals("get" + cont.toLowerCase()))
                    {
                        field = m.invoke(researcher, null);
                        method = m;
                        break;
                    }
                }
                if (method.getReturnType().isAssignableFrom(List.class))
                {
                    for (RestrictedField rr : (List<RestrictedField>) field)
                    {

                        if (rr.getVisibility() == 1)
                        {
                            if (rr.getValue() != null
                                    && !rr.getValue().isEmpty())
                            {
                                return false;
                            }
                        }

                    }
                }
                else if (method.getReturnType().isAssignableFrom(String.class))
                {
                    String rr = (String) field;
                    if (rr != null && !rr.isEmpty())
                    {
                        return false;
                    }
                }
                else
                {
                    RestrictedField rr = (RestrictedField) field;
                    if (rr.getVisibility() == 1)
                    {
                        if (rr.getValue() != null && !rr.getValue().isEmpty())
                        {
                            return false;
                        }
                    }
                }
            }
        }

        result = isGroupFieldsHidden(researcher.getDynamicField(), logicGroup);
        return result;

    }


    public static boolean isTabHidden(Object anagrafica,String tabName)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException{

        boolean hidden = true;
        if(anagrafica instanceof ResearcherPage){
            TabResearcherPage tab = applicationService.getTabByShortName(TabResearcherPage.class, tabName);
            List<BoxResearcherPage> boxes = tab.getMask();

            for(Box b: boxes){
                if(b.isUnrelevant()){
                    continue;
                }
                if(!isBoxHidden(anagrafica, b.getShortName())){
                    hidden= false;
                    break;
                }
            }
        }else if(anagrafica instanceof OrganizationUnit){
            TabOrganizationUnit tab = applicationService.getTabByShortName(TabOrganizationUnit.class, tabName);
            List<BoxOrganizationUnit> boxes = tab.getMask();

            for(Box b: boxes){
                if(b.isUnrelevant()){
                    continue;
                }
                if(!isBoxHidden(anagrafica, b.getShortName())){
                    hidden= false;
                    break;
                }
            }
        }else if(anagrafica instanceof Project){
            TabProject tab = applicationService.getTabByShortName(TabProject.class, tabName);
            List<BoxProject> boxes = tab.getMask();

            for(Box b: boxes){
                if(b.isUnrelevant()){
                    continue;
                }
                if(!isBoxHidden(anagrafica, b.getShortName())){
                    hidden= false;
                    break;
                }
            }
        }else if(anagrafica instanceof ResearchObject){
            TabDynamicObject tab = applicationService.getTabByShortName(TabDynamicObject.class, tabName);
            List<BoxDynamicObject> boxes = tab.getMask();

            for(Box b: boxes){
                if(b.isUnrelevant()){
                    continue;
                }
                if(!isBoxHidden(anagrafica, b.getShortName())){
                    hidden= false;
                    break;
                }
            }
        }

        return hidden;

    }

    public static boolean isBoxHidden(ResearcherPage anagrafica, String boxName)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException
    {
        BoxResearcherPage box = applicationService.getBoxByShortName(
                BoxResearcherPage.class, boxName);

        return isBoxHidden(anagrafica, box);

    }

    public static boolean isBoxHidden(Project anagrafica, String boxName)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException
    {
        BoxProject box = applicationService.getBoxByShortName(BoxProject.class,
                boxName);

        return isBoxHiddenInternal(anagrafica, box);

    }

    public static boolean isBoxHidden(Object anagrafica, String boxName)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException
    {
        if (anagrafica instanceof Project)
        {
            BoxProject box = applicationService.getBoxByShortName(
                    BoxProject.class, boxName);
            return isBoxHidden((Project) anagrafica, box);
        }
        if (anagrafica instanceof OrganizationUnit)
        {
            BoxOrganizationUnit box = applicationService.getBoxByShortName(
                    BoxOrganizationUnit.class, boxName);
            return isBoxHidden((OrganizationUnit) anagrafica, box);
        }
        if (anagrafica instanceof ResearchObject)
        {
            BoxDynamicObject box = applicationService.getBoxByShortName(
                    BoxDynamicObject.class, boxName);
            return isBoxHidden((ResearchObject) anagrafica, box);
        }
        BoxResearcherPage box = applicationService.getBoxByShortName(
                BoxResearcherPage.class, boxName);

        return isBoxHidden((ResearcherPage) anagrafica, box);

    }

    public static boolean isBoxHidden(ResearcherPage anagrafica,
            BoxResearcherPage box)
    {

        Researcher researcher = new Researcher();

        Map<String, ICRISComponent> rpComponent = researcher.getRPComponents();
        if (rpComponent != null && !rpComponent.isEmpty())
        {
            for (String key : rpComponent.keySet())
            {

                if (box.getShortName().equals(key))
                {
                    IComponent component = rpComponent.get(key);
                    component.setShortName(box.getShortName());
                    Map<String, IBeanSubComponent> comp = component.getTypes();

                    for (String compp : comp.keySet())
                    {
                        if (component.count(null, comp.get(compp)
                                .getComponentIdentifier(), anagrafica.getId()) > 0)
                        {
                            return false;
                        }
                    }

                }
            }

        }
        return isBoxHiddenInternal(anagrafica.getDynamicField(), box)
                && isBoxHiddenWithStructural(anagrafica, box);
    }

    public static boolean isBoxHidden(Project anagrafica, BoxProject box)
    {
        Researcher researcher = new Researcher();

        Map<String, ICRISComponent> rpComponent = researcher
                .getProjectComponents();
        if (rpComponent != null && !rpComponent.isEmpty())
        {
            for (String key : rpComponent.keySet())
            {

                if (box.getShortName().equals(key))
                {
                    IComponent component = rpComponent.get(key);
                    component.setShortName(box.getShortName());
                    Map<String, IBeanSubComponent> comp = component.getTypes();

                    for (String compp : comp.keySet())
                    {
                        if (component.count(null, comp.get(compp)
                                .getComponentIdentifier(), anagrafica.getId()) > 0)
                        {
                            return false;
                        }
                    }

                }
            }

        }
        return isBoxHiddenInternal(anagrafica, box);
    }

    public static boolean isBoxHidden(ResearchObject anagrafica,
            BoxDynamicObject box)
    {
        Researcher researcher = new Researcher();

        Map<String, ICRISComponent> rpComponent = researcher.getDOComponents();
        if (rpComponent != null && !rpComponent.isEmpty())
        {
            for (String key : rpComponent.keySet())
            {

                if (box.getShortName().equals(key))
                {
                    IComponent component = rpComponent.get(key);
                    component.setShortName(box.getShortName());
                    Map<String, IBeanSubComponent> comp = component.getTypes();

                    for (String compp : comp.keySet())
                    {
                        if (component.count(null, comp.get(compp)
                                .getComponentIdentifier(), anagrafica.getId()) > 0)
                        {
                            return false;
                        }
                    }

                }
            }

        }
        return isBoxHiddenInternal(anagrafica, box);
    }

    public static boolean isBoxHidden(OrganizationUnit anagrafica,
            BoxOrganizationUnit box)
    {
        Researcher researcher = new Researcher();

        Map<String, ICRISComponent> rpComponent = researcher.getOUComponents();
        if (rpComponent != null && !rpComponent.isEmpty())
        {
            for (String key : rpComponent.keySet())
            {

                if (box.getShortName().equals(key))
                {
                    IComponent component = rpComponent.get(key);
                    component.setShortName(box.getShortName());
                    Map<String, IBeanSubComponent> comp = component.getTypes();

                    for (String compp : comp.keySet())
                    {
                        if (component.count(null, comp.get(compp)
                                .getComponentIdentifier(), anagrafica.getId()) > 0)
                        {
                            return false;
                        }
                    }

                }
            }

        }
        return isBoxHiddenInternal(anagrafica, box);
    }

    @Deprecated
    public static <B extends Box<Containable>> boolean isBoxHiddenWithStructural(
            ResearcherPage anagrafica, B box)
    {
        boolean result = true;

        List<IContainable> containables = new LinkedList<IContainable>();

        applicationService.findOtherContainablesInBoxByConfiguration(
                box.getShortName(), containables,
                RPPropertiesDefinition.class.getName());
        for (IContainable decorator : containables)
        {
            String shortName = decorator.getShortName();
            Method method = null;
            Object field = null;

            try
            {
                method = anagrafica.getClass().getDeclaredMethod(
                        "get" + StringUtils.capitalise(shortName), null);
                field = method.invoke(anagrafica, null);
            }
            catch (IllegalArgumentException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            catch (InvocationTargetException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            catch (SecurityException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
            catch (NoSuchMethodException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }

            if (method.getReturnType().isAssignableFrom(List.class))
            {

                for (RestrictedField rr : (List<RestrictedField>) field)
                {

                    if (rr.getVisibility() == 1)
                    {
                        return false;
                    }
                }

            }
            else if (method.getReturnType().isAssignableFrom(String.class))
            {
                return false;
            }
            else
            {
                RestrictedField rr = (RestrictedField) field;
                if (rr.getVisibility() == 1)
                {
                    return false;
                }
            }
        }
        return result;
    }

    public static <TP extends PropertiesDefinition, P extends Property<TP>, B extends Box<Containable>> boolean isBoxHiddenInternal(
            AnagraficaSupport<P, TP> anagrafica, B box)
    {

        boolean result = true;

        List<IContainable> containables = new LinkedList<IContainable>();

        containables.addAll(box.getMask());

        for (IContainable cont : containables)
        {

            if (cont instanceof ADecoratorTypeDefinition)
            {
                ADecoratorTypeDefinition decorator = (ADecoratorTypeDefinition) cont;
                ATypeNestedObject<ANestedPropertiesDefinition> real = ((ATypeNestedObject<ANestedPropertiesDefinition>) decorator
                        .getReal());
                List<ANestedObject> results = applicationService
                        .getNestedObjectsByParentIDAndTypoID(Integer
                                .parseInt(anagrafica.getIdentifyingValue()),
                                (real.getId()), ANestedObject.class);
                boolean resultPiece = true;
                for (ANestedObject object : results)
                {
                    for (ANestedPropertiesDefinition rpp : real.getMask())
                    {
                        resultPiece = checkDynamicVisibility(object,
                                rpp.getShortName(), rpp.getRendering(), rpp);
                        if (resultPiece == false)
                        {
                            return false;
                        }
                    }
                }

            }

            if (cont instanceof ADecoratorPropertiesDefinition)
            {
                ADecoratorPropertiesDefinition decorator = (ADecoratorPropertiesDefinition) cont;
                boolean resultPiece = checkDynamicVisibility(anagrafica,
                        decorator.getShortName(), decorator.getRendering(),
                        (TP) decorator.getReal());
                if (resultPiece == false)
                {
                    return false;
                }
            }

        }

        return result;
    }

    private static <TP extends PropertiesDefinition, P extends Property<TP>> boolean checkDynamicVisibility(
            AnagraficaSupport<P, TP> anagrafica, String shortname,
            AWidget rendering, TP rpPropertiesDefinition)
    {

        for (P p : anagrafica.getAnagrafica4view().get(shortname))
        {
            if (p.getVisibility() == 1)
            {
                return false;
            }
        }

        return true;
    }

    public static <H extends Box<Containable>> boolean isThereMetadataNoEditable(
            String boxName, Class<H> model) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException
    {
        boolean result = false;
        List<IContainable> containables = new LinkedList<IContainable>();
        H box = applicationService.getBoxByShortName(model, boxName);
        containables.addAll(box.getMask());
        applicationService.findOtherContainablesInBoxByConfiguration(boxName,
                containables);

        for (IContainable decorator : containables)
        {

            if (decorator.getAccessLevel().equals(
                    AccessLevelConstants.STANDARD_ACCESS)
                    || decorator.getAccessLevel().equals(
                            AccessLevelConstants.LOW_ACCESS))
            {
                return true;
            }

        }
        return result;

    }

    public static List<AllMonthsStatsDTO> getAllMonthsStats(Object object)
            throws ParseException
    {

        String[][] temp = (String[][]) object;
        List<AllMonthsStatsDTO> result = new LinkedList<AllMonthsStatsDTO>();

        Map<String, List<String>> tempMap = new LinkedHashMap<String, List<String>>();
        boolean foundit = false;
        int countIntegrityMonth = 12;
        /*
         * for (int i = 0; i < temp.length; i++) {
         * 
         * String tempKey = temp[i][0]; String key = tempKey.substring(0, 4); if
         * (!tempMap.containsKey(key)) { tempMap.put(key, new
         * LinkedList<String>()); if (countIntegrityMonth > 0 &&
         * countIntegrityMonth != 12 && i > 0) { while (countIntegrityMonth !=
         * 0) { String check = temp[i - 1][0].substring(0, 4); List<String>
         * array = tempMap.get(check); array.add(0, null);
         * countIntegrityMonth--; } countIntegrityMonth = 12; } }
         * countIntegrityMonth--; List<String> array = tempMap.get(key);
         * array.add(temp[i][1]);
         * 
         * if (countIntegrityMonth > 0 && i == temp.length - 1) { while
         * (countIntegrityMonth != 0) { array.add(null); countIntegrityMonth--;
         * } } if (countIntegrityMonth == 0) { countIntegrityMonth = 12; } }
         */
        for (int i = 0; i < temp.length; i++)
        {
            String tempKey = temp[i][0];
            String year = tempKey.substring(0, 4);
            String month = tempKey.substring(5, 7);
            String key = year;
//            String key = Integer.parseInt(month) < 7 ? String.valueOf(Integer
//                    .parseInt(year) - 1) + "/" + year : year + "/"
//                    + String.valueOf(Integer.parseInt(year) + 1);
            if (!tempMap.containsKey(key))
            {
                final ArrayList<String> list = new ArrayList<String>();
                for (int j = 0; j < countIntegrityMonth; j++)
                    list.add(null);
                tempMap.put(key, list);
            }
            tempMap.get(key).set(Integer.parseInt(month) - 1, temp[i][1]);
        }

        for (String key : tempMap.keySet())
        {
            AllMonthsStatsDTO dto = new AllMonthsStatsDTO();
            dto.setYear(key);
            Integer total = 0;
            List<String> tempToken = tempMap.get(key);
            for (String token : tempToken)
            {
                if (token != null && !token.isEmpty())
                {
                    Integer addendum = Integer.parseInt(token);
                    total += addendum;
                    if (addendum > 0)
                    {
                        foundit = true;
                    }
                }
            }
            if (foundit == true)
            {
                dto.setJan(StringUtils.defaultString(tempToken.get(0), "0"));
                dto.setFeb(StringUtils.defaultString(tempToken.get(1), "0"));
                dto.setMar(StringUtils.defaultString(tempToken.get(2), "0"));
                dto.setApr(StringUtils.defaultString(tempToken.get(3), "0"));
                dto.setMay(StringUtils.defaultString(tempToken.get(4), "0"));
                dto.setJun(StringUtils.defaultString(tempToken.get(5), "0"));
                dto.setJul(StringUtils.defaultString(tempToken.get(6), "0"));
                dto.setAug(StringUtils.defaultString(tempToken.get(7), "0"));
                dto.setSep(StringUtils.defaultString(tempToken.get(8), "0"));
                dto.setOct(StringUtils.defaultString(tempToken.get(9), "0"));
                dto.setNov(StringUtils.defaultString(tempToken.get(10), "0"));
                dto.setDec(StringUtils.defaultString(tempToken.get(11), "0"));
                dto.setTotal(total);
                result.add(dto);
            }
        }
        // Collections.sort(result);
        return result;
    }

    public static int countBoxPublicMetadata(ResearcherPage anagrafica,
            String boxName, Boolean onlyComplexValue)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException
    {
        BoxResearcherPage box = applicationService.getBoxByShortName(
                BoxResearcherPage.class, boxName);

        return countBoxPublicMetadata(anagrafica, box, onlyComplexValue);

    }

    public static int countBoxPublicMetadata(ResearcherPage anagrafica,
            BoxResearcherPage box, boolean onlyComplexValue)
    {
        int result = 0;
        List<IContainable> containables = new LinkedList<IContainable>();

        containables.addAll(box.getMask());

        for (IContainable cont : containables)
        {

            if (cont instanceof DecoratorRPTypeNested)
            {
                DecoratorRPTypeNested decorator = (DecoratorRPTypeNested) cont;
                RPTypeNestedObject real = (RPTypeNestedObject) decorator
                        .getReal();
                List<RPNestedObject> results = applicationService
                        .getNestedObjectsByParentIDAndTypoID(Integer
                                .parseInt(anagrafica.getIdentifyingValue()),
                                (real.getId()), RPNestedObject.class);

                external: for (RPNestedObject object : results)
                {
                    for (RPNestedPropertiesDefinition rpp : real.getMask())
                    {

                        for (RPNestedProperty p : object.getAnagrafica4view()
                                .get(rpp.getShortName()))
                        {
                            if (p.getVisibility() == 1)
                            {
                                result++;
                                break external;
                            }
                        }

                    }
                }

            }

            if (cont instanceof DecoratorRPPropertiesDefinition)
            {
                DecoratorRPPropertiesDefinition decorator = (DecoratorRPPropertiesDefinition) cont;                
                result += countDynamicPublicMetadata(
                        anagrafica.getDynamicField(), decorator.getShortName(),
                        decorator.getRendering(), decorator.getReal(),
                        onlyComplexValue);
            }

        }

        if (!onlyComplexValue)
        {
            containables = new LinkedList<IContainable>();
            applicationService.findOtherContainablesInBoxByConfiguration(
                    box.getShortName(), containables);
            for (IContainable decorator : containables)
            {
                String shortName = decorator.getShortName();
                Method[] methods = anagrafica.getClass().getMethods();
                Object field = null;
                Method method = null;
                for (Method m : methods)
                {
                    if (m.getName().toLowerCase()
                            .equals("get" + shortName.toLowerCase()))
                    {
                        try
                        {
                            field = m.invoke(anagrafica, null);
                        }
                        catch (IllegalArgumentException e)
                        {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        catch (IllegalAccessException e)
                        {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        catch (InvocationTargetException e)
                        {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        method = m;
                        break;
                    }
                }
                if (method.getReturnType().isAssignableFrom(List.class))
                {

                    for (RestrictedField rr : (List<RestrictedField>) field)
                    {

                        if (rr.getVisibility() == 1)
                        {
                            result++;
                        }
                    }

                }
                else if (method.getReturnType().isAssignableFrom(String.class))
                {
                    result++;
                }
                else
                {
                    RestrictedField rr = (RestrictedField) field;
                    if (rr.getVisibility() == 1)
                    {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    public static <TP extends PropertiesDefinition, T extends AnagraficaSupport<? extends Property<TP>, TP>> int countDynamicPublicMetadata(
            T anagrafica, String shortname, AWidget rendering,
            TP rpPropertiesDefinition, boolean onlyComplexValue)
    {
        int result = 0;
        if (!onlyComplexValue)
        {
            for (Property<TP> p : anagrafica.getAnagrafica4view().get(shortname))
            {
                if (p.getVisibility() == 1)
                {
                    result++;
                }
            }
        }

        return result;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        ResearcherTagLibraryFunctions.applicationService = applicationService;
    }

    public static ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public static String rpkey(Integer id)
    {
        return ResearcherPageUtils.getPersistentIdentifier(id,
                ResearcherPage.class);
    }

    public static String criskey(Integer id, Class clazz)
    {
        return ResearcherPageUtils.getPersistentIdentifier(id, clazz);
    }

    public static <TP extends PropertiesDefinition, P extends Property<TP>> List<Containable<P>> sortContainableByComparator(
            List<Containable<P>> containables, String comparatorName)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException
    {

        Comparator comparator = (Comparator) Class.forName(comparatorName)
                .newInstance();
        Collections.sort(containables, comparator);
        return containables;

    }

    public static <TP extends PropertiesDefinition, P extends Property<TP>, C extends Containable<P>> List<Box<C>> sortBoxByComparator(
            List<Box<C>> boxs, String comparatorName)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException
    {
        Comparator comparator = (Comparator) Class.forName(comparatorName)
                .newInstance();
        Collections.sort(boxs, comparator);
        return boxs;

    }

    public static List<RPNestedObject> getResearcherNestedObject(
            Integer researcherID, Integer typoNestedId)
    {
        return applicationService.getNestedObjectsByParentIDAndTypoID(
                researcherID, typoNestedId, RPNestedObject.class);
    }

    public static List<RPNestedObject> getPaginateResearcherNestedObject(
            Integer researcherID, Integer typoNestedId, Integer limit,
            Integer offset)
    {
        return applicationService
                .getNestedObjectsByParentIDAndTypoIDLimitAt(researcherID,
                        typoNestedId, RPNestedObject.class, limit, offset);
    }

    public static List<RPNestedObject> getResearcherNestedObjectByShortname(
            Integer researcherID, String typoNested)
    {
        return applicationService.getNestedObjectsByParentIDAndShortname(
                researcherID, typoNested, RPNestedObject.class);
    }

    public static String encode(String value, String charset)
            throws UnsupportedEncodingException
    {
        return URLEncoder.encode(value, charset);
    }   

    public static <A extends ACrisNestedObject> List<A> getCrisNestedObjectByShortname(
            Integer id, String typoNested, Class<A> nestedClass)
    {
        return applicationService.getNestedObjectsByParentIDAndShortname(id,
                typoNested, nestedClass);
    }
    
    public static <TP extends PropertiesDefinition, P extends Property<TP>> TP getPropertiesDefinition(Class<TP> clazz, String shortName)
    {
        return applicationService.findPropertiesDefinitionByShortName(clazz, shortName);
    }
    
    public static <TP extends PropertiesDefinition, P extends Property<TP>> String getPropertyDefinitionLabel(String specificPart, String shortName) throws ClassNotFoundException
    {
        TP pd = null;
        Class<TP> clazz = null;
        switch (specificPart)
        {
        case "rp":
            clazz = (Class<TP>)RPPropertiesDefinition.class;
            break;
        case "project":
            clazz = (Class<TP>)ProjectPropertiesDefinition.class;
            break;
        case "ou":
            clazz = (Class<TP>)OUPropertiesDefinition.class;
            break;        
        default:
            clazz = (Class<TP>)DynamicPropertiesDefinition.class;
            break;
        }
        pd = applicationService.findPropertiesDefinitionByShortName(clazz, shortName);
        if(pd != null) {
            return pd.getLabel();
        }
        return null;
    }
    
    public static Object getPropertyDefinitionI18N(
            Object pd, String locale)
    {

        IContainable ipd = (IContainable) pd;
        String shortname = ipd.getShortName() + "_" + locale;
        IContainable pdLocalized = applicationService
                .findContainableByDecorable(ipd.getClass(), shortname);

        if (pdLocalized != null)
        {
            return (IPropertiesDefinition) PropertyDefinitionI18NWrapper
                    .getWrapper((IPropertiesDefinition) pdLocalized, locale);
        }
        return (IPropertiesDefinition) PropertyDefinitionI18NWrapper
                .getWrapper((IPropertiesDefinition) ipd, locale);
    }
}
