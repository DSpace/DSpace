/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.logging;

/**
 * Common interface for file records to store information about their origin
 * 
 * @author Michal Josífko
 *
 */
public interface Record
{    
    
    public void setLineNumber(int lineNumber);

    public int getLineNumber();
    
    public void setValid(boolean valid);
    
    public boolean isValid();

}
