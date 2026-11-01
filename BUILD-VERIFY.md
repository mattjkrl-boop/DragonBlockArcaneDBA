# DBA Compile Verification

Status: OPEN, and no longer a gate. The mod has never had a passing build. Do not claim it compiles until a build actually passes and produces build/libs/dragonblockarcanedba-1.0.0.jar.

Decision (Carter, Nov 1, 2026): Carter will not run the Windows build. "Just audit after and b4 I won't build it." Feature wave work proceeds without a build pass, verified instead by a before audit and an after audit on every change (see the audit method at the end of this file). This matches the Mega Mod standing directive: keep building even though Gradle cannot build in this VM, and audit continuously so the code will work when it does build. Never present an audit as a compile pass.

## Sandbox attempts (Muse VM)

Attempts 1 to 5 (Oct 31, 2026): failed for environment reasons (Gradle daemon connectivity, low reported memory), never for code reasons, on unmodified code.

Attempt 6 (Nov 1, 2026, this DBA chat, HEAD 80a2b98, clean tree, in sync with origin/main):
- Gradle wrapper 9.5.1 distribution downloads fine. `./gradlew --version --no-daemon` passes (JDK 25.0.5 Temurin).
- `./gradlew compileJava compileClientJava --no-daemon` fails in about 9 seconds before any Java compilation starts:
  `DaemonInitialConnectException: The first result from the daemon was empty.`
- With `--info`, the daemon does start (pid logged, address 127.0.0.1 port 46341) and then the client reports `Removing daemon from the registry due to communication failure.`
- Root cause: the known sandbox non-HTTP loopback TCP restriction. The Gradle client cannot talk to its own daemon over 127.0.0.1. Same root cause as Phoenix Fall (Oct 25), the Mega Mod Phase 0 build, and the DBA baseline (Oct 31). `--no-daemon` does not help: Gradle 9.5.1 still forks a single-use daemon because gradle.properties sets org.gradle.jvmargs=-Xmx1G.
- VM memory at the time: 7,934 MB total, about 600 to 970 MB available. Tight, but the failure happens at daemon connect, before compilation, so memory is not the proven cause of attempt 6.
- Conclusion: this sandbox cannot give a compile signal for DBA, and Carter has declined the Windows check (Nov 1, 2026). The build.py steps below stay in this file for whoever builds it next (for example Matt, or a future machine where Gradle works). They are not an open task for Carter.

## Exact build on a Windows machine (build.py, reference only)

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

## After a pass (whoever runs it)

Update this file with the date, machine, HEAD commit, and jar size, and update brain mods/DBA.md.

## Audit method (the standing verification for every DBA change)

Before a change:
- Read every file that will be touched, in full, plus the files that call it and the files it calls.
- Inventory current behavior in writing (the before audit) so regressions are visible in the diff.
- Check every Minecraft, Fabric, and GeckoLib API to be used against an existing usage in this repo on 26.2. An API with no in-repo precedent gets flagged in the change notes, not assumed.

After a change:
- Re-read the full diff, line by line, against the before audit.
- Validate every changed JSON file by parsing it (geo models, item models, post_effect chains, animations).
- Resolve every new import and reference: it must point at a class in this repo or at an external package already used by this repo for the same purpose.
- Check the split source sets: no client-only class referenced from src/main, no server-only logic placed in src/client.
- Check mixin targets and method names against the actual target classes as used elsewhere in the repo.
- Where dependency jars can be fetched directly over HTTP, run a javac pass against them as an extra check. If the jars are not available, say so in the change notes. An audit is never reported as a compile.
