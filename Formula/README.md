# Formula

This directory contains [Homebrew](https://brew.sh/) formula(s) for installing braids.

## Usage

To install braids via Homebrew:

```bash
brew install slagyr/braids/braids
```

Or, add the tap first and then install:

```bash
brew tap slagyr/braids https://github.com/slagyr/braids.git
brew install braids
```

## Development

The formula in `braids.rb` defines how Homebrew builds and installs braids. It:

- Installs all project files into Homebrew's libexec directory
- Creates a wrapper script in the bin directory that invokes braids via Babashka
- Depends on [Babashka](https://github.com/babashka/babashka) and [Beads](https://github.com/slagyr/beads)

When updating the formula, be sure to update the `tag:` version in the `url` line to match the latest release.
