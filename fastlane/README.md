fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android fireInternal

```sh
[bundle exec] fastlane android fireInternal
```

Submit a new dev Build to firebase

### android fireStaging

```sh
[bundle exec] fastlane android fireStaging
```

Submit a new dev Build to firebase

### android fireTrainingInternal

```sh
[bundle exec] fastlane android fireTrainingInternal
```

Submit a new training Build to firebase

### android fireTraining

```sh
[bundle exec] fastlane android fireTraining
```

Submit a new training Build to firebase

### android beta

```sh
[bundle exec] fastlane android beta
```

Submit a new Beta Build to Crashlytics Beta

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Deploy a new version to the Google Play

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
