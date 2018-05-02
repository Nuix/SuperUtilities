SuperUtilities
==============

# Overview

**Written By:** Jason Wells

A library of helper methods built on top of the Nuix API

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

The best place to start is the generated Java docs.

# Building From Source

Load up the code in your favorite IDE and make sure you reference the `\lib` sub-directory of your Nuix install.

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