# Introduction

Currently to add a command line script launcher.xml needs to be modified. When managing a shared codebase for multiple dspaces this becomes cumbersome because every receiving dspace needs to have the file edited. Being able to add a command line tool by simply adding a jar would be helpful.


# Adding new scripts to the dspace CLI 

Scripts can be added to the script launcher using 2 mechanisms

1. Defining a script in [dspace]/config/launcher.xml
2. Creating a spring bean of type _org.dspace.app.launcher.CommandType_

The advantage of the second mechanism is that a dependency can provide both the script and add it to the launcher without needing to modify the configuration of the receiving party.
The configuration can also be spread over multiple files in multiple modules. This feature leverages the existing dspace spring infrastructure: Create a file named spring-dspace-*-services.xml in the /spring folder of any jar on the classpath and put your beans in and they will be picked up. 



As an example script _org.dspace.authority.UpdateAuthorities_ has been configured to appear in the launcher via spring in the file.
[dspace-api/src/main/resources/spring/spring-dspace-addon-command-services.xml](/dspace-api/src/main/resources/spring/spring-dspace-addon-command-services.xml)

Configuring the bean is roughly the same as defining it in launcher.xml.

For example the script in launcher.xml

```xml
    <command>
        <name>solr-reindex-statistics</name>
        <description>Reindex the Solr-based usage statistics, for example after an upgrade that changes the schema</description>
        <step passuserargs="true">
            <class>org.dspace.util.SolrImportExport</class>
	    <argument>-a</argument>
	    <argument>reindex</argument>
	    <argument>-i</argument>
	    <argument>statistics</argument>
        </step>
    </command>
```

would, defined as a spring bean, look like this

```xml
   <bean class="org.dspace.app.launcher.CommandType">

       <property name="name" value="solr-reindex-statistics"/>
       <property name="description" value="Reindex the Solr-based usage statistics, for example after an upgrade that changes the schema"/>
       <property name="step">
           <list>
               <bean class="org.dspace.app.launcher.StepType">
                   <property name="className" value="org.dspace.util.SolrImportExport"/>
                   <property name="passuserargs" value="true"/>
                   <property name="argument">
                      <list>
                        	    <value>-a</value>
                        	    <value>reindex</value>
                        	    <value>-i</value>
                        	    <value>statistics</value>
                      </list>
                   </property>
               </bean>
           </list>
       </property>
   </bean>
```

Note that in the event of a name collision the definition from spring will likely take precedence. This scenario should be avoided.
