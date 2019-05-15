script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.export.CustomExporter

$current_case = $utilities.getCaseFactory.open('D:\cases\FakeData_1552095540_Hare\Pearlie Morar')

export_directory = "D:\\temp\\CustomExport_#{Time.now.to_i}"
items = $current_case.search("kind:email").take(1000)

ce = CustomExporter.new

# A path template is provided for text/native/pdf/tiff/json files exported by CustomExporter.
# The following placeholders are recognized.  When restructuring the final export, the given path
# template is resolved for the relevant item, determining where it will end up in the final restructured
# export.  DAT and OPT loadfiles are also updated to reflect final destination of these files.
#
# {export_directory} - The export directory specified when you call CustomExporter.exportItems
# {guid} - The item's GUID.
# {guid_prefix} Characters 0-2 of the item's GUID. Useful for creating sub-directories based on GUID.
# {guid_infix} Characters 3-5 of the item's GUID. Useful for creating sub-directories based on GUID.
# {name} - The item's name as obtained by Item.getLocalisedName
# {md5} - The item's MD5 or NO_MD5 for items without an MD5 value
# {type} - The item's type name as obtained by ItemType.getLocalisedName
# {mime_type} - The item's mime type as obtained by ItemType.getName
# {kind} - The item's kind name as obtained by ItemType.getKind.getName
# {custodian} - The item's assigned custodian or NO_CUSTODIAN for items without a custodian assigned
# {evidence_name} - The name of the evidence the item belongs to.
# {item_date_short} - The item's item date formatted YYYYMMDD or NO_DATE for items without an item date.
# {item_date_long} - The item's item date formatted YYYYMMdd-HHmmss or NO_DATE for items without an item date.
# {item_date_year} - The item's item date 4 digit year or NO_DATE for items without an item date.
# {top_level_guid} - The GUID of the provided item's top level item or ABOVE_TOP_LEVEL for items which are above top level.
# {top_level_name} - The name (via Item.getLocalisedName) of the provided item's top level item or ABOVE_TOP_LEVEL for items which are above top level.
# {top_level_kind} - The kind (via ItemType.getKind.getName) of the provided item's top level item or ABOVE_TOP_LEVEL for items which are above top level.
# {original_extension} - The original extension as obtained from Nuix via Item.getOriginalExtension or NO_ORIGINAL_EXTENSION for items where Nuix does not have an original extension value.
# {corrected_extension} - The corrected extension as obtained from Nuix via Item.getCorrectedExtension or NO_CORRECTED_EXTENSION for items where Nuix does not have a corrected extension value.
# {extension} - Extension of given file as Nuix exported it
#
# Additionally we can provide settings as accepted by BatchExporter.addProduct in the second argument.  Note that the following settings
# are overwritten by the script (effectively ignored) since their functionality is effectively provided by the path template you provide.
# - naming
# - path
ce.exportText("{export_directory}\\CUSTOM_TEXT\\{item_date_short}\\{guid}.txt",{})
ce.exportNatives("{export_directory}\\CUSTOM_NATIVE\\{kind}\\{guid}.{extension}",{})
ce.exportPdfs("{export_directory}\\CUSTOM_PDF\\{type}\\{guid}.pdf",{})
ce.exportTiffs("{export_directory}\\CUSTOM_IMAGE\\{type}\\{guid}.{extension}",{})
ce.exportJson("{export_directory}\\JSON\\{guid}.json")

# Some columns in the DAT file produced by BatchExporter are not based on the metadata profile
# you provide, but are added to the DAT as part of the export process.  Sometimes people would
# like these columns to have different headers.  Since we need to rebuild the DAT file to reflect
# the resstructure of the export products, we can also renamed headers during this process.
header_renames = {
	"DOCID" => "DOCID_Renamed",
	"PARENT_DOCID" => "PARENT_DOCID_Renamed",
	"ATTACH_DOCID" => "ATTACH_DOCID_Renamed",
	"BEGINBATES" => "BEGINBATES_Renamed",
	"ENDBATES" => "ENDBATES_Renamed",
	"BEGINGROUP" => "BEGINGROUP_Renamed",
	"ENDGROUP" => "ENDGROUP_Renamed",
	"PAGECOUNT" => "PAGECOUNT_Renamed",
	"ITEMPATH" => "ITEMPATH_Renamed",
	"TEXTPATH" => "TEXTPATH_Renamed",
	"PDFPATH" => "PDFPATH_Renamed",
	"TIFFPATH" => "TIFFPATH_Renamed",
}
ce.setHeaderRenames(header_renames)

ce.exportItems($current_case,export_directory,items)

$current_case.close