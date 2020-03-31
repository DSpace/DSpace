## References
_Add references/links to any related tickets or PRs. These may include:_
* Link to [JIRA](https://jira.lyrasis.org/projects/DS/summary) ticket(s), if any
* Link to [REST Contract](https://github.com/DSpace/Rest7Contract) or an open REST Contract PR, if any
* Link to [Angular issue or PR](https://github.com/DSpace/dspace-angular/issues) related to this PR, if any

## Description
Short summary of changes (1-2 sentences).

## Instructions for Reviewers
Please add a more detailed description of the changes made by your PR. At a minimum, providing a bulleted list of changes in your PR is helpful to reviewers.

List of changes in this PR:
* First, ...
* Second, ...

**Include guidance for how to test or review your PR.** This may include: steps to reproduce a bug, screenshots or description of a new feature, or reasons behind specific changes. 

## Checklist
_This checklist provides a reminder of what we are going to look for when reviewing your PR. You need not complete this checklist prior to creating your PR (draft PRs are always welcome). If you are unsure about an item in the checklist, don't hesitate to ask. We're here to help!_

- [ ] My PR is small in size (e.g. less than 1,000 lines of code, not including comments & integration tests). Exceptions may be made if previously agreed upon.
- [ ] My PR passes Checkstyle validation based on the [Code Style Guide](https://wiki.lyrasis.org/display/DSPACE/Code+Style+Guide)
- [ ] My PR includes Javadoc for _all new (or modified) public methods and classes_. It also includes Javadoc for large or complex private methods.
- [ ] My PR passes all tests and includes new/updated Unit or Integration Tests for any bug fixes, improvements or new features. A few reminders about what constitutes good tests:
    * Include tests for different user types, including: (1) Anonymous user, (2) Logged in user (non-admin), and (3) Administrator.
    * Include tests for known error scenarios and error codes (e.g. `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, etc)
    * For bug fixes, include a test that reproduces the bug and proves it is fixed. For clarity, it may be useful to provide the test in a separate commit from the bug fix.
- [ ] If my PR includes new, third-party dependencies (in any `pom.xml`), I've made sure their licenses align with the [DSpace BSD License](https://github.com/DSpace/DSpace/blob/master/LICENSE) based on the [Licensing of Contributions](https://wiki.lyrasis.org/display/DSPACE/Code+Contribution+Guidelines#CodeContributionGuidelines-LicensingofContributions) documentation.
- [ ] If my PR modifies the REST API, I've linked to the REST Contract page (or open PR) related to this change.
