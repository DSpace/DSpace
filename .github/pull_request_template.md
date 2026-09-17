## References
_Add references/links to any related issues or PRs. These may include:_
* Fixes #issue-number (if this fixes an issue ticket)
* Related to DSpace/RestContract#pr-number  (if a corresponding REST Contract PR exists)

## Description
Short summary of changes (1-2 sentences).

## Instructions for Reviewers
Please add a more detailed description of the changes made by your PR. At a minimum, providing a bulleted list of changes in your PR is helpful to reviewers.

List of changes in this PR:
* First, ...
* Second, ...

**Include guidance for how to test or review your PR.** This may include: steps to reproduce a bug, screenshots or description of a new feature, or reasons behind specific changes. 

## AI Disclosure (REQUIRED)

Per our AI Contribution Policy **_(ADD LINK)_**, all contributors to DSpace MUST disclose AI/LLM usage.
This information helps reviewers assess provenance, licensing, security, and the level of verification needed.

If AI/LLM tools were ONLY used for incidental assistance (such as spelling/grammar correction, IDE/code autocomplete, minor tooling suggestions, or translation), then select 'No'.

**Were AI/LLM tools used in the creation of this PR?**
   - [ ] No (This PR does NOT include AI-generated or AI-assisted content that requires disclosure.)
   - [ ] Yes (This PR includes AI-generated or AI-assisted content that requires disclosure.)

**If "Yes", briefly describe what portion of this PR was generated or assisted by AI. Additionally, tell us how you've verified the AI output is accurate.**

(Example: "AI assisted me in investigating the bug and generated a suggested patch. I reviewed, modified and tested the code. No AI was used for the PR description.")

## Checklist
_This checklist provides a reminder of what we are going to look for when reviewing your PR. You need not complete this checklist prior to creating your PR (draft PRs are always welcome).
However, reviewers may request that you complete any actions in this list if you have not done so. If you are unsure about an item in the checklist, don't hesitate to ask. We're here to help!_

- [ ] My PR is **created against the `main` branch** of code (unless it is a backport or is fixing an issue specific to an older branch).
- [ ] My PR is **small in size** (e.g. less than 1,000 lines of code, not including comments & integration tests). Exceptions may be made if previously agreed upon.
- [ ] My PR **follows all coding best practices** based on the [Code Conventions Guide](../CODE_CONVENTIONS.md).
- [ ] My PR **passes Checkstyle** validation based on the [Code Style Guide](../CODE_STYLE.md).
- [ ] My PR **includes Javadoc** for _all new (or modified) public methods and classes_. It also includes Javadoc for large or complex private methods.
- [ ] My PR **passes all tests and includes new/updated Unit or Integration Tests** based on the [Code Testing Guide](https://wiki.lyrasis.org/display/DSPACE/Code+Testing+Guide).
- [ ] My PR **includes details on how to test it**. I've provided clear instructions to reviewers on how to successfully test this fix or feature.
- [ ] If my PR includes new libraries/dependencies (in any `pom.xml`), I've made sure their licenses align with the [DSpace BSD License](https://github.com/DSpace/DSpace/blob/main/LICENSE) based on the [Licensing of Contributions](https://wiki.lyrasis.org/display/DSPACE/Code+Contribution+Guidelines#CodeContributionGuidelines-LicensingofContributions) documentation.
- [ ] If my PR modifies REST API endpoints, I've opened a separate [REST Contract](https://github.com/DSpace/RestContract/blob/main/README.md) PR related to this change.
- [ ] If my PR includes new configurations, I've provided basic technical documentation in the PR itself.
- [ ] If my PR fixes an issue ticket, I've [linked them together](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue).
