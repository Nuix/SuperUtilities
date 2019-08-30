script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.misc.DigestList

$current_case = $utilities.getCaseFactory.open('D:\cases\FakeData_8.0')

iutil = $utilities.getItemUtility

# Get all emails in case, deduplicate so that we only have 1 instance of each MD5
items = $current_case.search("kind:email")
items = iutil.deduplicate(items).to_a

# Split items into 2 collections, collection a contains the first 1000 items
# and collection b contains the rest
items_a = items.take(1000)
items_b = items[1000..-1]

puts "Items A: #{items_a.size}"
puts "Items B: #{items_b.size}"

digest_a = DigestList.new
digest_b = DigestList.new
digest_c = DigestList.new

digest_a.addItems(items_a)
digest_b.addItems(items_b)

digest_a_path = File.join(script_directory,"DigestA.hash")
digest_b_path = File.join(script_directory,"DigestB.hash")
digest_c_path = File.join(script_directory,"DigestC.hash")

digest_a_file = java.io.File.new(digest_a_path)
digest_b_file = java.io.File.new(digest_b_path)
digest_c_file = java.io.File.new(digest_c_path)

digest_a.saveFile(java.io.File.new(digest_a_path))
digest_b.saveFile(java.io.File.new(digest_b_path))

# Create a digest that is a combination of A and B
DigestList.combineDigestFiles(digest_c_file,[digest_a_file,digest_b_file])

# Test whether we can calculate digest count without actually importing file's contents
puts "C file contains #{DigestList.getDigestCount(digest_c_file)} according to DigestList.getDigestCount"
imported_to_c = digest_c.importFile(digest_c_path)
puts "Digests imported from file C: #{imported_to_c}"

# Check whether each digest finds expected number of items
puts "A: #{iutil.deduplicate(digest_a.findMatchingItems($current_case)).size} | #{items_a.size}"
puts "B: #{iutil.deduplicate(digest_b.findMatchingItems($current_case)).size} | #{items_b.size}"
puts "C: #{iutil.deduplicate(digest_c.findMatchingItems($current_case)).size} | #{items_a.size + items_b.size}"

digest_a.saveCaseLevelDigestList($current_case,"A_CaseLevel")
digest_a.saveUserLevelDigestList("A_UserLevel")
digest_a.saveSystemLevelDigestList("A_SystemLevel")

puts "deduped digest-list:A_CaseLevel: #{iutil.deduplicate($current_case.search("digest-list:A_CaseLevel")).size}"
puts "deduped digest-list:A_UserLevel: #{iutil.deduplicate($current_case.search("digest-list:A_UserLevel")).size}"
puts "deduped digest-list:A_SystemLevel: #{iutil.deduplicate($current_case.search("digest-list:A_SystemLevel")).size}"

# Cleanup
$current_case.close
digest_a_file.delete
digest_b_file.delete
digest_c_file.delete