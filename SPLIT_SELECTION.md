# Manual split selection

Enable **Settings → Patcher → Choose splits before patching**. It is off by default.
For a split APK archive, the manager asks which modules to include before launching
its patcher worker. This applies to simple/expert single-app patching and to each
split archive in a batch, including installed apps already packed by the existing
source-selection flow.

- **Select all** includes every split.
- **Match device** uses the existing automatic ABI, system-language and density
  rules, including density fallback. It also retains modules the automatic filter
  treats as needed, such as feature modules. These are selected initially.
- **Deselect all** clears the optional splits. The base APK remains checked and
  locked, identified by its manifest rather than its filename.
- **Continue** starts patching with that exact selection. An empty selection means
  base only. Omitting a required feature/configuration may produce an unusable app;
  the dialog explains this and does not silently add optional splits.
- **Cancel** leaves single-app patching or cancels the batch queue before the
  current worker is started.

Manual selection takes precedence over automatic split filtering. Native-library
stripping is also suppressed for that manually selected archive, preserving the
contents of chosen ABI splits. Plain APKs still respect the existing stripping
setting and do not show a split dialog. With the new setting off, existing behavior
is unchanged.

The dialog uses `AppDialog`, `AppDialogButtonRow`, `ActionPillButton`,
`RadioSelectionCard` and `SelectionCheckIndicator`. Colors come from the existing
theme and dialog color providers; there is no separate hard-coded palette. Bulk
buttons wrap, long filenames wrap, and the list scrolls while the footer remains
available. Checkbox choices survive activity recreation. A process restart before
confirmation inspects the source again before allowing patching to start.

The explicit nullable selection travels through worker arguments and the serialized
`PatchRequest`, covering both ART and the embedded JVM (including JVM fallback).
The core checks entry names and enforces exactly one base before extracting manual
selections. A stale selection or an unreadable/ambiguous archive blocks progress.
Inspection runs off the UI thread. Canceling a suspended prompt does not launch a
worker; batch temporary inputs are cleaned up through its existing finally block.

When original APK retention is enabled, manual runs retain the complete original
archive instead of the reduced merged APK, allowing different choices next time.
If an earlier run already saved only a merged APK, choose the original split archive
again to make its splits available. Background batch runs pause for this dialog;
open the batch screen to answer it.

## Validation

Added regression tests:

- `ManualSplitSelectionTest`: explicit foreign ABI/language retention, select-all,
  base-only selection, manifest-based base identification, recommendation parity
  with automatic filtering, density fallback, exclusion of nested APK assets,
  unchanged automatic behavior, stale entries and missing/ambiguous bases.
- `PatchRequestSplitSelectionTest`: preserves the distinction between automatic
  (`null`), base-only (empty set) and explicit selections across JSON serialization.

Source whitespace checks and resource XML parsing passed. All newly referenced
string resources were checked against their definitions. Source files unrelated
to the change were compared against the uploaded archive.

The Gradle test/build attempt was blocked before project configuration: the wrapper
could not download `gradle-9.8.2-bin.zip` because network access was unavailable.
No Android build, Kotlin unit test execution or device/emulator UI verification is
claimed. Run these checks in the normal development environment:

```sh
./gradlew :patcher-core:test :app:compileDebugKotlin :app:lintDebug
```

Device checks still required:

1. Open the dialog in light/dark themes, with dynamic/app colors, large text and
   a long split list; rotate with a custom selection and verify it is retained.
2. Exercise each bulk action and individual checkboxes with automatic stripping
   both on and off. Inspect the output resources and `lib/` entries.
3. Patch a plain APK with the new setting on and verify that no dialog appears.
4. Cancel/back out, including within a batch, and verify that no worker starts.
5. Exercise ART and JVM patching, a foreign ABI/language selection and base-only
   selection. Validate the resulting APK where the selected contents are usable.
6. Patch again from the retained original and verify previously excluded splits
   can be selected.
