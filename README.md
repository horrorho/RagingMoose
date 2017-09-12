# RagingMoose

Experimental Java LZFSE capable decompressor. Work in progress.

## What is it?

An experimental Java [LZFSE](https://github.com/lzfse/lzfse) capable decompressor created as a fallback solution for [InflatableDonkey](https://github.com/horrorho/InflatableDonkey). The codebase has been designed from the ground up and barring constants/ tables and a few core routines, has little in the way of resemblance to the source material.

I've opted for simplicity and there's little in the way of optimisation. It's presumed that compression will not be supported.

At present the code base is essentially complete. Unit tests are in place with support for extended tests using [tcgen](http://mattmahoney.net/dc/tcgen100.zip) and an external reference [lzfse](https://github.com/lzfse/lzfse) binary (see below). Tests on malformed input data are still required.

## Should I use it?

I would **strongly** suggest using the reference [lzfse](https://github.com/lzfse/lzfse) compressor in some manner instead if at all possible. An example using [ProcessBuilder](https://docs.oracle.com/javase/8/docs/api/java/lang/ProcessBuilder.html) can be found [here](https://github.com/horrorho/RagingMoose/blob/master/src/test/java/com/github/horrorho/ragingmoose/LZFSEInputStreamTest.java#L142). This decompressor should be used as a last resort. It may contain bugs (evil ones at that).

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
    Path path = Paths.get("mytext.lzfse"); // your LZFSE compressed text file here
    
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


## TODO
- Unit tests on malformed LZFSE input data.


## Links
[LZFSE](https://github.com/lzfse/lzfse) - reference implementation.

[Asymmetric_Numeral_Systems](https://en.wikipedia.org/wiki/Asymmetric_Numeral_Systems) - bed time reading.

[Finite State Entropy - A new breed of entropy coder](http://fastcompression.blogspot.co.uk/2013/12/finite-state-entropy-new-breed-of.html) - wonderful series of articles.

[Compression test file generator](https://encode.ru/threads/1306-Compression-test-file-generator) - Interesting thread post by [Matt Mahoney](http://mattmahoney.net/) author of `tcgen`. I've warped some of the examples into unit tests.
