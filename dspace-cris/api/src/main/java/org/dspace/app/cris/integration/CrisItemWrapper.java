/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemWrapperIntegration;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

public final class CrisItemWrapper implements MethodInterceptor, ItemWrapperIntegration
{
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable
    {

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
            	StringTokenizer dcf = new StringTokenizer((String) invocation.getArguments()[0], ".");
                
                String[] tokens = { "", "", "" };
                int i = 0;
                while(dcf.hasMoreTokens())
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
            if("item".equals(schema)) {
            	DCValue[] basic = (DCValue[]) invocation.proceed();
                DCValue[] dcvalues = addEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema,
                        element, qualifier, lang);
               return dcvalues;
            }
            else if ("crisitem".equals(schema))
            {
                DCValue[] basic = (DCValue[]) invocation.proceed();
                DCValue[] dcvalues = addCrisEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema,
                        element, qualifier, lang);
                return dcvalues;
            }
            else if (schema == Item.ANY)
            {
            	DCValue[] basic = (DCValue[]) invocation.proceed();
            	DCValue[] dcvaluesItem = addEnhancedMetadata(
                        (Item) invocation.getThis(), basic, schema,
                        element, qualifier, lang);
                DCValue[] dcvaluesCris = addCrisEnhancedMetadata(
                        (Item) invocation.getThis(), dcvaluesItem, schema,
                        element, qualifier, lang);
                return dcvaluesCris;
            }
        }
        return invocation.proceed();
    }

    private DCValue[] addCrisEnhancedMetadata(Item item, DCValue[] basic,
            String schema, String element, String qualifier, String lang)
    {
        List<DCValue> extraMetadata = new ArrayList<DCValue>();
        if (schema == Item.ANY)
        {
            List<String> crisMetadata = CrisItemEnhancerUtility
                    .getAllCrisMetadata();
            if (crisMetadata != null)
            {
                for (String cM : crisMetadata)
                {
                    extraMetadata = CrisItemEnhancerUtility
                            .getCrisMetadata(item, cM);
                }
            }
        }
        else if ("crisitem".equals(schema))
        {
            extraMetadata = CrisItemEnhancerUtility.getCrisMetadata(item,
                    schema + "." + element + "." + qualifier);
        }
        if (extraMetadata.size() == 0)
        {
            return basic;
        }
        else
        {
            DCValue[] result = new DCValue[basic.length
                    + extraMetadata.size()];
            List<DCValue> resultList = new ArrayList<DCValue>();
            resultList.addAll(Arrays.asList(basic));
            resultList.addAll(extraMetadata);
            result = resultList.toArray(result);
            return result;
        }
    }
    
    private DCValue[] addEnhancedMetadata(Item item, DCValue[] basic,
            String schema, String element, String qualifier, String lang)
    {
        List<DCValue> extraMetadata = new ArrayList<DCValue>();
        
          extraMetadata = ItemEnhancerUtility.getMetadata(item,
                    schema + "." + element + "." + qualifier);
        
        if (extraMetadata == null || extraMetadata.size() == 0)
        {
            return basic;
        }
        else
        {
            DCValue[] result = new DCValue[basic.length
                    + extraMetadata.size()];
            List<DCValue> resultList = new ArrayList<DCValue>();
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
        Item proxy = (Item)(pf.getProxy());
        return proxy;
    }
}