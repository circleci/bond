# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0]
### Removed
- Support for ClojureScript. Please pin to 0.4.0 if you need it.

### Changed
- `bond.james/calls` throws an exception if the function hasn't been spied or stubbed.
- Tests against Clojure 1.10 (bumped from 1.7).

## [0.4.0]
### Added
- New assertions `called?`, `called-times?`, and `called-with-args?`.

## [0.3.2]
### Changed
- `bond.james/stub!` and `bond.james/with-stub!` throw with an explanation when `:argslist` is missing.

## [0.3.1]
### Added
- Improved support for stubbing private functions.

### Changed
- Removed runtime dependencies on Clojure and ClojureScript.

## [0.3.0]
### Added
- `bond.james/with-stub!` which throws an exception when there's an arity mismatch.
