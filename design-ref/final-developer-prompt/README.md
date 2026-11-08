# Mor final developer prompt — pack contents

Paste `MOR-FINAL-DEVELOPER-PROMPT.md` to the developer LLM as the prompt.
Attach the SUPPORT files alongside it — they are verified ground truth
from this repo (Nov 2026) and override any conflicting candidates in the brief.

Read order for the developer LLM: prompt first, then SUPPORT-01,
SUPPORT-02, SUPPORT-03. The prompt says the same.

| File | What it is |
| --- | --- |
| [MOR-FINAL-DEVELOPER-PROMPT.md](MOR-FINAL-DEVELOPER-PROMPT.md) | The final prompt. Text only, no code/images/animation yet. |
| [SUPPORT-01-verified-baseline.md](SUPPORT-01-verified-baseline.md) | Step 0 baseline, verified line-by-line against `app/src`. |
| [SUPPORT-02-feedback-status.md](SUPPORT-02-feedback-status.md) | Every previously requested fix: implemented, partial, or missing, with file evidence. |
| [SUPPORT-03-library-inventory.md](SUPPORT-03-library-inventory.md) | Verified contents of the Mor Design Library (Drive download). |

No code changes, no asset exports, no generated screens in this stage.
The `modd_board/` originals and `design-ref/organized/` working copies are untouched.
