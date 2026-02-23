# Colorize braids help output (braids-ryb)

## Summary

Added ANSI color codes to `braids help` output for improved readability.

## Color Scheme

- **Title** ("braids"): Bold white (`\033[1;37m`)
- **Usage:** line: Bold cyan (`\033[1;36m`)
- **Section headers** (Commands:, Options:): Bold yellow (`\033[1;33m`)
- **Command names**: Bold blue (`\033[1;34m`)
- **Descriptions**: Default terminal color (white/uncolored)

## Changes

- `src/braids/core.clj` — Added `ansi` color map, `c` helper function, and updated `help-text` to apply colors
- `spec/braids/core_spec.clj` — Added 5 tests verifying color codes in help output

## Verification

```
$ bb braids help
[bold white]braids[reset] — CLI for the braids skill

[bold cyan]Usage:[reset] braids <command> [args...]

[bold yellow]Commands:[reset]
  [bold blue]config      [reset]Get/set/list braids configuration
  [bold blue]help        [reset]Show this help message
  ...

[bold yellow]Options:[reset]
  [bold blue]-h, --help[reset]   Show this help message
```

Test results: All 5 new color tests pass. Pre-existing test count unchanged (12 pre-existing failures, same as before this change).
