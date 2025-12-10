Scala plugin and  testing
 - Improvements for 
e: file:///home/runner/work/TorrentSearch-/TorrentSearch-/app/build.gradle.kts:31:13: Unresolved reference: debuggable
e: file:///home/runner/work/TorrentSearch-/TorrentSearch-/app/build.gradle.kts:40:13: Unresolved reference: debuggable
e: file:///home/runner/work/TorrentSearch-/TorrentSearch-/app/build.gradle.kts:48:13: Val cannot be reassigned
e: file:///home/runner/work/TorrentSearch-/TorrentSearch-/app/build.gradle.kts:52:13: Unresolved reference: debuggable
FAILURE: Build failed with an exception.
Configuration cache entry stored.
* Where:
Build file '/home/runner/work/TorrentSearch-/TorrentSearch-/app/build.gradle.kts' line: 31
* What went wrong:
Script compilation errors:
  Line 31: debuggable = false
                       ^ Unresolved reference: debuggable
  Line 40: debuggable = true
                       ^ Unresolved reference: debuggable
  Line 48: matchingFallbacks = mutableListOf("debug")
                       ^ Val cannot be reassigned
  Line 52: debuggable = false
                       ^ Unresolved reference: debuggable
4 errors
* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.
BUILD FAILED in 1m 21s
Error: Process completed with