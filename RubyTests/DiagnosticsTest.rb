# Load up the JAR
script_directory = File.dirname(__FILE__)
require File.join(script_directory,"SuperUtilities.jar")
java_import com.nuix.superutilities.SuperUtilities

# Initialize super utilities
$su = SuperUtilities.init($utilities,NUIX_VERSION)

# Use super utilities to save a Nuix diagnostics file
$su.saveDiagnostics("C:\\Temp\\DiagnosticsTest.zip")