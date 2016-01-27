/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.launcher;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.dspace.app.launcher package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Commands_QNAME = new QName("http://dspace.org/launcher", "commands");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.dspace.app.launcher
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CommandsType }
     * 
     */
    public CommandsType createCommandsType() {
        return new CommandsType();
    }

    /**
     * Create an instance of {@link CommandType }
     * 
     */
    public CommandType createCommandType() {
        return new CommandType();
    }

    /**
     * Create an instance of {@link StepType }
     * 
     */
    public StepType createStepType() {
        return new StepType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CommandsType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://dspace.org/launcher", name = "commands")
    public JAXBElement<CommandsType> createCommands(CommandsType value) {
        return new JAXBElement<CommandsType>(_Commands_QNAME, CommandsType.class, null, value);
    }

}
