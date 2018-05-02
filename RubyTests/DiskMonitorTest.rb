script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

monitor = $su.createFreeSpaceMonitor
monitor.whenFreeSpaceBelowThreshold do |monitor,info|
	puts "Low space detected"
	puts info.toString
end
monitor.addMonitoredLocation("E:\\",1.0)
monitor.beginMonitoring

sleep(30)

monitor.shutdownMonitoring