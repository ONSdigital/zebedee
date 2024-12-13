# Regression testing

## Table of contents

<!-- TOC -->
* [Regression testing](#regression-testing)
 * [Table of contents](#table-of-contents)
 * [Background](#background)
 * [Functionality](#functionality)
  * [Reader functionality (web and publishing)](#reader-functionality-web-and-publishing)
   * [Serving page content](#serving-page-content)
   * [Published content retrieval](#published-content-retrieval)
  * [CMS functionality (publishing only)](#cms-functionality-publishing-only)
   * [Managing collections](#managing-collections)
  * [Other functionality (including deprecated enpoints)](#other-functionality-including-deprecated-enpoints)
<!-- TOC -->

## Background

Zebedee has limited test coverage, both for end to end and for unit testing. This document represents the start of
trying to build this up to facilitate ongoing maintenance as well as helping onboarding by describing it's behaviours.
By building up regression test packs and then automating, we will increase confidence in updates we do to Zebedee. It is
reaching end of life, but there will still be ongoing changes to the code base as dependencies are upgraded and
functionality is decommissioned.

## Functionality

### Reader functionality (web and publishing)

#### Serving page content

- Data `/data` get
- Export
- FileSize
- Generator
- Parents
- ResolveDatasets
- Resource
- Taxonomy

#### Other

- Health
- ReIndex

#### Published content retrieval

Unauthenticated endpoints that only return published data from the master content directory

- Index of Published Content (`/publishedindex`)
- Get an item of published Content Data (`/publisheddata`)

### CMS functionality (publishing only)

For standard content pages, there exists a `data.json` in Zebedee with a `type` that corresponds to the handlebars
template that will render it.

#### Managing collections

- Approve `/approve`
- CheckCollectionsForURI
- Collection
- CollectionBrowseTree
- CollectionDetails
- Collections
- Complete
- Content
- ContentMove
- ContentRename
- DataVisualisationZip
- DeleteContent
- PublishedCollections
- Review
- Unlock

Publishing Collections

- OnPublishComplete
- Publish
-

#### Content functionality

- Equation
- EquationPreview
- File
- ModifyTable
- Page
- Table
- TimeseriesImport
- Transfer
- Version

### Other functionality (including deprecated endpoints)

- Deprecated auth endpoints
 - Identity
 - ListKeyring
 - Login (`/login`)
 - Password
 - Permission
 - Ping
 - Service
 - Teams
 - TeamsReport
 - Users
 - CMD Specific auth
  - ServiceDatasetPermissions
  - ServiceInstancePermissions
  - UserDatasetPermissions
  - UserInstancePermissions
- Other
 - ClickEventLog
 - DataServices

