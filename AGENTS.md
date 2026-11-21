# Mor App — Agent Instructions

## Screenshot and image workflow (required)

The provider rejects a request when the image content sent in that request
exceeds 30MB (`413`, `"Downloaded image content cannot exceed 30MB"`,
`payload_too_large`). This is a per-request budget, not a per-image budget:
small screenshots can still fail when many are accumulated in the session.

1. **Capture through the helper.** Use `scripts/mor-shot.sh <name>` for emulator
   screenshots, or `scripts/mor-shot.sh --prepare <input> <name>` for an
   existing image. It preserves the full-resolution original in
   `~/.mor-evidence/originals`, writes one compressed preview under 180KB to
   `~/.mor-evidence/previews`, validates bytes and dimensions, and prints the
   path. If it exits 32, the preview is rejected — do not send it.
2. **Check the budget before image-heavy work.** Run
   `scripts/mor-session-image-budget.sh`. It measures only the images in the
   active context (after the latest compaction). If it returns WARNING or
   CRITICAL, compact the session
   (`opencode api post /api/session/<id>/compact --data '{}'`) or start a fresh
   session before reading more images. Ordinary image compression alone does
   not clear accumulated history.
3. **One relevant preview at a time.** Do not read the same unchanged image
   again. Do not bulk-read a directory of screenshots.
4. **Prefer text for non-visual checks.** Use `adb shell uiautomator dump`,
   `adb logcat`, API output, or UI text for state/value checks. Use actual image
   inspection for layout, spacing, alignment, clipping, colour, and other visual
   checks.
5. **Never call a visual issue verified from text alone.** If you did not inspect
   the image, mark it captured-but-unverified (or not run), not verified.
6. **Keep evidence out of tracked source and out of reports.** Originals live in
   `~/.mor-evidence/originals`. Link paths in reports; do not embed images in
   progress notes or build reports.
7. **Checkpoint after each issue.** Update `/tmp/mor-progress.md` with what is
   done, what is next, and the failing step before starting the next image-heavy
   check.

## Mor build constraints

- Work on the current branch and working tree; do not reset, stash, checkout,
  revert, delete existing files, or commit unless Sameer explicitly asks.
- No server changes, deployments, restarts, or rollbacks.
- Do not overwrite existing APK deliverables; create a new filename.
- The FINAL build pack at `/Users/sameerhoda/Downloads/FINAL build pack` is the
  scope authority (`01-BUILD-SPEC.md` wins over `03-issue-evidence.md`).
