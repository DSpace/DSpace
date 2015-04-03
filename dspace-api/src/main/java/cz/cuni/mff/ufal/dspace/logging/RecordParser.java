/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.logging;

public interface RecordParser<T>
{
    public T parse(String s);

    public boolean matches(String s);
}
