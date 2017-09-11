# RagingMoose

Experimental Java LZFSE capable decompressor. Work in progress.

## What is it?

An experimental Java [LZFSE](https://github.com/lzfse/lzfse) capable decompressor created as a fallback solution for [InflatableDonkey](https://github.com/horrorho/InflatableDonkey).

The codebase for the most part revolves around simplicity and there's little in the way of optimisation. It's presumed that compression will not be supported.

Although essentially complete it has **not** been adequately tested at this point.

## Should I use it?

I would **strongly** suggest using the reference [LZFSE](https://github.com/lzfse/lzfse) compressor in some manner instead if at all possible. An example using [Runtime.exec](https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#exec-java.lang.String:A-java.lang.String:A-java.io.File-) can be found [here](https://github.com/horrorho/InflatableDonkey/blob/master/src/main/java/com/github/horrorho/inflatabledonkey/util/LZFSEExtInputStream.java). This decompressor should be used as a last resort. It may contain bugs (evil ones at that).

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


## What's with the name?

I'm not quite sure... It seemed like a good idea at the time.


## TODO
- Unit tests


## Links
[LZFSE](https://github.com/lzfse/lzfse) - reference implementation.

[Asymmetric_Numeral_Systems](https://en.wikipedia.org/wiki/Asymmetric_Numeral_Systems) - bed time reading.

[Finite State Entropy - A new breed of entropy coder](http://fastcompression.blogspot.co.uk/2013/12/finite-state-entropy-new-breed-of.html) - wonderful series of articles.
