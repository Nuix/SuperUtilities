script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.misc.DigestList

$current_case = $utilities.getCaseFactory.open('D:\cases\FakeData_8.0')

iutil = $utilities.getItemUtility

items = $current_case.search("kind:email")
items = iutil.deduplicate(items).to_a
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

digest_a.saveFile(java.io.File.new(digest_a_path))
digest_b.saveFile(java.io.File.new(digest_b_path))
DigestList.combineDigestFiles(java.io.File.new(digest_c_path),[digest_a_path,digest_b_path].map{|f|java.io.File.new(f)})
digest_c.importFile(digest_c_path)

puts "A: #{iutil.deduplicate(digest_a.findMatchingItems($current_case)).size} | #{items_a.size}"
puts "B: #{iutil.deduplicate(digest_b.findMatchingItems($current_case)).size} | #{items_b.size}"
puts "C: #{iutil.deduplicate(digest_c.findMatchingItems($current_case)).size} | #{items_a.size + items_b.size}"

$current_case.close