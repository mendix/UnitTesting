# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

## [9.5.2] - 2024-10-15

### Fixed:
- We updated the Apache Commons IO dependency to 2.17.0. 

## [9.5.1] - 2024-07-04

### Fixed:
- We fixed the missing Dutch translations in the unit test details page
- We fixed log node name for the assertion error check microflow

## [9.5.0] - 2024-05-28

### Fixed:
- We updated the unit test details page for React client compatibility, introduced in Mendix 10.7.0

## [9.4.3] - 2024-05-01

### Fixed:
- We fixed an issue where the result of a test suite was not updated after running an individual test
- We fixed an issue in transaction handling for teardown microflows that could result in a database lock
- We changed the length to limited for the 'Name' attribute in the UnitTest entity

## [9.4.2] - 2024-02-07

### Fixed:
- We updated slf4j-api from 1.7.36 to 2.0.9

Note: Review the dependencies in the userlib folder after upgrading the Unit Testing module. Also, if you use any slf4j libraries other than slf4j-api, make sure their major versions are matching.

## [9.4.1] - 2023-11-14

### Fixed:
- We updated httpclient5 from 5.0.3 to 5.2.1
- We updated httpcore5 from 5.0.2 to 5.2
- We updated httpcore5-h2 from 5.0.2 to 5.2
- We updated slf4j-api from 1.7.25 to 1.7.36
- We removed the dependency on commons-codec-1.13

Note: Review the dependencies in the userlib folder after upgrading the Unit Testing module.

## [9.4.0] - 2023-09-20

### Fixed:
- We upgraded the module to Mendix version 9.18.0 for compatibility with Mendix 10
- We replaced the usage of “IContext.rollbackTransAction” with “IContext.rollbackTransaction”

## [9.3.0] - 2023-04-14

### Fixed:
- We upgraded the module to Mendix version 9.18.0 for compatibility with Mendix 10
- We replaced the usage of “IContext.rollbackTransAction” with “IContext.rollbackTransaction”

## [9.2.0] - 2023-01-04

### Fixed:
- We removed the dependency on the Community Commons module
- We updated the commons-lang3 dependency from 3.11 to 3.12.0
- We fixed an issue for JUnit-based unit tests, where a single unit test could be discovered and added to multiple modules in certain scenarios

Notes when upgrading from an earlier version:
- Review the dependencies in the userlib folder after upgrading the Unit Testing module. Remove the files 'commons-lang3-3.11.jar' and 'commons-lang3-3.11.jar.UnitTesting.RequiredLib', since commons-lang3 is updated to 3.12.0
- When upgrading from v9.1.0: If the Community Commons module is no longer used by any other module after the upgrade, you should remove the Community Commons module from your project. Review all dependencies in the userlib folder afterwards, and remove the dependencies that were required for Community Commons only
- When upgrading from v9.0.5 or below: If the Object Handling module is no longer used by any other module after the upgrade, you should remove the Object Handling module from your project. In this case, also delete the 'commons-lang3-3.7.jar' and 'commons-lang3-3.7.jar.ObjectHandling.RequiredLib' files from your userlib folder

## [9.1.0] - 2022-11-07

### Fixed:
- We replaced the dependency on Object Handling module with Community Commons module
Notes when upgrading from an earlier version:

- The Community Commons module is now required for this module to work. Download the Community Commons module from the marketplace and review all dependencies in the userlib folder afterwards.
- If the Object Handling module is no longer used by any other module after upgrading the Unit Testing module, you should remove the Object Handling module from your project. In this case, also delete the 'commons-lang3-3.7.jar' and 'commons-lang3-3.7.jar.ObjectHandling.RequiredLib' files from your userlib folder.

## [9.0.5] - 2022-11-01

### Fixed:
- We updated the commons-io dependency from 2.8.0 to 2.11.0 to be in line with Community Commons