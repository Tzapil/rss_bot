# rss-get

A Telegram bot for feeds based on tblibrary on clojure.

## Commands

- /list - list of all feeds
- /subscribe [url] - subscribe on feed
- /unsubscribe [url] - unsubscribe from feed

## Instalation
The easiest way to run the app is:

```
lein run
```

or

```
lein deps
lein compile
lein uberjar 
java -jar ./target/uberjar/rss-get-0.1.0-SNAPSHOT-standalone.jar
```

## Dependencies

- a Java JDK/JRE installation, version 8 or above
- Clojure 1.8.0
- core.async 0.2.374 (provided via a dependency)
- tools.logging 0.3.1 (provided via a dependency)
- feedparser-clj 0.4.0 (provided via a dependency)
- data.json 0.2.6 (provided via a dependency)

## License

Copyright © 2016 du.kulaevskiy

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
