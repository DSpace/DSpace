/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 * 
 * @author Michal Josífko
 *
 * @param <T>
 */
public class RecordFileLineIterator<T extends Record> implements Iterator<T>
{

    private BufferedReader reader;

    private RecordParser<T> recordParser;

    private int cachedLineNumber;

    private String cachedLine;

    private boolean finished;
    
    public RecordFileLineIterator(InputStreamReader inputReader, RecordParser<T> recordParser)
    {                                 
        this.recordParser = recordParser;
        this.reader = inputReader == null ? null : new BufferedReader(inputReader);
        this.cachedLine = null;
        this.cachedLineNumber = 0;
        this.finished = false;        
        nextRecord();        
    }

    private String nextRecord()
    {        
        if (reader == null || finished)
        {
            finished = true;
            return null;
        }
        StringBuffer buf = new StringBuffer();
        buf.append(cachedLine);
        try
        {
            while (true)
            {
                cachedLine = reader.readLine();
                cachedLineNumber++;
                if (cachedLine == null)
                {
                    close();
                    break;
                }
                if (recordParser.matches(cachedLine))
                {                    
                    break;
                }
                buf.append("\n");
                buf.append(cachedLine);
            }
        }
        catch (IOException ioe)
        {
            close();
            throw new IllegalStateException(ioe.toString());
        }
        return buf.toString();
    }

    @Override
    public boolean hasNext()
    {
        return !finished;
    }

    @Override
    public T next()
    {
        T res = null;
        String s;

        int lineNumber = cachedLineNumber;
        s = nextRecord();
        if (!s.isEmpty())
        {
            res = recordParser.parse(s);
            res.setLineNumber(lineNumber);            
        }

        if (res == null)
        {
            throw new NoSuchElementException();
        }
        return res;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException(
                "Remove unsupported on RecordFileLineIterator");
    }

    public void close()
    {
        finished = true;
        try
        {
            reader.close();
        }
        catch (IOException ioe)
        {
            throw new IllegalStateException(ioe.toString());
        }
    }

}
