[![Release](https://jitpack.io/v/umjammer/jlha.svg)](https://jitpack.io/#umjammer/jlha)
[![Java CI](https://github.com/umjammer/jlha/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/jlha/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/jlha/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/jlha/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)
[![Parent](https://img.shields.io/badge/Parent-vavi--util--archive-pink)](https://github.com/umjammer/vavi-util-archive)

# LHA for Java

<img alt="logo" src="src/test/resources/duke_trunk.png" width="160" />

🗜️LHA compatible archiving library works with Java.

## Install

https://jitpack.io/#umjammer/jlha

## Usage

```java
    LhaFile file = new LhaFile(Paths.get("src/test/resources/test.lzh").toFile());
    Path outDir = Paths.("/foo/bar");
    for (LhaHeader header : file.getEntries()) {
        Files.copy(file.getInputStream(header), Files.newOutputStream(outDir.resolve(header.getPath())));
    }
```

## References

- [original](https://web.archive.org/web/20170909125843/http://dangan.g.dgdg.jp/)

## TODO

- ~~unit tests~~

---

<sub>image designed by @umjammer, drawn by nano banana</sub>
