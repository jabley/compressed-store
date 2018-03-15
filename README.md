A Store implementation which compresses the serialised version of the session.

See [the Tomcat documentation](https://tomcat.apache.org/tomcat-8.5-doc/config/manager.html).

This is the same as the JDBCStore, except it will store the serialised form of the session using [zstd][zstd].

For some applications, the amount of data stored in the session is large.

It might be impractical/uneconomic to rearchitect the application to reduce the amount of data stored in the session.

This library provides a way of reducing the amount of data persisted in the database, and thus offers a trade-off of
additional CPU to compress and decompress the session, versus much smaller network traffic and data being handled by the
database.

```xml
<Store
  className="com.eternus.tomcat.session.CompressingJDBCStore"
  compressionLevel="4" />
```

It supports the same configuration options as the JDBCStore, plus one extra thing.

We can tweak the ZStandard compression level using the `compressionLevel` attribute.

[zstd]: https://code.facebook.com/posts/1658392934479273/smaller-and-faster-data-compression-with-zstandard/