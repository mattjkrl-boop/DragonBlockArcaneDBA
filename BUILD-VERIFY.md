# DBA Compile Verification

Status: OPEN. The mod has never had a passing build. Do not claim it compiles until a build actually passes and produces build/libs/dragonblockarcanedba-1.0.0.jar.

## Sandbox attempts (Muse VM)

Attempts 1 to 5 (Oct 31, 2026): failed for environment reasons (Gradle daemon connectivity, low reported memory), never for code reasons, on unmodified code.

Attempt 6 (Nov 1, 2026, this DBA chat, HEAD 80a2b98, clean tree, in sync with origin/main):
- Gradle wrapper 9.5.1 distribution downloads fine. `./gradlew --version --no-daemon` passes (JDK 25.0.5 Temurin).
- `./gradlew compileJava compileClientJava --no-daemon` fails in about 9 seconds before any Java compilation starts:
  `DaemonInitialConnectException: The first result from the daemon was empty.`
- With `--info`, the daemon does start (pid logged, address 127.0.0.1 port 46341) and then the client reports `Removing daemon from the registry due to communication failure.`
- Root cause: the known sandbox non-HTTP loopback TCP restriction. The Gradle client cannot talk to its own daemon over 127.0.0.1. Same root cause as Phoenix Fall (Oct 25), the Mega Mod Phase 0 build, and the DBA baseline (Oct 31). `--no-daemon` does not help: Gradle 9.5.1 still forks a single-use daemon because gradle.properties sets org.gradle.jvmargs=-Xmx1G.
- VM memory at the time: 7,934 MB total, about 600 to 970 MB available. Tight, but the failure happens at daemon connect, before compilation, so memory is not the proven cause of attempt 6.
- Conclusion: this sandbox cannot give a compile signal for DBA. The check below on Carter's Windows machine is the real verification.

## Exact build on Carter's Windows machine (build.py)

Prereqs: JDK 25 installed, git, Python 3. No other setup. GeckoLib 5.5.3 is pulled by Gradle from Modrinth at build time, so an internet connection is needed on the first build.

1. Clone or update:
   - Fresh: `git clone https://github.com/mattjkrl-boop/DragonBlockArcaneDBA.git`
   - Existing: `cd DragonBlockArcaneDBA` then `git fetch origin` then `git pull`
   - Confirm HEAD: `git log --oneline -1` should show `80a2b98` or newer.
2. Point JAVA_HOME at JDK 25 (PowerShell, current window only):
   - `$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"` (adjust to the real install folder)
   - Verify: `& "$env:JAVA_HOME\bin\java.exe" -version` must print 25.x.
   - build.py exits immediately if JAVA_HOME is unset or has no javac, by design.
3. Build (from the repo root):
   - `python build.py`
   - That runs `gradlew.bat clean build --stacktrace`, then copies the remapped jar to `mod\dragonblockarcanedba-1.0.0.jar` (any previous jar there is moved to `mod\backup\`).
   - Variants: `python build.py --no-clean` skips clean. `python build.py --nuke-caches` also deletes `.gradle` and `.fabric` first; use it only if a normal build fails with cache or Loom errors.
4. Read the result:
   - Pass: console ends with `SUCCESS: Mod jar successfully built and placed in mod/ folder.` and `build\libs\dragonblockarcanedba-1.0.0.jar` exists.
   - Fail: send the full console output (or at least the first `error:` / `FAILURE` block with the file and line) back to this chat. Do not retry with random changes; the first error block is what gets fixed.
5. Optional run check after a pass: `python launch.py` builds and launches the client (it passes `-Pusername=` through to runClient).

## After a pass

Update this file with the date, machine, HEAD commit, and jar size, and update brain mods/DBA.md. Only then does feature wave coding start building on a verified base.
