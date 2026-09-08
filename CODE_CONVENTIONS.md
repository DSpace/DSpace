# DSpace's Java Code Conventions

DSpace has established coding conventions or best practices that all contributions must follow in order
to be accepted.

These code conventions describe the best practices for architecture, design and implementation of DSpace code.
The best practices for formatting your code are defined in our separate [Code Style Guidelines](CODE_STYLE.md).

1. [Enforcement of Guidelines](#enforcement-of-guidelines)
2. [REST API Conventions](#rest-api-conventions)
   1. [Follow endpoint naming conventions (plural nouns, all lowercase)](#follow-endpoint-naming-conventions-plural-nouns-all-lowercase)
   2. [Use standard HTTP verbs and response codes](#use-standard-http-verbs-and-response-codes)
   3. [All new endpoints, new parameters or changes in behavior must be documented in our REST Contract](#all-new-endpoints-new-parameters-or-changes-in-behavior-must-be-documented-in-our-rest-contract)
   4. [Prefer creating new endpoints via `RestRepository` classes instead of `Controller` classes](#prefer-creating-new-endpoints-via-restrepository-classes-instead-of-controller-classes)
   5. [Always define endpoint permissions via `@PreAuthorize` annotations](#always-define-endpoint-permissions-via-preauthorize-annotations)
   6. [Always return `Rest` Data Access Objects (DAOs) from endpoint methods](#always-return-rest-data-access-objects-daos-from-endpoint-methods)
   7. [Always validate `String` inputs received from endpoints](#always-validate-string-inputs-received-from-endpoints)
3. [Java API Conventions](#java-api-conventions)
   1. [When you need an HttpClient, always use `DSpaceHttpClientFactory`](#when-you-need-an-httpclient-always-use-dspacehttpclientfactory)
   2. [When you need an XML parser (e.g. `DocumentBuilderFactory`, `SAXBuilder`, `XMLInputFactory`) always use `XMLUtils`](#when-you-need-an-xml-parser-eg-documentbuilderfactory-saxbuilder-xmlinputfactory-always-use-xmlutils)

## Enforcement of Guidelines

Enforcement of these code conventions is handled in two ways:
* Some guidelines are enforced strictly via static code tools such as Checkstyle. Where possible, we prefer to automate enforcement.
* All guidelines are enforced during code review by the assigned reviewers.

## REST API Conventions

### Follow Endpoint naming conventions (plural nouns, all lowercase)

Follow the REST API endpoint naming conventions as detailed in our [REST Design Principles](https://github.com/DSpace/RestContract#rest-design-principles).
This includes using plural nouns, concatenating multiple words and always using lowercase.

### Use standard HTTP verbs and response codes

Follow the REST API guidelines for [use of HTTP Verbs and Response Codes](https://github.com/DSpace/RestContract#use-of-http-verbs-and-response-codes).
Our REST Contract details the types of endpoints and how each HTTP verb (e.g. `GET`, `POST`, `PUT`, `PATCH`, `DELETE`) should be used.

### All new endpoints, new parameters or changes in behavior must be documented in our REST Contract

Anytime a new endpoint is added, or an existing endpoint is modified (such that the behavior changes or new parameters are added),
these changes must be documented in our separate [RestContract GitHub repository](https://github.com/DSpace/RestContract/).

If you create a Pull Request (PR) that makes endpoint changes, you MUST open a corresponding [REST Contract PR](https://github.com/DSpace/RestContract/pulls)
and link the two PRs together. This allows reviewers to better understand the expected REST API behavior and maintains
our REST Contract documentation.

### Prefer creating new endpoints via `RestRepository` classes instead of `Controller` classes

All REST endpoints are implemented via one of two ways:
* `*RestRepository` class (RECOMMENDED) - These classes are annotated with `@Component` and extend `DSpaceObjectRestRepository` to provide methods to interact with the given Rest Object
* `*Controller` class - These classes are annotated with `@RestController` and implement specific endpoints directly via `@RequestMapping` annotations

When at all possible, we **highly recommend** using `*RestRepository` classes (in `org.dspace.app.rest.repository` package), 
as extending `DSpaceObjectRestRepository` provides overrideable methods for all commons HTTP verbs:
* `findOne()` - `GET` for one object by UUID
* `findAll()` - `GET` for all objects in system (in paginated structure)
* `createAndReturn()` - `POST` to create a new object
* `patch()` - `PATCH` to partially update an object
* `put()` - `PUT` to fully/completely update an object (i.e. replace)
* `delete()` - `DELETE` to remove one object

In situations where a REST endpoint cannot be implemented via a `*RestRepository`, a `*Controller` class can be defined
which explicitly maps the endpoint (and valid HTTP verbs) via the `@RequestMapping` annotation.

### Always define endpoint permissions via `@PreAuthorize` annotations

All REST endpoint methods (in either `*RestRepository` classes to `*Controller` classes) MUST use `@PreAuthorize` annotations
to define the access restrictions on that endpoint.

For example:
```
// This findOne() method can be called by anonymous users
@PreAuthorize("permitAll()")
public BrowseIndexRest findOne(Context context, String name) {

// This findOne() method requires you to be authenticated in the system before you can access it.
@PreAuthorize("hasAuthority('AUTHENTICATED')")
public SubmissionFormRest findOne(Context context, String submitName)

// This findAll() method requires you to be authenticated as a site-wide Admin
@PreAuthorize("hasAuthority('ADMIN')")
public Page<ItemRest> findAll(Context context, Pageable pageable)

// This "put()" method requires you to be authenticated as a user with WRITE permissions on the Item specified in the `uuid` param
@PreAuthorize("hasPermission(#uuid, 'ITEM', 'WRITE')")
protected ItemRest put(... UUID uuid, ...)
```

### Always return `Rest` Data Access Objects (DAOs) from endpoint methods

All REST endpoints MUST return objects via a `*Rest` Data Access Object (DAO) class (in `org.dspace.app.rest.model` package).
These `*Rest` object classes are translated from the (dspace-api's) `DSpaceObject` class via a `*Converter` class (in `org.dspace.app.rest.converter` package).

Essentially, the `*Rest` object defines which data/properties are available for that object type via any REST Endpoint. 
This may NOT include the same properties available to the `DSpaceObject` class.  
* For example, an `org.dspace.app.rest.model.ItemRest` object represents the data that is visible for an `org.dspace.content.Item` object via the REST API. 
* That underlying `Item` object is translated into an `ItemRest` object via the `org.dspace.app.rest.converter.ItemConverter`, 
which just initializes all the properties of `ItemRest` based on the values in the `Item` object.

### Always validate `String` inputs received from endpoints

All `String` properties in a `*Rest` DAO classes MUST include validation.

We recommend using [Jakarta constraint annotations](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/beanvalidation/bean-validation/bean-validation.html) like `@Size`, `@Pattern`, `@Email`
to verify the content in every `String` property.

For example:
```
// This String must be a valid email address
@Email
String email;

// This String must be a valid UUID
@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$")
String item_id;

// This String must be <=1000 characters
@Size(max = 1000)
String message
```


## Java API Conventions

### When you need an HttpClient, always use `DSpaceHttpClientFactory`

When initializing an `HttpClient` you MUST use our `org.dspace.app.client.DSpaceHttpClientFactory` class in order
to support proxy configurations and protect against connection leaks.

**This convention is strictly enforced by Checkstyle via our `checkstyle.xml` configuration file.**

```
// These are all INCORRECT as we are attempting to initialize an HttpClient directly
CloseableHttpClient client = HttpClientBuilder.create().build();
CloseableHttpClient client = HttpClients.createDefault();

// This is CORRECT as we are using DSpaceHttpClientFactory to open an HttpClient
CloseableHttpClient client = DSpaceHttpClientFactory.getInstance().build();
```

### When you need an XML parser (e.g. `DocumentBuilderFactory`, `SAXBuilder`, `XMLInputFactory`) always use `XMLUtils`

All XML parser classes MUST be initialized via the secure methods in our `org.dspace.app.util.XMLUtils` class.
This ensures that each XML parser is fully secured against XXE-style attacks (and similar).

**This convention is strictly enforced by Checkstyle via our `checkstyle.xml` configuration file.**

```
// These are all INCORRECT as we are attempting to initialize these parsers directly
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
SAXBuilder builder = new SAXBuilder();
XMLInputFactory factory = XMLInputFactory.newFactory();

// These are all CORRECT as we are using XMLUtils to obtain a secured copy of the parser
DocumentBuilderFactory factory = XMLUtils.getDocumentBuilderFactory();
SAXBuilder builder = XMLUtils.getSAXBuilder();
XMLInputFactory factory = XMLUtils.getXMLInputFactory();
```