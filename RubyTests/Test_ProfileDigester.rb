script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)
java_import com.nuix.superutilities.misc.ProfileDigester

$current_case = $utilities.getCaseFactory.open("D:\\cases\\FakeData_8.0")

profile_digester = ProfileDigester.new

include_content_text = true
items = $current_case.search("kind:email")

profile = $utilities.getMetadataProfileStore.createMetadataProfile
profile = profile.addMetadata("SPECIAL","Name")
profile = profile.addMetadata("SPECIAL","Kind")

profile_digester.setProfile(profile)
profile_digester.setIncludeItemText(include_content_text)

concat_grouped = Hash.new{|h,k| h[k] = [] }
hash_grouped = Hash.new{|h,k| h[k] = [] }

items.each do |item|
	concat = profile.getMetadata.map{|field| field.evaluate(item)}.join
	concat << item.getTextObject.toString if include_content_text

	hash = profile_digester.generateMd5String(item)

	concat_grouped[concat] << item
	hash_grouped[hash] << item
end

if concat_grouped.size == hash_grouped.size
	puts "Group sizes match"


	# Ruby iterates a hash in key insertion order so I believe each
	# entry should correspond to the same "dedupe group".  We will peel
	# off 1 group from each and make sure the concatenation based key
	# grouped up the same number of input items as the 
	cge = concat_grouped.to_enum
	hge = hash_grouped.to_enum

	matched = 0
	mismatched = 0

	concat_grouped.size.times do |iteration|
		# Get next key value pair from each hash
		c = cge.next
		h = hge.next

		# For each group, do they have the same number of items?  We expect they should.
		if c[1].size != h[1].size
			# If item counts for same group differ, report about it
			puts "="*20
			puts "#{c[0]} => #{c[1].size}"
			puts "#{h[0]} => #{h[1].size}"
			"Concat had #{c[1].size} for group but hash had #{h[1].size} for same group"
			mismatched += 1
		else
			matched += 1
		end
	end

	puts "Matched: #{matched}"
	puts "Mismatched: #{mismatched}"

	# Now we put same items into an item set using ProfileDigester, we expect that the
	# total number of originals in that item set should match the number of groups in either hash,
	# 1 original per group, when deduping by individual
	item_set_name = "Profile Debugger #{Time.now.to_i}"
	dedupe_by = "INDIVIDUAL"
	item_set = profile_digester.addItemsToItemSet($current_case,item_set_name,dedupe_by,items)
	puts "Item Set Originals: #{item_set.getOriginals.size}, Expected: #{concat_grouped.size}"
	# We expect that duplicates will be items count - originals count
	puts "Item Set Duplicates: #{item_set.getDuplicates.size}, Expected: #{items.size - concat_grouped.size}"
else
	puts "Sizes don't match!"
end


$current_case.close