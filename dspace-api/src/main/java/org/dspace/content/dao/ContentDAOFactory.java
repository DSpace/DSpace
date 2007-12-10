package org.dspace.content.dao;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;

public class ContentDAOFactory
{
    public static ContentDAO getInstance(ContentDAO dao, Context context)
    {
        ContentDAO instantiated = null;

        try
        {
            instantiated = dao.getClass().getConstructor(Context.class).newInstance(context);
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
            System.err.println(e.getCause());
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }

        return instantiated;
    }

    public static <T extends ContentDAO> T prepareStack(Context context,
            Class clazz, T first, T last, String configLine)
    {
        if (first == null || last == null)
        {
            throw new IllegalArgumentException(
                    "DAO stack must contain at least two elements");
        }

        List<T> list = new ArrayList<T>();

        list.add(first);
        if (ConfigurationManager.getBooleanProperty(configLine))
        {
            Object[] hooks = PluginManager.getPluginSequence(clazz);
            for (Object dao : hooks)
            {
                list.add((T) getInstance((T) dao, context));
            }
        }
        list.add(last);

        for (int i = 0; i < list.size() - 1; i++)
        {
            T dao = list.get(i);
            dao.setChild(list.get(i+1));
        }

        return first;
    }
}
