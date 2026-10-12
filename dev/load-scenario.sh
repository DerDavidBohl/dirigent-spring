#!/usr/bin/env bash
# Usage: load-scenario.sh <name> <state-dir>. Builds local Git repos for a scenario from dev/scenarios/<name>.
set -euo pipefail

name="$1"
state="$(cd "$2" && pwd)"
src="$(cd "$(dirname "$0")" && pwd)/scenarios/$name"
[ -d "$src" ] || { echo "Unknown scenario '$name'. Available: $(ls "$(dirname "$src")" | tr '\n' ' ')" >&2; exit 1; }

target="$state/scenario"
rm -rf "$target"
mkdir -p "$target"
cp -r "$src/repos" "$target/repos"
cp -r "$src/config" "$target/config"
sed -i "s|@REPOS@|$target/repos|g" "$target/config/deployments.yml"

for repo in "$target"/repos/* "$target/config"; do
  git -C "$repo" init -q -b main
  git -C "$repo" add -A
  git -C "$repo" -c user.name=dev -c user.email=dev@localhost commit -q -m "Scenario $name"
done
echo "Scenario '$name' loaded. Start the environment with 'make dev'."
