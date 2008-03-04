import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


public class Properties2XML
{

       public static void main(String[] args) throws FileNotFoundException, IOException
       {
           
           File dir = new File("src/main/resources");
           
           for(File file : dir.listFiles())
           {
               if(file.getName().endsWith(".properties"))
               {
                   Properties props = new Properties();
                   props.load(new FileInputStream(file));
                   
                   props.storeToXML(new FileOutputStream(new File(file.getAbsolutePath().concat(".xml"))), "");
               }
           }
           
           
           
       }
}
