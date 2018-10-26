SuperUtilities
==============

# Overview

**Written By:** Jason Wells

A library of helper methods built on top of the Nuix API

View the GitHub project [here](https://github.com/Nuix/SuperUtilities) or download the latest release [here](https://github.com/Nuix/SuperUtilities/releases).

View the Java docs [here](https://nuix.github.io/SuperUtilities/).

This is a small library of functionality which is a companion to the functionality already present in the Nuix API.  Over the years as I have workendon Nuix scripts, there were certain pieces of functionality I had noticed I was re-writing over and over.  With this project my aim was to codify some of that functionality in such a manner that it was reusable and hopefully generic enough that it would be applicable to other scripts down the line.  I've also since added some functionality that was needed just in a specific instance but I envisioned it may have usefulness later on as well.  Examples of the functionality include:

- [NuixVersion](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/misc/NuixVersion.html): Provides a way to test the version of Nuix being ran, allowing you to have code which reacts differently depending on the version of Nuix in use.
- [CaseUtility](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/cases/CaseUtility.html): Provides methods for finding case directories and parsing case FBI2 files.
- [CaseInfo](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/cases/CaseInfo.html): A class providing information about a file system based Nuix case that can be be determine about the case without opening it in the API such as:
    - Is the case compound?
    - How many stores does the case have?
    - Does the case appear to be locked currently?
    - etc
- [CaseLockInfo](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/cases/CaseLockInfo.html): Parses case lock file to help you determine what user is locking the case and what machine.
- [BulkCaseProcessor](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/cases/BulkCaseProcessor.html): Provides a way to run a block of code against multiple cases.  Exposes callbacks allowing a script to determine how to handle: a case being locked, an error opening a case and an error in user code while working with a case.
- [FormatUtility](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/misc/FormatUtility.html): Provides various helper methods I have found useful from a script such as:
    - Conversion of byte count to KM, MB, GB
    - Conversion of metadata property data types to string
    - General number formatting
    - Resolve placeholders in template strings provided by end user
    - etc
- [JsonExporter](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/export/JsonExporter.html): Provides methods for exporting an item to a JSON representation.
- [TemplateExporter](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/export/TemplateExporter.html): Kava wrapper around Ruby ERB templates, supports converting HTML template output to PDFs using iText.
- [PdfUtility](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/misc/PdfUtility.html): Provides convenience method for using iText to merge multiple PDFs into a single PDF.
- [SuperItemUtility](https://nuix.github.io/SuperUtilities/com/nuix/superutilities/items/SuperItemUtility.html): Provides some methods for accomplishing some operations not covered by the Nuix API's ItemUtility object such as:
	- Find container ancestor items for one or more provided items
	- Deduplicate with custom logic determining which item is kept as the original
	- Find physical file ancestors for provided items
	- Split a collection of items into chunks while attempting to keep items in the same family within the same chunk
- and more

A good place to get a sense of the functionality on offer is to preuse the [Java docs](https://nuix.github.io/SuperUtilities/).

# Getting Started

## Setup

Begin by downloading the latest release.  Include a reference to the JAR file in your code.  Make sure to initialize the library with the appropriate Nuix objects.  For example in Ruby this could look like:

```ruby
# Get the directory this script file resides in
script_directory = File.dirname(__FILE__)

# Load the JAR, in this instance we are assuming it is located
# in the same directory as this script file
require File.join(script_directory,"SuperUtilities.jar")

# Import the SuperUtilities class
java_import com.nuix.superutilities.SuperUtilities

# Initialize library with copy of Utilities object and current version of Nuix
# and for convenience store in global variable $su
$su = SuperUtilities.init($utilities,NUIX_VERSION)
```

Similar to the Utilities object in the API, you can then use SuperUtilities object to obtain the various objects offered in this library.  For example, use SuperItemUtility to resolve
a collection of items to their container ancestor items:

```ruby
# Open a case
$current_case = $utilities.getCaseFactory.open("C:\\Cases\\SomeCase")

# Find emails belonging to bob
items = $current_case.search("custodian:Bob AND kind:email")

# Resolve those emails to the container items they came from
container_ancestor_items = super_iutil.findContainerAncestors(items)

# List the names of the container items
container_ancestor_items.each do |item|
	puts item.getLocalisedName
end

# Make sure to close the case
$current_case.close
```

# Building From Source

Load up the code in your favorite IDE and make sure you reference the `\lib\*.jar` from your Nuix install in the build path.

# License

```
Copyright 2018 Nuix

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```