## References
_Add references/links to any related issues or PRs. These may include:_
* Fixes #`issue-number` (if this fixes an issue ticket)
* Related to DSpace/RestContract#`pr-number`  (if a corresponding REST Contract PR exists)

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
- [ ] My PR passes Checkstyle validation based on the [Code Style Guide](https://wiki.lyrasis.org/display/DSPACE/Code+Style+Guide).
- [ ] My PR includes Javadoc for _all new (or modified) public methods and classes_. It also includes Javadoc for large or complex private methods.
- [ ] My PR passes all tests and includes new/updated Unit or Integration Tests based on the [Code Testing Guide](https://wiki.lyrasis.org/display/DSPACE/Code+Testing+Guide).
- [ ] If my PR includes new libraries/dependencies (in any `pom.xml`), I've made sure their licenses align with the [DSpace BSD License](https://github.com/DSpace/DSpace/blob/main/LICENSE) based on the [Licensing of Contributions](https://wiki.lyrasis.org/display/DSPACE/Code+Contribution+Guidelines#CodeContributionGuidelines-LicensingofContributions) documentation.
- [ ] If my PR modifies REST API endpoints, I've opened a separate [REST Contract](https://github.com/DSpace/RestContract/blob/main/README.md) PR related to this change.
- [ ] If my PR includes new configurations, I've provided basic technical documentation in the PR itself.
- [ ] If my PR fixes an issue ticket, I've [linked them together](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue).
