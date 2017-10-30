/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.content.Item;
import org.dspace.content.ItemWrapperIntegration;
import org.dspace.content.MetadataValue;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.util.ItemUtils;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.util.ReflectionUtils;

public final class CrisItemWrapper
        implements MethodInterceptor, ItemWrapperIntegration
{

    private static final Logger log = Logger.getLogger(CrisItemWrapper.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        if (invocation.getMethod().getName().equals("getTypeText"))
        {
            return getTypeText(invocation);
        }

        if (invocation.getMethod().getName()
                .equals("getMetadataWithoutPlaceholder"))
        {
            String schema = "";
            String element = "";
            String qualifier = "";
            String lang = "";
            if (invocation.getArguments().length == 4)
            {
                schema = (String) invocation.getArguments()[0];
                element = (String) invocation.getArguments()[1];
                qualifier = (String) invocation.getArguments()[2];
                lang = (String) invocation.getArguments()[3];

                Metadatum[] basic = (Metadatum[]) invocation.proceed();
                Metadatum[] MetadatumsItem = addEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema, element,
                        qualifier, lang);
                Metadatum[] MetadatumsCris = addCrisEnhancedMetadata(
                        (Item) invocation.getThis(), MetadatumsItem, schema,
                        element, qualifier, lang);

                List<Metadatum> values = new ArrayList<Metadatum>();
                for (Metadatum dcv : MetadatumsCris)
                {
                    if (!StringUtils.equals(dcv.value,
                            MetadataValue.PARENT_PLACEHOLDER_VALUE)
                            && ItemUtils.match(schema, element, qualifier, lang, dcv))
                    {
                        // We will return a copy of the object in case it is
                        // altered
                        Metadatum copy = new Metadatum();
                        copy.element = dcv.element;
                        copy.qualifier = dcv.qualifier;
                        copy.value = dcv.value;
                        copy.language = dcv.language;
                        copy.schema = dcv.schema;
                        copy.authority = dcv.authority;
                        copy.confidence = dcv.confidence;
                        values.add(copy);
                    }
                }
                // Create an array of matching values
                Metadatum[] valueArray = new Metadatum[values.size()];
                valueArray = (Metadatum[]) values.toArray(valueArray);

                return valueArray;
            }

        }
        if (invocation.getMethod().getName().equals("getMetadata"))
        {
            String schema = "";
            String element = "";
            String qualifier = "";
            String lang = "";
            if (invocation.getArguments().length == 4)
            {
                schema = (String) invocation.getArguments()[0];
                element = (String) invocation.getArguments()[1];
                qualifier = (String) invocation.getArguments()[2];
                lang = (String) invocation.getArguments()[3];
            }
            else if (invocation.getArguments().length == 1)
            {
                StringTokenizer dcf = new StringTokenizer(
                        (String) invocation.getArguments()[0], ".");

                String[] tokens = { "", "", "" };
                int i = 0;
                while (dcf.hasMoreTokens())
                {
                    tokens[i] = dcf.nextToken().trim();
                    i++;
                }
                schema = tokens[0];
                element = tokens[1];
                qualifier = tokens[2];

                if ("*".equals(qualifier))
                {
                    qualifier = Item.ANY;
                }
                else if ("".equals(qualifier))
                {
                    qualifier = null;
                }

                lang = Item.ANY;
            }
            if ("item".equals(schema))
            {
                Metadatum[] basic = (Metadatum[]) invocation.proceed();
                Metadatum[] Metadatums = addEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema, element,
                        qualifier, lang);
                return Metadatums;
            }
            else if ("crisitem".equals(schema))
            {
                Metadatum[] basic = (Metadatum[]) invocation.proceed();
                Metadatum[] Metadatums = addCrisEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema, element,
                        qualifier, lang);
                return Metadatums;
            }
            else if (schema == Item.ANY)
            {
                Metadatum[] basic = (Metadatum[]) invocation.proceed();
                Metadatum[] MetadatumsItem = addEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema, element,
                        qualifier, lang);
                Metadatum[] MetadatumsCris = addCrisEnhancedMetadata(
                        (Item) invocation.getThis(), MetadatumsItem, schema,
                        element, qualifier, lang);
                return MetadatumsCris;
            }
        }
        return invocation.proceed();
    }

    private String getTypeText(MethodInvocation invocation)
    {
        String metadata = ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE, "global.item.typing");
        if (StringUtils.isNotBlank(metadata))
        {
            Item item = (Item) invocation.getThis();
            Metadatum[] Metadatums = item.getMetadataByMetadataString(metadata);
            if (Metadatums != null && Metadatums.length > 0)
            {
                for (Metadatum dcval : Metadatums)
                {
                    String value = dcval.value;
                    if (StringUtils.isNotBlank(value))
                    {
                        String valueWithoutWhitespace = StringUtils
                                .deleteWhitespace(value);
                        String isDefinedAsSystemEntity = ConfigurationManager
                                .getProperty(CrisConstants.CFG_MODULE,
                                        "facet.type." + valueWithoutWhitespace
                                                .toLowerCase());
                        if (StringUtils.isNotBlank(isDefinedAsSystemEntity))
                        {
                            return value.toLowerCase();
                        }
                    }
                }
            }
        }
        return Constants.typeText[Constants.ITEM].toLowerCase();
    }

    private Metadatum[] addCrisEnhancedMetadata(Item item, Metadatum[] basic,
            String schema, String element, String qualifier, String lang)
    {
        List<Metadatum> extraMetadata = new ArrayList<Metadatum>();
        if (schema == Item.ANY)
        {
            List<String> crisMetadata = CrisItemEnhancerUtility
                    .getAllCrisMetadata();
            if (crisMetadata != null)
            {
                for (String cM : crisMetadata)
                {
                    extraMetadata.addAll(
                            CrisItemEnhancerUtility.getCrisMetadata(item, cM));

                }
            }
        }
        else if ("crisitem".equals(schema))
        {
            extraMetadata.addAll(CrisItemEnhancerUtility.getCrisMetadata(item,
                    schema + "." + element + "." + qualifier));

        }
        if (extraMetadata.size() == 0)
        {
            return basic;
        }
        else
        {
            Metadatum[] result = new Metadatum[basic.length
                    + extraMetadata.size()];
            List<Metadatum> resultList = new ArrayList<Metadatum>();
            resultList.addAll(Arrays.asList(basic));
            resultList.addAll(extraMetadata);
            result = resultList.toArray(result);
            return result;
        }
    }

    private Metadatum[] addEnhancedMetadata(Item item, Metadatum[] basic,
            String schema, String element, String qualifier, String lang)
    {
        List<Metadatum> extraMetadata = new ArrayList<Metadatum>();

        extraMetadata = ItemEnhancerUtility.getMetadata(item, schema + "."
                + element + (qualifier != null ? "." + qualifier : ""));

        if (extraMetadata == null || extraMetadata.size() == 0)
        {
            return basic;
        }
        else
        {
            Metadatum[] result = new Metadatum[basic.length
                    + extraMetadata.size()];
            List<Metadatum> resultList = new ArrayList<Metadatum>();
            resultList.addAll(Arrays.asList(basic));
            resultList.addAll(extraMetadata);
            result = resultList.toArray(result);
            return result;
        }
    }

    @Override
    public Item getWrapper(Item item)
    {
        AspectJProxyFactory pf = new AspectJProxyFactory(item);
        pf.setProxyTargetClass(true);
        pf.addAdvice(new CrisItemWrapper());
        Item proxy = (Item) (pf.getProxy());
        proxy.extraInfo = item.getExtraInfo();
        Field declaredField = null;
        try
        {

            declaredField = ReflectionUtils.findField(Item.class,
                    "modifiedMetadata");
            boolean accessible = declaredField.isAccessible();
            declaredField.setAccessible(true);
            declaredField.set(proxy, item.isModifiedMetadata());
            declaredField.setAccessible(accessible);

        }
        catch (Exception ex)
        {
            log.error(ex.getMessage(), ex);
        }
        return proxy;
    }
}