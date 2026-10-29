---
description: Run the project harness check (./gradlew test) per .agent-os/harness.md and report pass/fail.
---

Run the harness automated check exactly as defined in `.agent-os/harness.md`:

```bash
./gradlew test --console=plain --no-daemon
```

After it finishes, report:

- **Pass / fail status** (per the harness "Pass condition: all tests green").
- **New test failures**: test name + assertion line for each failure.
- **New deprecation warnings** vs the previous run (if any).
- On full pass: confirm "all tests green" exactly, and remind that this is only the *Automated check* — the 7 manual sections (Intent / Requirements / Code / Test / Operations / Security / Reward Hacking) in `.agent-os/harness.md` are separate.

Do not modify code as part of running the harness — if tests fail, surface the failures back to the user for direction before making any fixes.
