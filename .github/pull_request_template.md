# References
Link to [JIRA](https://jira.lyrasis.org/projects/DS/summary) ticket(s), if any
Link to [REST Contract](https://github.com/DSpace/Rest7Contract) or PR containing the new REST Contract or required changes
Link to [Angular issue or PR](https://github.com/DSpace/dspace-angular/issues) related to this PR, if any

# Description
Short summary of changes (1-2 sentences)

# Instruction to Test/Review
Longer description (as necessary). May include code examples if those are helpful. 

**Be sure to add any guidance for how to test or review this PR.**

List of changes in this PR:
* First, ...
* Second, ...

# Checklist
Checklist of things all PRs should include. _These are just friendly reminders for you to check off before submitting your PR!_
- [ ] Your PR must be small in size (e.g. less than 1,000 lines of code, not including comments & integration tests) to make reviewing easier.  Exceptions may be made for larger efforts
- [ ] Your PR must follow the [Code Style Guide](https://wiki.lyrasis.org/display/DSPACE/Code+Style+Guide).
- [ ] Your PR must implement an already approved REST Contract or at least be linked to an existing PR 
- [ ] Your PR must provide javadoc for all the new public methods and classes. You are also requested to update javadoc of touched methods according to your changes and adding javadoc to complex private methods
- [ ] Your PR must include new Unit/Integration Tests for any new improvements/features.
    * Remember to provide tests based on different user types, including (1) Anonymous user, (2) Logged in user (non-admin), and (3) Administrator.
    * Remember to provide tests for both normal than error scenarios, all expected return codes should be tested
    * Avoid as much as possible to alter existing tests except if they were buggy. Be sure to note in the description any test that have been changed due to a change in the behavior so that we can evaluate impact on the Angular side (this typically result also in the need of REST Contract PR)
- [ ] If your PR is related to a bug fix you should have at least one test able to reproduce the bug and proof that the bug is now solved. Having a first commit with the test that has triggered a failed Travis build and a subsequent commit with the fix approved by the CI will drastically reduce the review time     
- [ ] If you add new third-party libraries/dependencies to any `pom.xml`, make sure their licenses align with our BSD-license.  See [Licensing of Contributions](https://wiki.lyrasis.org/display/DSPACE/Code+Contribution+Guidelines#CodeContributionGuidelines-LicensingofContributions)
