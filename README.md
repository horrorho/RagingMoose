# RagingMoose

Experimental Java LZFSE capable decompressor. Work in progress.

## What is it?

An experimental Java [LZFSE](https://github.com/lzfse/lzfse) capable decompressor created as a fallback solution for [InflatableDonkey](https://github.com/horrorho/InflatableDonkey).

The codebase for the most part revolves around simplicity and there's little in the way of optimisation. It's presumed that compression will not be supported.

Although essentially complete it has **not** been adequately tested at this point.

## Should I use it?

I would **strongly** suggest using the reference [LZFSE](https://github.com/lzfse/lzfse) compressor in some manner instead if at all possible. An example using [Runtime.exec](https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#exec-java.lang.String:A-java.lang.String:A-java.io.File-) can be found [here](https://github.com/horrorho/InflatableDonkey/blob/master/src/main/java/com/github/horrorho/inflatabledonkey/util/LZFSEExtInputStream.java). This decompressor should be used as a last resort. It may contain bugs (evil ones at that).

## How do I use it?

```
//  in = LZFSE compressed input
// out = decompressed output
```

```Java
    static void decode(Path in, Path out) {
        try (InputStream is = new BufferedInputStream(Files.newInputStream(in));
                OutputStream os = new BufferedOutputStream(Files.newOutputStream(out))) {
            new LZFSEDecoder().decode(is, os);

        } catch (IOException ex) {
            System.out.println("IO error: " + ex.getMessage());
        } catch (LZFSEDecoderException ex) {
            System.out.println("Decoder error: " + ex.getMessage());
        }
    }
```


## What's with the name?

I'm not quite sure... It seemed like a good idea at the time.


## TODO
- Unit tests
- InputStream wrapper


## Links
[LZFSE](https://github.com/lzfse/lzfse) - reference implementation.

[Asymmetric_Numeral_Systems](https://en.wikipedia.org/wiki/Asymmetric_Numeral_Systems) - bed time reading.

[Finite State Entropy - A new breed of entropy coder](http://fastcompression.blogspot.co.uk/2013/12/finite-state-entropy-new-breed-of.html) - wonderful series of articles on Finite State Entropy.
