package org.dspace.app.importer;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public abstract class AReflectorImporter<T extends ItemImport> extends
        AConfigurableImporter<T>
{
    protected final int getTotal(String data)
    {
        return getSingleRecordDataImport(data).size();
    }

    protected final List<SingleImportResultBean> processData(String data,
            Community community, Collection collection, EPerson eperson)
    {
        init();
        Set<String> singleRecordData = getSingleRecordDataImport(data);

        List<SingleImportResultBean> results = new ArrayList<SingleImportResultBean>();
        for (String record : singleRecordData)
        {
            Context context = null;
            try
            {
                T crossitem = getImportItem(record);

                context = new Context();
                context.setCurrentUser(eperson);
                int[] specialGroups = AuthenticationManager.getSpecialGroups(
                        context, null);
                for (int groupid : specialGroups)
                {
                    context.setSpecialGroup(groupid);
                }
                Collection targetCollection = getCollection(context, community,
                        collection, crossitem);
                WorkspaceItem witem = WorkspaceItem.create(context,
                        targetCollection, true);
                fitMetatadata(context, witem, crossitem);
                extractMetadata(context, witem,
                        getTargetCollectionFormName(crossitem));
                removeInvalidMetadata(context, witem,
                        getTargetCollectionFormName(crossitem));
                witem.update();
                context.complete();
                SingleImportResultBean result = new SingleImportResultBean(
                        SingleImportResultBean.SUCCESS, witem.getID(),
                        "Tipologia assegnata: " + targetCollection.getName(),
                        getImportIdentifier(record), record);
                results.add(result);

            }
            catch (Exception e)
            {
                SingleImportResultBean result = new SingleImportResultBean(
                        SingleImportResultBean.ERROR, -1, e.getMessage(),
                        record, record);
                results.add(result);
            }
            finally
            {
                if (context != null && context.isValid())
                {
                    context.abort();
                }
            }
        }
        return results;
    }

    protected abstract String getImportIdentifier(String record)
            throws Exception;

    // protected abstract T getImportItem(String record) throws Exception;

    public final T getImportItem(String record) throws Exception
    {
        T t = getInternalImportItem(record);
        t.setSource(t.getClass().getCanonicalName());
        t.setRecord(record);
        return t;
    }

    protected abstract T getInternalImportItem(String record) throws Exception;

    private void fitMetatadata(Context context, WorkspaceItem witem, T crossitem)
            throws SQLException, AuthorizeException, IllegalArgumentException,
            IllegalAccessException, SecurityException, NoSuchMethodException,
            InvocationTargetException
    {
        Item item = witem.getItem();

        Field[] fields = getItemImportClass().getDeclaredFields();
        for (Field field : fields)
        {
            if (field.getType() == String.class)
            {
                Method getter = getItemImportClass().getMethod(
                        "get" + field.getName().substring(0, 1).toUpperCase()
                                + field.getName().substring(1));

                String value = (String) getter.invoke(crossitem);
                addMetadata(item, field.getName(),
                        getTargetCollectionFormName(crossitem), value);
            }
            else if (field.getType() == List.class)
            {
                ParameterizedType pt = (ParameterizedType) field
                        .getGenericType();

                Method getter = getItemImportClass().getMethod(
                        "get" + field.getName().substring(0, 1).toUpperCase()
                                + field.getName().substring(1));

                if (pt.getActualTypeArguments()[0] instanceof GenericArrayType)
                { // nomi di persone
                    List<String[]> values = (List<String[]>) getter
                            .invoke(crossitem);
                    if (values != null)
                    {
                        for (String[] nvalue : values)
                        {
                            String value = nvalue[1] + ", " + nvalue[0];
                            addMetadata(item, field.getName(),
                                    getTargetCollectionFormName(crossitem),
                                    value);
                        }
                    }
                }
                else
                { // metadati ripetibili
                    List<String> values = (List<String>) getter
                            .invoke(crossitem);
                    if (values != null)
                    {
                        for (String value : values)
                        {
                            addMetadata(item, field.getName(),
                                    getTargetCollectionFormName(crossitem),
                                    value);
                        }
                    }
                }
            }
        }
    }

    protected abstract Set<String> getSingleRecordDataImport(String data);

    protected abstract Class<T> getItemImportClass();

}
