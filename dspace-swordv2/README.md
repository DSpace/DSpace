# DSpace SWORD (v2) README

* Author: Richard Jones
* SWORDv2 Version: 1.0
* Based on: https://github.com/swordapp/JavaServer2.0/

This document describes the DSpace implementation of the SWORD (v2) deposit standard.  This is an extension to the ATOM
Publishing Protocol (APP), which provides a framework to discover deposit targets, and to deposit packaged content into
remote repositories.

For more information see:

http://swordapp.org/sword-v2/sword-v2-specifications/

## Configuration

The SWORD (v2) interface is configured within the `[dspace]/config/modules/swordv2-server.cfg` file.

## Testing

Test example ZIP packages for SWORDv2 are available within the swordapp GitHub repository at: https://github.com/swordapp/JavaClient2.0/tree/master/src/test/resources

These ZIP packages consist of a `mets.xml` file (providing metadata) along with example PDF files.

Testing can be performed using a separately available SWORDv2 Client, or by invoking the sword deposit web service via a
command line tool such as `curl`.  Below are some examples of using `curl` against our demo server (http://demo.dspace.org) SWORDv2 interface.

### Service Documents

An example request to obtain the `servicedocument`:

```
curl -i  http://demo.dspace.org/swordv2/servicedocument --user "dspacedemo+admin@gmail.com"
```

In the above example, we are attempting to login as the Demo Administrator account (dspacedemo+admin@gmail.com)
and will be prompted for the password.

### Deposits

While we've provided a few example deposits below. These examples do not make use of all the available options within the SWORDv2 protocol. For all the nitty gritty details, see the SWORDv2 Profile documentation on "Creating a Resource": http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_creatingresource

#### Depositing a zip package

This is an example of depositing one of the `example.zip` packages (or a similarly structured package). In this example, the `example.zip` file is assumed to be located in the current directory. We are depositing it into a Collection with handle `10673/2` as the Demo Administrator account.

```
curl -i http://demo.dspace.org/swordv2/collection/10673/2 --data-binary "@example.zip" -H "Content-Disposition: filename=example.zip" -H "Content-Type: application/zip" -H "Packaging: http://purl.org/net/sword/package/METSDSpaceSIP" --user "dspacedemo+admin@gmail.com"
```

#### Depositing via an Atom entry

You can also perform a deposit via an Atom entry. This will create a *metadata-only* Item initially, but you can POST files to it after creating the item.

To perform this type of deposit, first you must create file to represent your Atom entry, with the metadata provided in `dcterms`. You can name it whatever you want, but in this example we've named it `entry.xml`. Here's an example:

```
<?xml version='1.0' encoding='UTF-8'?>
<entry xmlns:dcterms="http://purl.org/dc/terms/" xmlns="http://www.w3.org/2005/Atom">
  <dcterms:title>This is a sample book chapter</dcterms:title>
  <dcterms:type>book-chapter</dcterms:type>
  <dcterms:contributor>Jane Doe</dcterms:contributor>
  <dcterms:contributor>John Smith</dcterms:contributor>
  <dcterms:identifier>http://example.com/my-book-chapter</dcterms:identifier>
</entry>
```

Once your `entry.xml` is created, you can deposit it via `curl` similar to below. In this example, the `entry.xml` file is assumed to be located in the current directory. We are depositing it into a Collection with handle `10673/2` as the Demo Administrator account.

```
curl -i http://demo.dspace.org/swordv2/collection/10673/2 --data-binary "@entry.xml" -H "Content-Type: application/atom+xml" --user "dspacedemo+admin@gmail.com"
```

The result will be a new metadata-only Item in DSpace.
