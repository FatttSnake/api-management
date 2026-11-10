/**
 * Strips native libraries for non-target platforms from a directory
 * extracted via Spring Boot layertools' dependencies/ layer.
 *
 * Keeps only linux/amd64 and linux/arm64/v8 (Linux x86_64 + aarch64).
 *
 * Usage: java StripNative.java &lt;dependencies-dir&gt;
 * e.g.   java StripNative.java target/extracted/dependencies/BOOT-INF/lib
 *
 * Depends only on the JDK — no third-party libraries.
 */

private static final Rule[] RULES = {
        // sqlite-jdbc — keep only Linux/x86_64 and Linux/aarch64
        new Rule("sqlite-jdbc-",
                "org/sqlite/native/FreeBSD/",
                "org/sqlite/native/Mac/",
                "org/sqlite/native/Windows/",
                "org/sqlite/native/Linux-Musl/",
                "org/sqlite/native/Linux-Android/",
                "org/sqlite/native/Linux/arm/",
                "org/sqlite/native/Linux/armv6/",
                "org/sqlite/native/Linux/armv7/",
                "org/sqlite/native/Linux/ppc64/",
                "org/sqlite/native/Linux/x86/",
                "org/sqlite/native/Linux/riscv64/"
        ),
        // aircompressor-v3 — keep only linux-amd64 and linux-aarch64
        new Rule("aircompressor-v3-",
                "aircompressor/linux-ppc64le/",
                "aircompressor/macos-aarch64/",
                "aircompressor/macos-amd64/"
        ),
        // JNA (excluding -platform) — keep only linux-x86-64 and linux-aarch64
        new Rule("jna-",
                "com/sun/jna/win32-x86/",
                "com/sun/jna/win32-x86-64/",
                "com/sun/jna/win32-aarch64/",
                "com/sun/jna/darwin-x86-64/",
                "com/sun/jna/darwin-aarch64/",
                "com/sun/jna/sunos-x86/",
                "com/sun/jna/sunos-x86-64/",
                "com/sun/jna/sunos-sparc/",
                "com/sun/jna/sunos-sparcv9/",
                "com/sun/jna/freebsd-x86/",
                "com/sun/jna/freebsd-x86-64/",
                "com/sun/jna/linux-x86/",
                "com/sun/jna/linux-arm/",
                "com/sun/jna/linux-armel/",
                "com/sun/jna/linux-ppc/",
                "com/sun/jna/linux-ppc64le/",
                "com/sun/jna/linux-loongarch64/",
                "com/sun/jna/linux-mips64el/",
                "com/sun/jna/linux-s390x/",
                "com/sun/jna/linux-riscv64/"
        ),
};

void main(String[] args) throws IOException {
    if (args.length < 1) {
        System.err.println("Usage: java StripNative.java <dependencies/lib-dir>");
        System.err.println("e.g.:  java StripNative.java target/extracted/dependencies/BOOT-INF/lib");
        System.exit(1);
    }

    Path libDir = Path.of(args[0]);
    if (!Files.isDirectory(libDir)) {
        System.err.println("Error: " + libDir + " is not a valid directory");
        System.exit(1);
    }

    List<StripResult> results = new ArrayList<>();

    // Scan all jars in the directory
    try (Stream<Path> files = Files.list(libDir)) {
        for (Path jarFile : (Iterable<Path>) files::iterator) {
            if (!jarFile.toString().endsWith(".jar")) continue;

            String fileName = jarFile.getFileName().toString();

            // Find a matching rule
            Rule rule = null;
            for (Rule r : RULES) {
                // jna-platform is pure Java, no native libs — skip
                if (fileName.startsWith("jna-") && fileName.contains("-platform")) continue;
                if (fileName.startsWith(r.jarPrefix)) {
                    rule = r;
                    break;
                }
            }
            if (rule == null) continue;

            long before = Files.size(jarFile);
            long after = stripJar(jarFile, rule.prefixes);
            results.add(new StripResult(fileName, before, after));
        }
    }

    // ──── Print report ────
    IO.println();
    IO.println("  ┌─────────────────────────────────────────────────────────────┐");
    IO.println("  │  StripNative — strip native libs for non-target platforms  │");
    IO.println("  │  Kept: linux/amd64 + linux/arm64/v8                        │");
    IO.println("  └─────────────────────────────────────────────────────────────┘");
    IO.println();

    long totalBefore = 0;
    long totalAfter = 0;

    for (StripResult r : results) {
        totalBefore += r.before;
        totalAfter += r.after;
        long saved = r.before - r.after;
        if (saved > 0) {
            System.out.printf("  ✓ %-50s %s → %s  (-%s)%n",
                    r.fileName,
                    fmt(r.before), fmt(r.after), fmt(saved));
        }
    }

    if (results.isEmpty()) {
        IO.println("  (no jars to process)");
    }

    IO.println();
    IO.println("  ──────────────────────────────────────────────────────────────");
    System.out.printf("  Processed %d jars,  total size %s → %s,  saved %s%n",
            results.size(),
            fmt(totalBefore), fmt(totalAfter), fmt(totalBefore - totalAfter));
    IO.println();
}

// ─────────────────────── Core: strip entries from a jar ───────────────────────

private static long stripJar(Path jarFile, List<String> deletePrefixes) throws IOException {
    Path tmpFile = jarFile.resolveSibling(jarFile.getFileName() + ".stripped");

    // Clean up old temp file
    Files.deleteIfExists(tmpFile);

    try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(jarFile));
         ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(tmpFile))) {

        byte[] buf = new byte[32768];
        ZipEntry entry;

        outer:
        while ((entry = zin.getNextEntry()) != null) {
            String name = entry.getName();
            for (String prefix : deletePrefixes) {
                if (name.startsWith(prefix)) {
                    continue outer;
                }
            }
            // Copy constructor — preserves original entry timestamps, method, etc.
            zout.putNextEntry(new ZipEntry(entry));
            int len;
            while ((len = zin.read(buf)) > 0) {
                zout.write(buf, 0, len);
            }
            zout.closeEntry();
        }
    }

    try {
        Files.move(tmpFile, jarFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
        // Retry: transient file lock on Windows
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
        Files.move(tmpFile, jarFile, StandardCopyOption.REPLACE_EXISTING);
    }

    return Files.size(jarFile);
}

// ───────────────────────────── Utilities ─────────────────────────────

private record Rule(String jarPrefix, List<String> prefixes) {
    Rule(String jarPrefix, String... prefixes) {
        this(jarPrefix, List.of(prefixes));
    }
}

private record StripResult(String fileName, long before, long after) { }

private static String fmt(long bytes) {
    if (bytes > 1048576) {
        return String.format("%.1f MB", bytes / 1048576.0);
    } else if (bytes > 1024) {
        return String.format("%.1f KB", bytes / 1024.0);
    }
    return bytes + " B";
}
