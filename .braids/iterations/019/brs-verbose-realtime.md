# Fix verbose mode: post step-by-step updates in real time (braids-brs)

## Summary
Updated orchestrator.md verbose mode section to post step-by-step updates as each orchestrator step completes, instead of a single summary after the tick.

## Details
The old verbose mode posted one big summary message after `braids orch-run` completed — by then the work was done or timed out, making it useless for debugging.

The new verbose mode posts 5 discrete messages at each step:
1. **Tick start** — `🤖 Orchestrator tick started`
2. **After sessions_list** — `📋 Sessions: ...` showing discovered labels
3. **After orch-run** — `⚙️ orch-run result: ...` with spawn/idle decision
4. **After each spawn** — `🏗️ Spawning...` / `✅ Worker spawned: ...`
5. **Tick complete** — `✅ Tick complete`

Each message is posted immediately as the step completes, giving real-time visibility into orchestrator behavior.

No code changes needed — this is a documentation/protocol change that the orchestrator agent follows.
