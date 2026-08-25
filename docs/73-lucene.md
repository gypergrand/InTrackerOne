# 73 — Apache Lucene

## What it is

Apache Lucene is a high-performance, full-featured **text search engine library** written in Java. It is not a server and has no network layer — it is an embeddable Java library that you call directly from your code to index and search text.

> **Acronyms / terms**
> - **IR** — Information Retrieval, the field of finding relevant documents.
> - **Inverted index** — the core data structure mapping terms → documents.
> - **BM25** — Best Matching 25, the probabilistic ranking function Lucene uses by default.
> - **NRT** — Near Real Time, the ability to search documents seconds after they are indexed.
> - **Analyzer** — the pipeline that turns text into indexable/searchable tokens.

Lucene is the foundation that **Elasticsearch** (ch. 74) and **Apache Solr** (ch. 75) are built on top of. Both are servers; Lucene is the engine inside them.

## Lucene vs Elasticsearch vs Solr

| Aspect | Lucene | Elasticsearch | Solr |
| --- | --- | --- | --- |
| Form factor | Java **library** | Search **server** | Search **server** |
| Network / REST | None (in-process API) | HTTP REST + JSON | HTTP + XML/JSON config |
| Distribution | You build it yourself | Built-in (shards, replicas) | SolrCloud (ZooKeeper) |
| Schema | Fields defined in code | Dynamic mapping (schema-less) | `schema.xml` / managed-schema |
| Operational cost | Lowest (embedded) | Highest (cluster) | Medium (server) |
| Best when | You need full control, embed search, or tiny footprint | You need a distributed, scalable search cluster | You want feature-rich enterprise search with config |

Rule of thumb: pick **Lucene** when you want the engine and nothing else; pick **Elasticsearch** or **Solr** when you want a ready-made, distributed, operational search server.

## How it works

### Inverted index
Lucene stores an *inverted* index: for each term it keeps a posting list of the documents (and positions) that contain it. Searching a term is O(1)-ish lookup instead of scanning every document.

### Segments
An index is split into **segments** — immutable, append-only mini-indexes. `IndexWriter` writes new segments; deletes/updates are recorded in separate delete files and applied at merge time. A background **MergePolicy** merges small segments into larger ones to keep search fast and disk tidy. Readers see a consistent point-in-time view of committed segments.

### Analyzer = CharFilter → Tokenizer → TokenFilter
Text is normalized before indexing and before querying via an `Analyzer`:
- **CharFilter** — preprocess characters (e.g. HTML strip).
- **Tokenizer** — split text into tokens (e.g. `StandardTokenizer`).
- **TokenFilter** — lowercase, stem, remove stopwords, synonyms (`LowerCaseFilter`, `SnowballFilter`, `StopFilter`).

The *same* analyzer must be used at index and query time so tokens match.

### Scoring (BM25)
Since Lucene 6, the default similarity is **BM25**. For a term `t` in document `d`:

```
score(d, t) = IDF(t) * (f(t,d) * (k1 + 1)) / (f(t,d) + k1 * (1 - b + b * |d|/avgdl))
```

- `f(t,d)` — term frequency in the doc
- `|d|` — document length, `avgdl` — average length
- `k1` (≈1.2) controls term-frequency saturation, `b` (≈0.75) controls length normalization
- `IDF(t) = ln(1 + (N - n + 0.5)/(n + 0.5))`

BM25 bounds how much repeated terms help (unlike raw TF-IDF) and penalizes long documents.

## Usage

### Maven dependency
```xml
<dependency>
  <groupId>org.apache.lucene</groupId>
  <artifactId>lucene-core</artifactId>
  <version>9.11.1</version>
</dependency>
<dependency>
  <groupId>org.apache.lucene</groupId>
  <artifactId>lucene-queryparser</artifactId>
  <version>9.11.1</version>
</dependency>
<dependency>
  <groupId>org.apache.lucene</groupId>
  <artifactId>lucene-analysis-common</artifactId>
  <version>9.11.1</version>
</dependency>
```

### Minimal index + search example
```java
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.util.List;

public class LuceneDemo {

    public static void main(String[] args) throws Exception {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Directory dir = new RAMDirectory();           // or FSDirectory.open(Path)

        // 1) Indexing
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(dir, cfg)) {
            List.of(
                new Doc("1", "Apache Lucene full text search library", "java"),
                new Doc("2", "Elasticsearch is built on Lucene", "search"),
                new Doc("3", "Spring Boot REST API in Java", "java")
            ).forEach(d -> {
                Document doc = new Document();
                doc.add(new StringField("id", d.id, Field.Store.YES));
                doc.add(new TextField("title", d.title, Field.Store.YES));
                doc.add(new StringField("tag", d.tag, Field.Store.YES));
                try { writer.addDocument(doc); } catch (Exception e) { throw new RuntimeException(e); }
            });
        } // close => commit

        // 2) Searching
        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("title", analyzer);
            Query query = parser.parse("lucene");     // term query

            TopDocs hits = searcher.search(query, 10);
            System.out.println("Total hits: " + hits.totalHits.value());
            for (ScoreDoc sd : hits.scoreDocs) {
                Document d = searcher.storedFields().document(sd.doc);
                System.out.printf("%.4f  %s  %s%n",
                        sd.score, d.get("id"), d.get("title"));
            }

            // Boolean query: title contains "java" AND tag == "search"
            Query q = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("title", "java")), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("tag", "search")), BooleanClause.Occur.MUST)
                .build();
            System.out.println("Boolean hits: " + searcher.search(q, 10).totalHits.value());
        }
    }

    record Doc(String id, String title, String tag) {}
}
```

Key classes: `IndexWriter` (writes/commits segments), `DirectoryReader`/`IndexSearcher` (reads), `Document`/`Field` (`TextField` analyzed, `StringField` exact), `QueryParser`, and `StandardAnalyzer`.

## Pros and cons

**Pros**
- Blazing fast, battle-tested, used by ES/Solr/Druid.
- Tiny footprint, embeddable, no separate process or JVM to operate.
- Full control over indexing, scoring (`Similarity`), codecs, and analyzers.
- Mature, stable API and huge ecosystem of analyzers.

**Cons**
- No server, no REST, no distribution — you build clustering, HA, and APIs yourself.
- Steep learning curve for analyzers, codecs, and segment/merge tuning.
- You own operational concerns: backups, replication, monitoring.
- Not a document store — pair it with a primary DB for source-of-truth data.

## Real project scenarios

- **Embedded search in a desktop / CLI tool** — bundle Lucene in a single JVM, index local files, search offline with zero infrastructure.
- **Custom ranking engine** — implement a bespoke `Similarity` or query-time boosting for domain-specific relevance (legal, medical, e-commerce).
- **Full-text inside a primary datastore** — index PostgreSQL rows into Lucene on write to offer fast `LIKE`/phrase search without a separate search cluster.
- **Search for an in-memory cache** — build a RAMDirectory-backed index over a hot dataset for sub-millisecond lookups.
- **Building your own search server** — start from Lucene when you need sharding/replication semantics that differ from ES/Solr.
- **Analyzer R&D** — prototype language-specific tokenization/stemming (CJK, Arabic) before committing to a server product.
- **Log/classifier indexing** — index structured events by tokenized message text for quick pattern retrieval in a backend service.
- **Testing/CI** — unit-test query logic against an in-process Lucene index instead of spinning up Elasticsearch.

## FAQ (interview)

**Q1. Lucene is a library, not a server — what does that mean in practice?**
You call its Java API in your own process; there is no HTTP endpoint, no cluster, no daemon. You are responsible for concurrency, distribution, and exposing any API.

**Q2. What is the difference between `TextField` and `StringField`?**
`TextField` is analyzed (tokenized, lowercased) and is what you search *within*. `StringField` is indexed as a single exact token (not analyzed) — use it for IDs, enums, tags you match exactly.

**Q3. Why are segments immutable and why does Lucene merge them?**
Immutable segments let readers open a snapshot safely and avoid locking on writes. Merging reclaims deleted documents and reduces the number of segments, which keeps search fast (fewer files to open) and reclaims disk.

**Q4. What is NRT (Near Real Time) search?**
After `IndexWriter.commit()` or `openIfChanged`, a newly opened `DirectoryReader` sees recently added documents within ~1s without a full flush, enabling "search what you just indexed" with low latency.

**Q5. Why must the same analyzer be used at index and query time?**
Tokens must match byte-for-byte. If you index "Lucene" lowercased but query with the raw case, or vice-versa, the terms won't match and you get zero hits.

**Q6. BM25 vs classic TF-IDF — what changed?**
BM25 caps the benefit of term repetition (saturation via `k1`) and normalizes for document length (`b`), reducing the advantage of long, keyword-stuffed documents compared to unbounded TF-IDF.

## Cheat sheet

```
Core classes      IndexWriter, IndexReader/DirectoryReader, IndexSearcher, Document, Field
Analyzer          CharFilter → Tokenizer → TokenFilter (same at index + query time)
Fields            TextField = analyzed; StringField = exact; IntPoint/StoredField = numeric
Index structure   immutable segments + delete files + background MergePolicy
Scoring           BM25 (default), Similarity is pluggable
Search            QueryParser, TermQuery, BooleanQuery, PhraseQuery, RangeQuery
NRT               DirectoryReader.openIfChanged(reader) to see fresh docs
Storage           Directory: RAMDirectory (test), FSDirectory (disk)
Persistence       writer.commit() / close() to flush segments to disk
```

> **See also**
> - [74 — Elasticsearch](./74-elasticsearch.md) — distributed search server built on Lucene.
> - [75 — Apache Solr](./75-solr.md) — enterprise search server built on Lucene.
> - [68 — ELK Stack](./68-elk-stack.md) — log/analytics stack centered on Elasticsearch.
