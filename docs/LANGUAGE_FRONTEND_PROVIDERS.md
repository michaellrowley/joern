# Language Frontend Providers in Joern

## Overview

This document explains who provides the language frontends (like `rubysrc2cpg`, `swiftsrc2cpg`, etc.) for different programming languages in Joern.

## Short Answer

**All language frontends are part of the Joern repository itself**, located in `joern-cli/frontends/`. They are maintained by the Joern project (joernio organization on GitHub).

## Detailed Breakdown by Language

### Languages with Joern-Maintained Frontends + External AST Generators

Some frontends are written in Scala/Java but rely on external tools to generate Abstract Syntax Trees (ASTs):

| Language | Frontend | External AST Generator | Technology |
|----------|----------|------------------------|------------|
| **Ruby** | `rubysrc2cpg` | [joernio/ruby_ast_gen](https://github.com/joernio/ruby_ast_gen) | Ruby `parser` gem, executed via JRuby |
| **Swift** | `swiftsrc2cpg` | [joernio/swiftastgen](https://github.com/joernio/swiftastgen) | Swift Syntax parser, native binary |
| **JavaScript/TypeScript** | `jssrc2cpg` | [joernio/astgen](https://github.com/joernio/astgen) | Babel parser, native binary |

These external tools are **also maintained by the joernio organization** and bundled with Joern.

### Languages Using Third-Party Parsers

These frontends use established, well-maintained parsing libraries:

| Language | Frontend | Parser/Technology | Source |
|----------|----------|-------------------|--------|
| **Java** | `javasrc2cpg` | [JavaParser](https://javaparser.org) | Open source Java parsing library |
| **C/C++** | `c2cpg` | [Eclipse CDT](https://wiki.eclipse.org/CDT) | Eclipse C/C++ Development Tooling |
| **Java Bytecode** | `jimple2cpg` | [Soot/Jimple](https://github.com/soot-oss/soot) | Java bytecode analysis framework |

### Languages with Self-Contained Frontends

These are implemented entirely within the Joern repository:

| Language | Frontend | Notes |
|----------|----------|-------|
| **Python** | `pysrc2cpg` | Uses Python's built-in parsing capabilities |
| **PHP** | `php2cpg` | Custom PHP parser implementation |
| **Go** | `gosrc2cpg` | Uses Go's standard library parser |
| **C#** | `csharpsrc2cpg` | Custom C# parser implementation |
| **Kotlin** | `kotlin2cpg` | Custom Kotlin parser implementation |

### Binary Analysis Frontends

| Tool | Frontend | Purpose | Technology |
|------|----------|---------|------------|
| **Ghidra** | `ghidra2cpg` | Binary/executable analysis | [NSA's Ghidra](https://ghidra-sre.org/) reverse engineering tool |

## Architecture Pattern

All frontends follow a common pattern:

1. **Frontend Implementation** (in `joern-cli/frontends/<lang>2cpg/`)
   - Written in Scala
   - Implements the CPG generation logic
   - Converts AST to Code Property Graph format

2. **AST Generation** (varies by language)
   - External tool (Ruby, Swift, JS/TS)
   - Third-party library (Java, C/C++)
   - Self-contained (Python, Go, PHP)

3. **CPG Integration** (in `console/src/main/scala/io/joern/console/cpgcreation/`)
   - Generator class (e.g., `RubyCpgGenerator`, `SwiftSrcCpgGenerator`)
   - Registered in `ImportCode.scala`
   - Language detection in `package.scala`

## For Ada Language Support

Based on this pattern, to add Ada support (`adasrc2cpg`), the Joern project would need to:

1. **Create the frontend** in `joern-cli/frontends/adasrc2cpg/`
   - Implement in Scala following existing patterns
   
2. **Choose an AST generation approach**:
   - **Option A**: Create a new external tool (like `joernio/adaastgen`)
   - **Option B**: Use an existing Ada parser library (e.g., [libadalang](https://github.com/AdaCore/libadalang))
   - **Option C**: Implement a custom parser in the frontend itself

3. **Integrate with Joern console** (already done in this PR)
   - `AdaCpgGenerator` class
   - Registration in `ImportCode.scala`
   - Language detection for `.adb` and `.ads` files

## Maintenance and Contributions

All language frontends are:
- **Open source** (Apache 2.0 license)
- **Maintained by the Joern community**
- **Part of the main Joern repository** (except external AST generators)
- **Built and distributed together** with Joern releases

External AST generators (ruby_ast_gen, swiftastgen, astgen) are maintained in separate repositories but are also part of the joernio GitHub organization.

## Summary

**The Joern project (joernio organization) provides and maintains all language frontends.** Some rely on external tools (also maintained by joernio) or third-party parsing libraries, but the CPG generation logic is always part of Joern itself.

For Ada or any new language, the frontend would need to be:
1. Implemented in the Joern repository
2. Maintained by the Joern community
3. Integrated following the established patterns

There is no "external provider" for language frontends - they are all part of the Joern ecosystem.
