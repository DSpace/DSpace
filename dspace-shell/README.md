# DSpace Shell - command list

## database test

**Group:** `Database commands`

Alias: ``

Test Database Connection

usage: `database test [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for database test | void | false |

## database validate

**Group:** `Database commands`

Alias: ``

Run DSpace's "update-sequences.sql" script

usage: `database validate [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for database validate | void | false |

## database info

**Group:** `Database commands`

Displays database information using the DSpace context

usage: `database info [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for database info | void | false |

## metadata

**Group:** ``

Alias: ``

List all available commands and their options - for test only - you should use -h option or the help command

usage: `metadata [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for metadata | void | false |

## database migrate

**Group:** `Database commands`

Alias: ``

Run all pending database migrations

usage: `database migrate [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --ignored | Also run any previously "ignored" migrations during the migration | boolean | false |
| --force | Even if no pending migrations exist, still run migrate to trigger callbacks | boolean | false |
| --version | ONLY run migrations up to a specific DSpace version (ONLY FOR TESTING) | java.lang.String | false |
| --help | help for database migrate | void | false |

## database repair

**Group:** `Database commands`

Alias: ``

Run Flyway repair script

usage: `database repair [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for database repair | void | false |

## clear

**Group:** `Built-In Commands`

Alias: ``

Clear the shell screen.

usage: `clear [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for clear | void | false |

## history

**Group:** `Built-In Commands`

Alias: ``

Display or save the history of previously run commands

usage: `history [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --file | A file to save history to. | java.io.File | false |
| --help | help for history | void | false |

## version

**Group:** `Built-In Commands`

Alias: ``

Show version info

usage: `version [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for version | void | false |

## script

**Group:** `Built-In Commands`

Alias: ``

Read and execute commands from a file.

usage: `script [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --file | null | java.io.File | true |
| --help | help for script | void | false |

## discovery index

**Group:** `Discovery commands`

called without any options, will update/clean an existing index

usage: `discovery index [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --rebuild | (re)build index, wiping out current one if it exists | boolean | false |
| --clean | clean existing index removing any documents that no longer exist in the db | boolean | false |
| --force | if updating existing index, force each handle to be reindexed even if uptodate | boolean | false |
| --item | Reindex an individual object (and any child objects).  When run on an Item, it just reindexes that single Item. When run on a Collection, it reindexes the Collection itself and all Items in that Collection. When run on a Community, it reindexes the Community itself and all sub-Communities, contained Collections and contained Items. | java.lang.String | false |
| --remove | Remove an Item, Collection or Community from index based on its handle | java.lang.String | false |
| --spellchecker | Rebuild the spellchecker, can be combined with -b and -f | boolean | false |
| --help | help for discovery index | void | false |

## help

**Group:** `Built-In Commands`

Alias: ``

Display help about available commands

usage: `help [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --command | The command to obtain help for. | java.lang.String[] | false |
| --help | help for help | void | false |

## database skip

**Group:** `Database commands`

Alias: ``

Skip a specific Flyway migration (by telling Flyway it succeeded)

usage: `database skip [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --version | The 'skip' command REQUIRES a version to be specified. Only that single migration will be skipped. For the list of migration versions use the 'info' command. | java.lang.String | true |
| --help | help for database skip | void | false |

## database clean

**Group:** `Database commands`

Alias: ``

Run Flyway clean script - WARNING!!! might delete all data from your database

usage: `database clean [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for database clean | void | false |

## database update-sequences

**Group:** `Database commands`

Alias: ``

Run DSpace's "update-sequences.sql" script

usage: `database update-sequences [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for database update-sequences | void | false |

## stacktrace

**Group:** `Built-In Commands`

Alias: ``

Display the full stacktrace of the last error.

usage: `stacktrace [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for stacktrace | void | false |

## embargo lifter

**Group:** `Embargo commands`

this command executes embargo lifter by reindexing content

usage: `embargo lifter [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --nDays | The number of past days to process embargo. As an example, nDays=5 means process the past 5 days. | int | false |
| --help | help for embargo lifter | void | false |

## quit

**Group:** `Built-In Commands`

Exit the shell.

usage: `quit [OPTIONS]`

| Option | Description | Type | Required |
| ------ | ------ | ------ | ------ |
| --help | help for quit | void | false |
