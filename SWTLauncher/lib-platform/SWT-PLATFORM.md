# SWT JAR for this platform (for IDEs)

Run `../setup-platform-swt.sh` to put the SWT JAR for your platform here.
The JAR is ignored by git; only this file is committed.

`../lib` has the SWT JARs for all platforms. The released tools pick the
right one at runtime through `SWTLauncher`. An IDE runs the tools
directly, so its classpath must have exactly one SWT JAR, from this directory.

See [Getting started](../../doc/intellij-idea.md#getting-started)
in the IntelliJ IDEA guide.
