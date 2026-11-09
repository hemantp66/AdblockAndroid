# JNI keeps: adblockclient-lib.cpp finds AdBlockClient and MatchResult by class name and calls
# MatchResult's constructor through its exact signature, so R8 must not rename or shrink them.
-keep class io.github.edsuns.adblockclient.AdBlockClient { *; }
-keep class io.github.edsuns.adblockclient.MatchResult { *; }
