package org.dspace.app.webui.cris.util;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class MultiformRegexConfigurator
{
    
    public MultiformRegexConfigurator()
    {
    }
    
    private List<RegexMultiform> regex2Decorator4Authority;

    private List<RegexMultiform> regex2Decorator4Value;
    
    private String defaultDecorator;

    public String checkRegex(String value, String authority)
    {
        if (StringUtils.isNotBlank(authority))
        {
            for (RegexMultiform key : regex2Decorator4Authority)
            {
                if (Pattern.matches(key.getRegex(), authority))
                {
                    return key.getDecorator();
                }
            }
        }
        for (RegexMultiform key : regex2Decorator4Value)
        {
            if (Pattern.matches(key.getRegex(), value))
            {
                return key.getDecorator();
            }
        }
        return defaultDecorator;
    }

    public List<RegexMultiform> getRegex2Decorator4Authority()
    {
        return regex2Decorator4Authority;
    }

    public void setRegex2Decorator4Authority(
            List<RegexMultiform> regex2Decorator4Authority)
    {
        this.regex2Decorator4Authority = regex2Decorator4Authority;
    }

    public List<RegexMultiform> getRegex2Decorator4Value()
    {
        return regex2Decorator4Value;
    }

    public void setRegex2Decorator4Value(List<RegexMultiform> regex2Decorator4Value)
    {
        this.regex2Decorator4Value = regex2Decorator4Value;
    }

    public String getDefaultDecorator()
    {
        return defaultDecorator;
    }

    public void setDefaultDecorator(String defaultDecorator)
    {
        this.defaultDecorator = defaultDecorator;
    }
    
    static class RegexMultiform {
        
        private String regex;
        private String decorator;
        
        RegexMultiform()
        {
        }
        
        public String getRegex()
        {
            return regex;
        }
        public void setRegex(String regex)
        {
            this.regex = regex;
        }
        public String getDecorator()
        {
            return decorator;
        }
        public void setDecorator(String decorator)
        {
            this.decorator = decorator;
        }
    }
}
