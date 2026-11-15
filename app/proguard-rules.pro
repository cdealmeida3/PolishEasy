# R8 rules for the release build.
#
# kotlinx.serialization ships its own R8 rules inside the library, so the content
# model in :engine needs no keep rules here.

# Keep line numbers so crash stack traces can be read with the mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
