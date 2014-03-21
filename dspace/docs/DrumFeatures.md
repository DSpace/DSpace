# DRUM Features

Summary of DRUM enhancements to base DSpace functionality.

## Administration

    add collection to community mapping
    select/display the bundle of a bitstream
    add preservation bundle 

## Authentication/Authorization

    dual authentication: password/CAS
    CAS authentication
        login
        gateway login
        logout
        automatic registration
        admin override (force login as a specific user)
        remove password updating on profile page
    LDAP authorization
        determine faculty status
        department affiliation for submission
        admin: Unit -> Group mapping
        admin: display Ldap info on Eperson edit page

## ETD

    bitstream start/end authorization
    custom embargoed bitstream messaging
    embargoed item statistics
    loader
        transform Proquest metadata to dublin core
        transform Proquest metadata to marc and transfer to TSD
        map into department etd collections
        transfer of files to bindery
        email notification: duplicate titles, .csv with embargoes, load report, marc transfer, bindery transfer

## Item/Community/Collection display

    live links for urls
    add handle display for Community/Collection
    change contributor.sponsor to relation.isAvailableAt
    bitstream views statistic (No. of Downloads)

## Loaders (other than ETD)
legacy, single-use loaders; do not need testing

    CS Tech Reports
    ISR
    CISSM

## Navigation

    community groups (display, admin)
    remove /index.jsp from url
    navbar: mydspace links
    navbar: login status placement
    add context link to search help

## Miscellaneous

    duplicate title detection
    static browse page for crawlers
    fix bitstream servlet when user is not authorized
    upgrade to Handle server 6.2
    fix audio upload problem?
    fix bitstream format registry problem?
    fix oai-pmh to xml escape set names
    add ability for attachments on email notices

## Search / Browse

    add Advisor to advanced search
    fix diacritic problem: add synonyms with a non-diacritic version
    fix browse of items in multiple collections
    change author browse to contributor.author instead of contributor.*

## Statistics

    add item,bitstream views count (with monthly update)
    add Item option to not update last modified date
    view standard stats from admin interface
    embargo statistics

## Submission

    select multiple collections
    require contributor.author
    add contributor.advisor
    require date.issued for all submission types
    show required submission metadata fields (submit/edit-metadata.jsp)
    allow bitstream editing in workflow mode in addition to workspace (submission) mode (submit/upload-file-list.jsp)
    make citation required if previously published
    add "Submit to This Collection"
