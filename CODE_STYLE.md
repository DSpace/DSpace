# DSpace's Java Code Style Guidelines

DSpace has established code style / code formatting guidelines that all contributions must follow in order
to be accepted.

These code style guidelines describe the best practices for formatting your code.
The best practices for architecture, design and implementation of your code are defined in our separate [Code Conventions](CODE_CONVENTIONS.md).

* [Enforcement of Guidelines](#enforcement-of-guidelines)
* [Java Style Guide](#java-style-guide)
* [IDE Support](#ide-support)

## Enforcement of Guidelines

Enforcement of these guidelines is handled by [Checkstyle](https://github.com/checkstyle/checkstyle) via the 
`maven-checkstyle-plugin`.

Enforcement occurs when running `mvn install`, but may also be triggered directly via:
```
mvn checkstyle:check
```

DSpace's Checkstyle configuration can be found in the [`checkstyle.xml`](checkstyle.xml) configuration file in the 
root directory.

Code contributions may only suppress these rules if pre-approved. Code may suppress these rules by being added to 
the [`checkstyle-suppressions.xml`](checkstyle-suppressions.xml) configuration file or by using the `@SuppressWarnings` annotation. 


## Java Style Guide

1. 4-space indents for Java and XML. NO TABS ALLOWED.
2. K&R style braces required. Braces are required on all blocks. For example:
   ```
   if (expression) {
      // code
   } else {
      // code
   }
   ```
3. Maximum length of lines is 120 characters (except for long URLs, packages or imports)
4. No trailing spaces allowed (except in comments)
5. Do not use wildcard imports (e.g. `import java.util.*`). Duplicated or unused imports are also not allowed.
6. Write Javadocs for public methods and classes. Keep it short and to the point.
    * Javadoc `@author` tags are optional, but should refer to an individual's name or handle (e.g. GitHub username) when included
7. Tokens should be surrounded by whitespace. (For examples see the [WhitespaceAround]( http://checkstyle.sourceforge.net/config_whitespace.html#WhitespaceAround) checkstyle rule)
   ```
   // These examples are all INCORRECT because whitespace around tokens is missing
   String[]={"one","two","three"}
   for(int i=1; i>1; i++){ }
   
   // These examples are CORRECT because each token is surrounded by whitespace
   String [] = { "one", "two", "three" }
   for (int i = 1; i > 1; i++) { }
   ```
8. Each line of code can only include one statement. This also means each variable declaration must be on its own line, e.g.
   ```
   // This is INCORRECT. Three variables are declared on one line
   String first = "", second = "", third = "";
   
   // This is CORRECT. Each statement is on its own line
   String first = "";
   String second = "";
   String third = "";
   ```
9. No empty `catch` blocks in try/catch. A `catch` block must minimally include a comment as to why the catch is empty, e.g.
   ```
   // This is INCORRECT. The catch block is empty
   try {
     // some code ..
   } catch (Exception e) { 
   }
   
   // This is CORRECT. The catch block has a comment to describe why it is empty
   try {
     // some code ..
   } catch (Exception e) {
     // ignore, this exception is not important
   }
   ```
10. All `switch` statements must include a `default` clause.  Also, each clause in a `switch` must include a `break`, `return`, `throw` or `continue` (no [fall through](https://checkstyle.sourceforge.io/checks/coding/fallthrough.html#FallThrough) allowed), e.g.
   ```
   // This is INCORRECT. Switch doesn't include a "default" and is missing a "break" in first "case"
   switch (myVal) {
     case "one":
        // do something
     case "two":
        // do something else
        break;
   }

   // This is CORRECT. Switch has all necessary breaks and includes a "default" clause
   switch (myVal) {
     case "one":
        // do something
        break;
     case "two":
        // do something else
        break;
     default:
        // do nothing
        break;
   }

   ```
11. Any "utility" classes (a utility class is one that just includes static methods or variables) should have non-public (i.e. private or protected) constructors, e.g.
   ```
   // This is an example class of static constants
   public class Constants {
      public static final String DEFAULT_ENCODING = "UTF-8";
      public static final String ANOTHER_CONSTANT = "Some value";

      // As this is a utility class, it MUST have a constructor that is non-public.
      private Constants() { }
   }
   ```
12. Each source file must contain the required DSpace license header, e.g.
   ```
   /**
    * The contents of this file are subject to the license and copyright
    * detailed in the LICENSE and NOTICE files at the root of the source
    * tree and available online at
    *
    * http://www.dspace.org/license/
    */
   ```

## IDE Support

Most major IDEs (IntelliJ IDEA, Eclipse, NetBeans, VS Code, etc) will provide a plugin to support Checkstyle configurations. 
The plugin usually let you import an existing "checkstyle.xml" configuration to configure your IDE to use and/or validate against that style.

For more information on specific IDE support, see our wiki page docs for this [Code Style Guide](https://wiki.lyrasis.org/spaces/DSPACE/pages/90967266/Code+Style+Guide)
