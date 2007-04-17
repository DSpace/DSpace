import java.io.File;

public class Test {
  public static void main(String[] args) throws Exception {
    File file = new File("/a/b/cdefgh");
    File filegz = new File(file.getParent(), file.getName() + ".gz");
    System.out.println(file);
    System.out.println(filegz);
  }
  
}



