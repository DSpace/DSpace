# How to Contribute

DSpace is a community built and supported project. We do not have a centralized development or support team, but have a dedicated group of volunteers who help us improve the software, documentation, resources, etc.

* [Contribute new code via a Pull Request](#contribute-new-code-via-a-pull-request)
* [Contribute documentation](#contribute-documentation)
* [Help others on mailing lists or Slack](#help-others-on-mailing-lists-or-slack)
* [Join a working or interest group](#join-a-working-or-interest-group)

## Contribute new code via a Pull Request

We accept [GitHub Pull Requests (PRs)](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork) at any time from anyone.
Contributors to each release are recognized in our [Release Notes](https://wiki.lyrasis.org/display/DSDOC7x/Release+Notes).

Code Contribution Checklist
- [ ] PRs _should_ be smaller in size (ideally less than 1,000 lines of code, not including comments & tests)
- [ ] PRs **must** pass Checkstyle validation based on our [Code Style Guide](https://wiki.lyrasis.org/display/DSPACE/Code+Style+Guide).
- [ ] PRs **must** include Javadoc for _all new/modified public methods and classes_. Larger private methods should also have Javadoc
- [ ] PRs **must** pass all automated tests and include new/updated Unit or Integration tests based on our [Code Testing Guide](https://wiki.lyrasis.org/display/DSPACE/Code+Testing+Guide).
- [ ] If a PR includes new libraries/dependencies (in any `pom.xml`), then their software licenses **must** align with the [DSpace BSD License](https://github.com/DSpace/DSpace/blob/main/LICENSE) based on the [Licensing of Contributions](https://wiki.lyrasis.org/display/DSPACE/Code+Contribution+Guidelines#CodeContributionGuidelines-LicensingofContributions) documentation.
- [ ] Basic technical documentation _should_ be provided for any new features or changes to the REST API. REST API changes should be documented in our [Rest Contract](https://github.com/DSpace/RestContract).
- [ ] If a PR fixes an issue ticket, please [link them together](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue).

Additional details on the code contribution process can be found in our [Code Contribution Guidelines](https://wiki.lyrasis.org/display/DSPACE/Code+Contribution+Guidelines)

## Contribute documentation

DSpace Documentation is a collaborative effort in a shared Wiki. The latest documentation is at https://wiki.lyrasis.org/display/DSDOC7x 

If you find areas of the DSpace Documentation which you wish to improve, please request a Wiki account by emailing wikihelp@lyrasis.org.
Once you have an account setup, contact @tdonohue (via [Slack](https://wiki.lyrasis.org/display/DSPACE/Slack) or email) for access to edit our Documentation.

## Help others on mailing lists or Slack

DSpace has our own [Slack](https://wiki.lyrasis.org/display/DSPACE/Slack) community and [Mailing Lists](https://wiki.lyrasis.org/display/DSPACE/Mailing+Lists) where discussions take place and questions are answered.
Anyone is welcome to join and help others. We just ask you to follow our [Code of Conduct](https://www.lyrasis.org/about/Pages/Code-of-Conduct.aspx) (adopted via LYRASIS).

## Join a working or interest group

Most of the work in building/improving DSpace comes via [Working Groups](https://wiki.lyrasis.org/display/DSPACE/DSpace+Working+Groups) or [Interest Groups](https://wiki.lyrasis.org/display/DSPACE/DSpace+Interest+Groups).

All working/interest groups are open to anyone to join and participate.  A few key groups to be aware of include:

* [DSpace 7 Working Group](https://wiki.lyrasis.org/display/DSPACE/DSpace+7+Working+Group) - This is the main (mostly volunteer) development team. We meet weekly to review our current development [project board](https://github.com/orgs/DSpace/projects), assigning tickets and/or PRs.
* [DSpace Community Advisory Team (DCAT)](https://wiki.lyrasis.org/display/cmtygp/DSpace+Community+Advisory+Team) - This is an interest group for repository managers/administrators. We meet monthly to discuss DSpace, share tips & provide feedback back to developers.