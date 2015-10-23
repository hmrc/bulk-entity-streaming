
bulk-entity-streaming
====
[![Build Status](https://travis-ci.org/hmrc/bulk-entity-streaming.svg?branch=master)](https://travis-ci.org/hmrc/bulk-entity-streaming) [ ![Download](https://api.bintray.com/packages/hmrc/releases/bulk-entity-streaming/images/download.svg) ](https://bintray.com/hmrc/releases/bulk-entity-streaming/_latestVersion)

Bulk Entity Streaming is a library that streams deliminator based data from a source of various types and provides an iterator or a list of converted entities.
The motivation for this library is to provide applications with a consistent way of processing large sources of data (file, http) without the risk of encountering an out of memory exception, no matter how large the source data.
The library expects a source function, a deliminator and a function to convert a 'row' (parsed based on the deliminator) of string-based data into an entity.

At the moment this library supports input sources of the following type:
* ```Enumerator[Array[Byte]]```
* ```Future[Enumerator[Array[Byte]]]```
* ```Iterator[Char]```

##Download bulk-entity-streaming
====
```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "bulk-entity-streaming" % "x.x.x"
```

##Future Enhancements
====
* N/A

##License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").