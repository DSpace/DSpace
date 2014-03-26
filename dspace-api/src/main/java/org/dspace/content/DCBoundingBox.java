/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 *
 * @author vkopsachilis
 */
public class DCBoundingBox {
    
      /** West*/
    private String west;

    /** East */
    private String east;

      /** South */
    private String south;

    /** North */
    private String north;

    /** Construct clean Bounding Box */
    public DCBoundingBox()
    {
        west = null;
        east = null;
        south = null;
        north = null;
    }

    /**
     * Construct from raw DC value
     * 
     * @param value
     *            value from database
     */
    public DCBoundingBox(String value)
    {
        this();

        if (value != null)
        {
            String boundingBox[]=value.split(",");
            west = boundingBox[0].trim();
            east = boundingBox[1].trim();
            south = boundingBox[2].trim();
            north = boundingBox[3].trim();
        }
    }

    /**
     * Construct from given values
     * 
     * @param w
     *            west
     * @param e
     *            east
     * @param s
     *            south
     * @param n
     *            north
     */
    public DCBoundingBox(String w, String e, String s, String n)
    {
        west = w;
        east = e;
        south = s;
        north = n;
    }

    /**
     * Write as raw DC value
     * 
     * @return the bounding box as they should be stored in the DB
     */
    public String toString()
    {
      
        return (west + "," + east + "," + south + "," + north);
       
    }
    
     /**
     * Return a double array of Bounding Box
     * 
     * @return a Double array with the for Bounding Box element (west, east, south, north)
     */
    public Double[] toDouble()
    {
        
        String[] strCoords=this.toString().split(",");
        
        Double[] coords=new Double[4];
        coords[0]=Double.valueOf(strCoords[0].trim());//get westBound
        coords[1]=Double.valueOf(strCoords[1].trim());//get eastBound
        coords[2]=Double.valueOf(strCoords[2].trim());//get southBound
        coords[3]=Double.valueOf(strCoords[3].trim());//get northBound
        
        return coords;       
    }

    /**
     * Get West - guaranteed non-null
     */
    public String getWest()
    {
        return ((west == null) ? "" : west);
    }

    /**
     * Get East - guaranteed non-null
     */
    public String getEast()
    {
        return ((east == null) ? "" : east);
    }
    
     /**
     * Get South - guaranteed non-null
     */
    public String getSouth()
    {
        return ((south == null) ? "" : south);
    }

    /**
     * Get North - guaranteed non-null
     */
    public String getNorth()
    {
        return ((north == null) ? "" : north);
    }
    
}
