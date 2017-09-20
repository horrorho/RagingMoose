# RagingMoose

Experimental Java LZFSE capable decompressor.


## What is it?

An experimental Java [LZFSE](https://github.com/lzfse/lzfse) capable decompressor created as a fallback solution for [InflatableDonkey](https://github.com/horrorho/InflatableDonkey). The codebase has been designed from the ground up and barring constants/ tables and a few core routines, has little in the way of resemblance to the source material.

I've opted for simplicity and there's little in the way of optimisation. It's presumed that compression will not be supported.

Unit tests are in place with support for extended tests using [tcgen](https://gist.github.com/horrorho/7837e9b83f2aa42d2781374c99fd0ba3) and an external reference [lzfse](https://github.com/lzfse/lzfse) binary (see below). However the decompressor has not been battle tested and bugs may remain.


## Should I use it?

The raison d'être of RagingMoose is it's ease of integration into Java projects without the use of external dependencies/ interfacing.

However I would **strongly** suggest using the reference [lzfse](https://github.com/lzfse/lzfse) compressor in some manner instead if at all possible:

- Reference lzfse executable/ ProcessBuilder:
A (convoluted) example using [ProcessBuilder](https://docs.oracle.com/javase/8/docs/api/java/lang/ProcessBuilder.html) can be found [here](https://github.com/horrorho/RagingMoose/blob/master/src/test/java/com/github/horrorho/ragingmoose/LZFSEInputStreamTest.java#L142). It uses a simple utility class [ProcessAssistant](https://github.com/horrorho/RagingMoose/blob/master/src/test/java/com/github/horrorho/ragingmoose/ProcessAssistant.java) to handle concurrent streaming.

- Reference lzfse library/ JNI:
[JNI](https://en.wikipedia.org/wiki/Java_Native_Interface) example [header](https://gist.github.com/horrorho/b5b2f7eadfa1d73560dadbe4a0a92b85) and [c code](https://gist.github.com/horrorho/1f9ab1742355c1edcb339935657bff31) for the following call:

```Java
    public static native long decompress(ByteBuffer src, ByteBuffer dst);
```

The detailed mechanics of JNI are beyond the scope of this readme.

Benchmarks comparing RagingMoose and the reference lzfse via JNI/ ProcessBuilder are detailed below.


## How do I use it?

Create an instance of [LZFSEInputStream](https://github.com/horrorho/RagingMoose/blob/master/src/main/java/com/github/horrorho/ragingmoose/LZFSEInputStream.java) and consume/ close as an [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html).

The native constructor accepts [ReadableByteChannel](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/ReadableByteChannel.html)s.

```Java
    public LZFSEInputStream(ReadableByteChannel ch) {
        ...
    }
```


The InputStream constructor wraps over the native constructor using the [Channels#newChannel](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/Channels.html#newChannel-java.io.InputStream-) adapter. 

```Java
    public LZFSEInputStream(InputStream is) {
        this(Channels.newChannel(is));
    }
```


A simple example that decompresses and prints the contents of an LZFSE compressed text archive. [LZFSEDecoderException](https://github.com/horrorho/RagingMoose/blob/master/src/main/java/com/github/horrorho/ragingmoose/LZFSEDecoderException.java)s signify errors in the underlying data format.

```Java
    Path path = Paths.get("my.lzfse.compressed.text.file"); // your LZFSE compressed text file here
    
    try (LZFSEInputStream is = new LZFSEInputStream(Files.newByteChannel(path));
            ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        int b;
        while ((b = is.read()) != -1) {
            baos.write(b);
        }
        System.out.println(baos.toString("UTF-8"));

    } catch (LZFSEDecoderException ex) {
        System.err.println("Bad LZFSE archive: " + path);

    } catch (IOException ex) {
        System.err.println("IOException: " + ex.toString());
    }
```

## Benchmarks
Decompression benchmarks using [JMH](http://openjdk.java.net/projects/code-tools/jmh/). The core benchmarking code is [here](https://gist.github.com/horrorho/56eb417ac415c3aa0893849713d54750). Tests are conducted on in-memory byte data. Use the figures as a rough guide only as your use case/ environment may differ significantly.

- iOS 11 sqlitedb file. 460 bytes (16,384 bytes bytes uncompressed):

```
Benchmark                            Mode  Cnt      Score     Error  Units
LZFSEBenchmark.lzfseJNI             thrpt   20  32444.002 ± 300.831  ops/s
LZFSEBenchmark.lzfseProcessBuilder  thrpt   20   2086.615 ± 130.519  ops/s
LZFSEBenchmark.ragingMoose          thrpt   20  12571.402 ± 458.201  ops/s
```

- J.R.R.Tolkien, The Hobbit. Chapter 1. 47,257 bytes (18,905 bytes compressed):

```
Benchmark                            Mode  Cnt     Score     Error  Units
LZFSEBenchmark.lzfseJNI             thrpt   20  5404.936 ± 379.371  ops/s
LZFSEBenchmark.lzfseProcessBuilder  thrpt   20  1900.292 ±   8.834  ops/s
LZFSEBenchmark.ragingMoose          thrpt   20  1631.971 ± 102.401  ops/s
```

 - Homer, The Iliad. Full. 808,298 bytes (297,947 bytes compressed):

```
Benchmark                            Mode  Cnt    Score    Error  Units
LZFSEBenchmark.lzfseJNI             thrpt   20  307.237 ± 26.004  ops/s
LZFSEBenchmark.lzfseProcessBuilder  thrpt   20  251.843 ±  1.500  ops/s
LZFSEBenchmark.ragingMoose          thrpt   20  116.270 ±  7.660  ops/s
```

- [Rust-lang](https://github.com/rust-lang/rust), rust-master tarball. 42,270,720 bytes (6,813,253 bytes compressed):

```
Benchmark                            Mode  Cnt   Score   Error  Units
LZFSEBenchmark.lzfseJNI             thrpt   20  11.388 ± 0.220  ops/s
LZFSEBenchmark.lzfseProcessBuilder  thrpt   20   5.989 ± 0.166  ops/s
LZFSEBenchmark.ragingMoose          thrpt   20   4.583 ± 0.150  ops/s
```

## Extended unit tests
[LZFSEInputStreamTest#tcgenTest](https://github.com/horrorho/RagingMoose/blob/master/src/test/java/com/github/horrorho/ragingmoose/LZFSEInputStreamTest.java#L107) is by default set to `@Ignore`. It requires both the [tcgen](https://gist.github.com/horrorho/7837e9b83f2aa42d2781374c99fd0ba3) and [lzfse](https://github.com/lzfse/lzfse) binaries to be on the command path either as `.exe` binaries or their extensionless counterparts.

With the binaries in place and `@Ignore` deleted/ commented out an additional series of tests is performed. These generate test data using `tcgen`. This data is then compressed with the reference `lzfse` binary and in turn decompressed by RagingMoose. We expected the decompressed data to match the generated test data.

The data is cut to predetermined lengths to hit the various underlying block types (bvx-, bvxn, bvx2).

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.github.horrorho.ragingmoose.LZFSEInputStreamTest
Tests run: 32, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 14.196 sec - in com.github.horrorho.ragingmoose.LZFSEInputStreamTest

Results :

Tests run: 32, Failures: 0, Errors: 0, Skipped: 1
```

[LZFSEInputStreamTest#tcgenTestExt](https://github.com/horrorho/RagingMoose/blob/master/src/test/java/com/github/horrorho/ragingmoose/LZFSEInputStreamTest.java#L143) is by default set to `@Ignore`. It's essentially a sanity test and bypasses RagingMoose entirely and compresses/ decompresses using [lzfse](https://github.com/lzfse/lzfse). It's not suitable as a benchmark as the chokepoint is in [tcgen](https://gist.github.com/horrorho/7837e9b83f2aa42d2781374c99fd0ba3) test data generation.


## What's with the name?

I'm not quite sure... It seemed like a good idea at the time.


## Links
[LZFSE](https://github.com/lzfse/lzfse) - reference implementation.

[Asymmetric_Numeral_Systems](https://en.wikipedia.org/wiki/Asymmetric_Numeral_Systems) - bed time reading.

[Finite State Entropy - A new breed of entropy coder](http://fastcompression.blogspot.co.uk/2013/12/finite-state-entropy-new-breed-of.html) - wonderful series of articles.

[Compression test file generator](https://encode.ru/threads/1306-Compression-test-file-generator) - interesting thread post by [Matt Mahoney](http://mattmahoney.net/) author of `tcgen`. I've warped some of the examples into unit tests.
