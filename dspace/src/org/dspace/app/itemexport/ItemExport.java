package org.dspace.itemexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.Utils;

/*
    issues
        -doesn't handle special characters in metadata
          (needs to turn &'s into &amp;, etc.)
 */

public class ItemExport
{
    /*
      
     */
    public static void main(String [] argv)
        throws Exception
    {
        if( argv.length < 4 )
        {
            printUsage();
            return;
        }

        String typeString   = argv[0];
        String idString     = argv[1];
        String destDirName  = argv[2];
        String seqNumString = argv[3];

        int myID     = Integer.parseInt( idString     );
        int seqStart = Integer.parseInt( seqNumString ); 
        int myType;
        
        if( typeString.equals("ITEM") )
        {
            myType = Constants.ITEM;
        }
        else if( typeString.equals("COLLECTION") )
        {
            myType = Constants.COLLECTION;
        }
        else
        {
            printUsage();
            return;
        }
        
        Context c = new Context();
        c.setIgnoreAuthorization( true );
        
        if( myType == Constants.ITEM )
        {
            // it's only a single item
            Item myItem = Item.find( c, myID );
            
            exportItem( c, myItem, destDirName, seqStart);
        }
        else
        {
            // it's a collection, so do a bunch of items
            Collection myCollection = Collection.find( c, myID );
            
            ItemIterator i = myCollection.getItems();
        }
        
        File destDir = new File( destDirName );

        c.complete();
    }
    
    private static void printUsage()
    {
        System.out.println("Output simple AIPs, given collection or item ID");
        System.out.println("Usage: ITEM|COLLECTION ID dest_dir sequence_number");
        System.out.println("  dest_dir = destination of archive files");
        System.out.println("  sequence_number = 0, or some other number to start naming the archive directories");
        System.out.println("  first item dir is sequence_number, then sequence_number+1, etc.");
    }

    private static void exportItem( Context c, ItemIterator i, String destDirName, int seqStart )
        throws Exception
    {
        int mySequenceNumber = seqStart;
        
        while( i.hasNext() )
        {
            exportItem(c, i.next(), destDirName, mySequenceNumber);
            mySequenceNumber++;
        }
    } 
    
    private static void exportItem( Context c, Item myItem, String destDirName, int seqStart)
        throws Exception
    {
        File destDir = new File( destDirName );
        
        if( destDir.exists() )
        {
            // now create a subdirectory
            File itemDir = new File ( destDir + "/" + seqStart );
            
            if( itemDir.exists() )
            {
                throw new Exception("Directory " + destDir + "/" + seqStart + " already exists!");
            }
            else
            {
                if( itemDir.mkdir() )
                {
                    // make it this far, now start exporting
                    writeMetadata  ( c, myItem, itemDir );
                    writeBitstreams( c, myItem, itemDir );
                }
                else
                {
                    throw new Exception("Error, can't make dir " + itemDir);
                }
            }
        }
        else
        {
            throw new Exception("Error, directory " + destDirName + " doesn't exist!");
        }
    }
    
    // output the item's dublin core into the item directory
    private static void writeMetadata( Context c, Item i, File destDir )
        throws Exception
    {
        File outFile = new File( destDir, "dublin_core.xml" );
     
        System.out.println("Attempting to create file " + outFile);
           
        if( outFile.createNewFile() )
        {
            PrintWriter out = new PrintWriter( new FileWriter( outFile ) );
            
            DCValue dcorevalues[] = i.getDC(Item.ANY, Item.ANY, Item.ANY);
       
            out.println("<dublin_core>");
             
            for(int j = 0; j < dcorevalues.length; j++)
            {
                DCValue dcv = dcorevalues[j];
            
                String output = "  <dcvalue element=\"" + dcv.element + "\" " +
                                "qualifier=\"" + dcv.qualifier + "\">" +
                                dcv.value +
                                "</dcvalue>";
            
                out.println( output );
            }
            out.println("</dublin_core>");
            out.close();
        }
        else
        {
            throw new Exception( "Cannot create dublin_core.xml in " + destDir );
        }
    }
    
    // create both the bitstreams and the manifest file
    private static void writeBitstreams( Context c, Item i, File destDir )
        throws Exception
    {
        File outFile = new File( destDir, "contents" );
        
        if( outFile.createNewFile() )
        {
            PrintWriter out = new PrintWriter( new FileWriter( outFile ) );

            Bundle [] bundles = i.getBundles();
            
            for( int j = 0; j < bundles.length; j++ )
            {
                // currently one bitstream per bundle!
                Bitstream b   = (bundles[j].getBitstreams())[0];
                String myName = b.getName();
                
                // write the manifest file entry
                out.println( myName );
                
                InputStream is = b.retrieve();
                
                File fout = new File( destDir, myName );
                
                if( fout.createNewFile() )
                {
                    FileOutputStream fos = new FileOutputStream(fout);
                    Utils.bufferedCopy( is, fos );
                }
                else
                {
                    throw new Exception("File " + fout + " already exists!" );
                }
            }
            
            // close the manifest file
            out.close();
        } 
        else
        {
            throw new Exception( "Cannot create contents in " + destDir );
        }
    }
}
