=begin

Copyright 2019 Nuix
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Visit GitHub for more examples: https://github.com/Nuix/Worker-Side-Script-Examples

This WSS demonstrates tagging items based on their mime type.  The mime types and associated tags assigned
to them is determined by listing each mime type and associated tag in the Hash $mime_type_tags.

=end

# IMPORTANT!!!
# It is important that SuperUtilities.jar is in the {NuixInstall}\lib directory so that
# the SuperUtilities classes are available to the workers when this WSS is executing!

# Root directory that JSON files will be written to
$json_export_directory = "D:\\Temp\\WorkerItemJson"

# WSS doesn't get NUIX_VERSION set, so we need to use a hard coded value.
# We need this for SuperUtilities so that JSON Exporter can make a couple version
# checks regarding some features availabilitiy on WorkerItem and SourceItem after
# 7.4 and 7.6.  Note much of the rest of SuperUtilities may not work since we are
# providing nil for Utilities, but the JsonExporter should work just fine.
$nuix_version = "8.0"

# We can perform initialization here
def nuixWorkerItemCallbackInit
	java_import com.nuix.superutilities.SuperUtilities
	$su = SuperUtilities.init(nil,$nuix_version)
	java_import com.nuix.superutilities.export.JsonExporter
	$json_exporter = JsonExporter.new
end

# Define our worker item callback
def nuixWorkerItemCallback(worker_item)
	output_json_path = File.join($json_export_directory,"#{worker_item.getItemGuid}.json")
	output_json_file = java.io.File.new(output_json_path)
	output_json_file.getParentFile.mkdirs
	$json_exporter.exportWorkerItemAsJson(worker_item,output_json_file)
end

# We can perform cleanup here if we need to
def nuixWorkerItemCallbackClose
end