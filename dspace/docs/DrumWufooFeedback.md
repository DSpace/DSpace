# DRUM WuFoo Feedback

## Introduction

This page describes the DRUM Wufoo feedback functionality, as of DSpace 7.

The Wufoo feedback functionality replaces the stock DSpace feedback
functionality with a Wufoo form.

## Wufoo Form Functionality

DSpace 6 embedded a Wufoo form directly into the DSpace feedback page.
Performing a similar embed in DSpace 7 proved to be extremely difficult (at
least based on the knowledge available), and not very reliable, as the
embedded JavaScript would not always trigger on page load.

A more reliable mechanism proved to be simply redirecting the user to the
Wufoo form directly, instead of embedding the form in the page. Functionally,
this is equivalent to embedding the form, although is somewhat less
aesthetically pleasing as the user is redirected to a third-party page.

Access to the submitted forms is through the Wufoo website.

## Wufoo Form Configuration

Implementing the Wufoo form functionlity requires the following properties in
the "local.cfg" file:

* wufoo.feedback.formUrl - The "base" URL for the Wufoo form, typically `https://libumd.wufoo.com/forms/`
* wufoo.feedback.formHash - The "hash" for the form, as assigned by Wufoo

The following properties are optional, and their value is the Wufoo API key for
the associated field in the form:

* wufoo.feedback.field.email - the email address of the user (if logged in)
* wufoo.feedback.field.page - the page on which the "Send Feedback" link was clicked.
* wufoo.feedback.field.eperson - the email address of user's DRUM account (if logged in)
* wufoo.feedback.field.agent - the "User Agent" of the browser
* wufoo.feedback.field.date - the date the form was submitted
* wufoo.feedback.field.session - the DSpace session for the user
* wufoo.feedback.field.host - the server hostname
