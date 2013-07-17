package org.dspace.sword2;

import java.util.HashMap;
import java.util.Map;

public class SimpleDCMetadata
{
    private Map<String, String> dublinCore = new HashMap<String, String>();
    private Map<String, String> atom = new HashMap<String, String>();

    public void addDublinCore(String element, String value)
    {
        this.dublinCore.put(element, value);
    }

    public void addAtom(String element, String value)
    {
        this.atom.put(element, value);
    }

    public Map<String, String> getDublinCore()
    {
        return dublinCore;
    }

    public Map<String, String> getAtom()
    {
        return atom;
    }
}
