# Versioning And Branching

<!-- docs-nav-start -->
[Previous: Release Process](/project/release-process) | [Documentation Home](/) | [Next: Security Policy](/project/security)
<!-- docs-nav-end -->

Coredeux uses semantic versions for released artifacts and `SNAPSHOT` versions
for active development.

The simple rule is:

```text
features -> develop -> release/x.y.z -> main
```

Tags mark published builds:

```text
v0.1.0-M1
v0.1.0-M2
v0.1.0-RC1
v0.1.0
```

## Branch Structure

Use this branch structure:

```text
main                         stable / latest released code
develop                      active integration branch
feature/*                    individual feature branches
release/0.1.0                stabilization branch
hotfix/0.1.1                 urgent fixes after release
```

`main` should stay clean and stable. `develop` carries the next unreleased
`SNAPSHOT`. Create `release/x.y.z` only when the project is ready to publish
milestones, release candidates, or the final release for that version.

## Maven Version Meaning

| Branch | Maven version | Purpose |
| --- | --- | --- |
| `main` | `0.1.0` | Latest stable release |
| `develop` | `0.2.0-SNAPSHOT` | Next development version |
| `feature/*` | Same as `develop` | Feature work |
| `release/0.1.0` | `0.1.0-M1`, `0.1.0-RC1`, `0.1.0` | Stabilization |
| `hotfix/0.1.1` | `0.1.1-SNAPSHOT` to `0.1.1` | Emergency fixes |

## Start Development

Create `develop` from `main`:

```bash
git checkout main
git pull
git checkout -b develop
```

Set the Maven version to the next snapshot:

```text
0.1.0-SNAPSHOT
```

Feature branches come from `develop`:

```bash
git checkout develop
git checkout -b feature/0.1.0-ai-core
```

When the feature is complete, merge it back into `develop`:

```bash
git checkout develop
git merge feature/0.1.0-ai-core
```

So the normal feature flow is:

```text
feature/* -> develop
```

## Create A Milestone Release

When `develop` is ready for a milestone such as `0.1.0-M1`, create a release
branch:

```bash
git checkout develop
git checkout -b release/0.1.0
```

Change the Maven version to:

```text
0.1.0-M1
```

Commit, tag, and push:

```bash
git add .
git commit -m "Prepare 0.1.0-M1"
git tag v0.1.0-M1
git push origin release/0.1.0
git push origin v0.1.0-M1
```

Publish `0.1.0-M1` to the milestone repository.

Continue stabilization on `release/0.1.0`. For later milestones, update the
Maven version and tag each milestone:

```text
0.1.0-M2
0.1.0-M3
```

```bash
git tag v0.1.0-M2
git push origin v0.1.0-M2
```

## Create A Release Candidate

When the release branch is mostly stable, move to a release candidate:

```text
0.1.0-RC1
```

Commit and tag it:

```bash
git commit -am "Prepare 0.1.0-RC1"
git tag v0.1.0-RC1
git push origin v0.1.0-RC1
```

## Publish The Final Release

When the release is ready, change the Maven version to the final version:

```text
0.1.0
```

Commit and tag:

```bash
git commit -am "Release 0.1.0"
git tag v0.1.0
git push origin v0.1.0
```

Publish the final release to Maven Central.

Then merge the release branch into `main`:

```bash
git checkout main
git merge release/0.1.0
git push origin main
```

Also merge `main` back into `develop`:

```bash
git checkout develop
git merge main
```

Then bump `develop` to the next snapshot:

```text
0.2.0-SNAPSHOT
```

Commit and push:

```bash
git commit -am "Start 0.2.0-SNAPSHOT development"
git push origin develop
```

## Hotfixes

Use a hotfix branch when a released version needs an urgent fix.

For example:

```bash
git checkout main
git checkout -b hotfix/0.1.1
```

Set the Maven version to:

```text
0.1.1-SNAPSHOT
```

After the fix is complete, stabilize the branch, set the final Maven version to
`0.1.1`, tag it as `v0.1.1`, merge it into `main`, and merge `main` back into
`develop`.

## Branch Naming

Use names that describe the target version and the work:

```text
feature/0.1.0-ai-core
feature/0.1.0-bom
feature/0.1.0-starter
release/0.1.0
hotfix/0.1.1
```

Avoid using a long-lived feature branch as the main development branch:

```text
feature/0.1.0-SNAPSHOT
```

Use `develop` for active integration instead.

## Practical Coredeux Rule

Keep `main` stable, use `develop` for `SNAPSHOT`, and create `release/x.y.z`
only when you are ready to publish milestones.

<!-- docs-nav-start -->
[Previous: Release Process](/project/release-process) | [Documentation Home](/) | [Next: Security Policy](/project/security)
<!-- docs-nav-end -->
