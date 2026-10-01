# webjars-locator-lite

A small, dependency-free Java library (`org.webjars:webjars-locator-lite`) that locates assets inside
WebJars on the classpath (`WebJarVersionLocator`). Published to Maven Central.

Follow the `zen-of-projects` Skill (extract it with `./mvnw -q skillsjars:extract`); this file records
only project-specific facts and exceptions.

## Skills

`zen-of-projects`, `zen-of-james` (from `com.jamesward:skills`, extracted to `.kiro/skills/`).

## MCP

`javadocs` (https://www.javadocs.dev/mcp), configured in `.mcp.json` / `.kiro/settings/mcp.json`. Use
its `get_latest_version` for version lookups and its source/doc tools for API questions.

## Build & test

- `./mvnw -B -ntp verify` (CI currently runs `mvn -B -ntp test` on Java 8).

## Exceptions to zen-of-projects

- **Java 8 bytecode:** this library targets Java 8 (`<source>`/`<target>` 1.8, CI on JDK 8) so it stays
  usable by old applications. Don't raise the target or use APIs newer than Java 8. Keep using
  `<source>`/`<target>` rather than `<release>8</release>`: with `--release 8`, javac checks annotations
  against the restricted Java 8 `ElementType` enum, which lacks `MODULE` — and `jspecify`'s `@NullMarked`
  targets `ElementType.MODULE`, so `--release 8` turns into an unsuppressable `-Xlint:all -Werror`
  failure ("unknown enum constant ... MODULE"). `<source>`/`<target>` compiles against the full modern
  JDK API instead, so the mismatch doesn't surface.
- **Strict compilation:** `-Xlint:all,-options -Werror` on `maven-compiler-plugin`. `-options` is
  suppressed because `<source>`/`<target>` 1.8 (see above) makes javac emit an unsuppressable-otherwise
  "source/target value 8 is obsolete" warning.
- **JUnit stays on 5.x:** `junit-bom` is pinned to the latest JUnit **5** release, not JUnit 6 (6.1.3 as
  of 2026-10-01). JUnit 6 raises its baseline to Java 17, which is incompatible with this project's
  Java 8 CI (`.github/workflows/test.yml` sets `java-version: 8`, so the whole Maven process — not just
  compiled bytecode — runs on JDK 8). Re-evaluate only if the Java 8 exception above is ever dropped.
- **Pinned WebJar test fixtures:** `bootstrap`, `bootswatch-yeti`, `js-base64`, `jquery`, `react`,
  `qrcodejs`, and `jquery-ui` test-scope dependencies are intentionally pinned to specific old versions.
  Each exercises a distinct version-string edge case that `WebJarVersionLocatorTest` asserts against
  verbatim (e.g. `qrcodejs`'s commit-hash version, `react`'s `-rc.3` suffix, `jquery-ui`'s `+1` build
  metadata, `bootstrap`'s `-1` WebJar-revision suffix). Don't bump them in the daily routine; a version
  bump would just need the hardcoded assertions updated for no behavioral benefit, and could silently
  drop the edge case the fixture exists to cover.
- **Releases:** `maven-release-plugin` plus `central-publishing-maven-plugin` (profile
  `sonatype-oss-release`, GPG signing). Never run a release or deploy in the daily routine.
