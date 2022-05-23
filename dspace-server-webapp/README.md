# DSpace Server Webapp
> This is the new server webapp for DSpace 7 built with Spring Boot, MVC + HATEOAS with a focus on the [JSON HAL format](http://stateless.co/hal_specification.html) ([formal specification](https://tools.ietf.org/html/draft-kelly-json-hal-08))

This webapp uses the following technologies:
- [Spring Boot](https://projects.spring.io/spring-boot/) 
- [Spring MVC](https://spring.io/guides/gs/rest-service/)
- [Spring HATEOAS](http://projects.spring.io/spring-hateoas/)

*Please note that we don't use Spring Data REST* but we mimic as much as possible its architecture and behaviour.
We don't use Spring Data REST as we haven't a spring data layer and we want to provide clear separation between the persistence representation and the REST representation

## How to contribute
Check the infomation available on the DSpace Official Wiki page for the [DSpace 7 Working Group](https://wiki.duraspace.org/display/DSPACE/DSpace+7+UI+Working+Group)

[DSpace 7 REST: Coding DSpace Objects](https://wiki.duraspace.org/display/DSPACE/DSpace+7+REST%3A+Coding+DSpace+Objects)

## How to run
The only tested way right now is to run this webapp inside your IDE (Eclipse). Just create a new Tomcat 8 server and deploy the dspace-server-webapp maven module to it.
> The *dspace.dir* is configured in the *dspace-server-webapp/src/main/resources/application.properties* file
[currently](src/main/resources/application.properties#L25)

> dspace.dir = d:/install/dspace7

## HAL Browser

The modified version of the HAL Browser from https://github.com/mikekelly/hal-browser

We've updated/customized the HAL Browser to integrate better with our authentication system, provide CSRF support, and use a more recent version of its dependencies.

## Packages and main classes
*[org.dspace.app.rest.Application](src/main/java/org/dspace/app/rest/Application.java)* is the spring boot main class it initializes
- the DSpace kernel
- the dspaceContextListener
- the DSpaceWebappServletFilter
- the dspaceRequestContextFilter
- a custom HAL RelProvider for dspace

*[org.dspace.app.rest.RestResourceController](src/main/java/org/dspace/app/rest/RestResourceController.java)* is the controller responsible to handle all the REST requests (so far) delegating the execution to the right repository depending on the requested resource (REST entity)

*[org.dspace.app.rest.model](src/main/java/org/dspace/app/rest/model)* is the package where to put all the classes representing DSpace REST resource. The classes should be named with the name of the DSpace perstent class mainly exposed + the Rest suffix (i.e. ItemRest, CollectionRest, etc.)

*[org.dspace.app.rest.model.hateoas](src/main/java/org/dspace/app/rest/model/hateoas)* contains the classes specific of the HAL implementation. The most important class is org.dspace.app.rest.model.hateoas.DSpaceResource<T> that wrap a DSpaceRest object adding the support for the links and embedded resources

*[org.dspace.app.rest.repository](src/main/java/org/dspace/app/rest/repository)* contains the implementation of the Repository Design pattern for the Rest Objects

*[org.dspace.app.rest.converter](src/main/java/org/dspace/app/rest/converter)* contains the converters from/to DSpace persistent entities (Item, Collection, etc.) to their equivalent REST object (ItemRest, CollectionRest, etc.)
