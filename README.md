# Kotlin JVM Template
[![Build](https://github.com/Virtlink/refret-eclipse-testcase-generator/actions/workflows/build.yml/badge.svg)](https://github.com/Virtlink/refret-eclipse-testcase-generator/actions)
[![License](https://img.shields.io/github/license/Virtlink/refret-eclipse-testcase-generator)](https://github.com/Virtlink/refret-eclipse-testcase-generator/blob/main/LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/Virtlink/myapp)](https://github.com/Virtlink/myapp/releases)

Generates SPT test cases from Eclipse unit tests for the Reference Retention paper.

## Quick Start
Download/clone this project and build it:

```shell
./gradlew installShadowDist
```


## Usage
Clone the Eclipse JDI UI and IntelliJ repositories. We used a specific commit on their main branches.

```shell
git clone --recurse-submodules --remote-submodules -j8 --depth=1 git@github.com:eclipse-jdt/eclipse.jdt.ui.git eclipse
cd eclipse
git checkout 79766bc97903d157b852359d5084ec6a03935fce

cd ..

git clone --recurse-submodules --remote-submodules -j8 --depth=1 git@github.com:JetBrains/intellij-community.git intellij
cd intellij
git checkout f40b4d47bae224d4a1176b8da1c1b6136f1540a0
```

From the root of this repository, move to the directory where the tool's invocation script can be found.  Then invoke this tool on the refactoring resources directory of Eclipse and Intellij, providing also an output directory for the generated test suite Java files.

```shell
cd rr-test-generator/build/install/rr-test-generator-shadow/bin/

./rr-test-generator discover-eclipse \
  eclipse/org.eclipse.jdt.ui.tests.refactoring/resources  # input directory with refactoring resources
  --out eclipse-tests/                                    # output directory where test suites are placed

./rr-test-generator discover-intellij \
  intellij/java/java-tests/testData/refactoring           # input directory with refactoring resources
  --out intellij-tests/                                   # output directory where test suites are placed
```

Adjust the generated files in `tests/` to have references and declarations. A declaration is a name `x` surrounded by `[[id|x]]`, where `id` is a unique ID you can use to refer to this declaration.  A reference `x` to a declaration with id `id` is similarly written as `[[->id|x|y]]`, where `x` is the reference before reference retention and `y` is the expected reference after performing reference retention.  It is assumed that the program parses correctly in both the _original_ (before) and _expected_ (after) cases, and is semantically correct in the _expected_ (after) case.  For example:

```java
test;
[p] {
  [A]
  package p;
  class A {
      int [[1|x]] = 2; // target
  }
  class B extends A {
      int x = 3;
      class C {
          int y = [[->1|x|B.super.x]]; // reference
      }
  }
}
```

Finally, generate SPT tests from the adjusted test suites.  Specify the directory with the test suites as the input argument, and a directory where the SPT tests should be placed as the output argument.

```shell
cd rr-test-generator/build/install/rr-test-generator-shadow/bin/
./rr-test-generator generate \
  tests/                      # input directory with the adjusted test suites
  --out spttests/             # output directory where the SPT tests are placed
```


## License
Copyright © 2023 Daniel A. A. Pelsmaeker

Licensed under the Apache License, Version 2.0 (the "License"); you may not use files in this project except in compliance with the License. You may obtain a copy of the License at <https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an **"as is" basis, without warranties or conditions of any kind**, either express or implied. See the License for the specific language governing permissions and limitations under the License.
