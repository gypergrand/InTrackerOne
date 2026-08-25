# 73 — Apache Lucene

## Что это такое

Apache Lucene — это высокопроизводительная, полнофункциональная **библиотека полнотекстового поиска**, написанная на Java. Это не сервер и в ней нет сетевого слоя — это встраиваемая Java-библиотека, к API которой вы обращаетесь напрямую из своего кода, чтобы индексировать и искать текст.

> **Аббревиатуры / термины**
> - **IR** — Information Retrieval, область поиска релевантных документов.
> - **Инвертированный индекс** — ключевая структура данных: термин → документы.
> - **BM25** — Best Matching 25, вероятностная функция ранжирования, используемая в Lucene по умолчанию.
> - **NRT** — Near Real Time, возможность искать документы через секунды после индексации.
> - **Analyzer** — конвейер, превращающий текст в индексируемые/искомые токены.

Lucene — это фундамент, на котором построены **Elasticsearch** (гл. 74) и **Apache Solr** (гл. 75). Обе они — серверы; Lucene — движок внутри них.

## Lucene vs Elasticsearch vs Solr

| Аспект | Lucene | Elasticsearch | Solr |
| --- | --- | --- | --- |
| Форма | Java-**библиотека** | Поисковый **сервер** | Поисковый **сервер** |
| Сеть / REST | Нет (in-process API) | HTTP REST + JSON | HTTP + XML/JSON конфиг |
| Распределённость | Строите сами | Встроенная (шарды, реплики) | SolrCloud (ZooKeeper) |
| Схема | Поля задаются в коде | Динамическое отображение (schema-less) | `schema.xml` / managed-schema |
| Операционные затраты | Минимальные (встроен) | Максимальные (кластер) | Средние (сервер) |
| Когда лучше | Нужен полный контроль, встраивание, малый след | Нужен распределённый масштабируемый кластер | Нужен насыщенный enterprise-поиск с конфигом |

Правило: выбирайте **Lucene**, когда нужен только движок и ничего лишнего; выбирайте **Elasticsearch** или **Solr**, когда нужен готовый распределённый работающий поисковый сервер.

## Как устроен

### Инвертированный индекс
Lucene хранит *инвертированный* индекс: для каждого термина ведётся список постингов — документов (и позиций), содержащих его. Поиск по термину — это быстрый lookup вместо перебора всех документов.

### Сегменты
Индекс разбит на **сегменты** — неизменяемые, дописываемые mini-индексы. `IndexWriter` пишет новые сегменты; удаления/обновления записываются в отдельные файлы delete и применяются при слиянии. Фоновая **MergePolicy** сливает мелкие сегменты в крупные, чтобы поиск оставался быстрым, а диск — чистым. Читатели видят согласованный снимок зафиксированных сегментов.

### Analyzer = CharFilter → Tokenizer → TokenFilter
Текст нормализуется перед индексацией и перед запросом через `Analyzer`:
- **CharFilter** — предобработка символов (например, удаление HTML).
- **Tokenizer** — разбиение текста на токены (например, `StandardTokenizer`).
- **TokenFilter** — lower-case, стемминг, стоп-слова, синонимы (`LowerCaseFilter`, `SnowballFilter`, `StopFilter`).

*Один и тот же* анализатор должен использоваться и при индексации, и при запросе, чтобы токены совпадали.

### Ранжирование (BM25)
Начиная с Lucene 6, similarity по умолчанию — **BM25**. Для термина `t` в документе `d`:

```
score(d, t) = IDF(t) * (f(t,d) * (k1 + 1)) / (f(t,d) + k1 * (1 - b + b * |d|/avgdl))
```

- `f(t,d)` — частота термина в документе
- `|d|` — длина документа, `avgdl` — средняя длина
- `k1` (≈1.2) задаёт насыщение частоты, `b` (≈0.75) — нормализацию длины
- `IDF(t) = ln(1 + (N - n + 0.5)/(n + 0.5))`

BM25 ограничивает вклад повторов термина (в отличие от чистого TF-IDF) и штрафует длинные документы.

## Применение

### Maven-зависимость
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

### Минимальный пример индексации + поиска
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
        Directory dir = new RAMDirectory();           // или FSDirectory.open(Path)

        // 1) Индексация
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

        // 2) Поиск
        try (DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("title", analyzer);
            Query query = parser.parse("lucene");     // терминовый запрос

            TopDocs hits = searcher.search(query, 10);
            System.out.println("Total hits: " + hits.totalHits.value());
            for (ScoreDoc sd : hits.scoreDocs) {
                Document d = searcher.storedFields().document(sd.doc);
                System.out.printf("%.4f  %s  %s%n",
                        sd.score, d.get("id"), d.get("title"));
            }

            // Булев запрос: title содержит "java" И tag == "search"
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

Ключевые классы: `IndexWriter` (пишет/коммитит сегменты), `DirectoryReader`/`IndexSearcher` (читает), `Document`/`Field` (`TextField` анализируется, `StringField` — точное), `QueryParser` и `StandardAnalyzer`.

## Плюсы и минусы

**Плюсы**
- Очень быстрый, проверенный временем, используется в ES/Solr/Druid.
- Малый след, встраиваемый, без отдельного процесса или JVM для эксплуатации.
- Полный контроль над индексацией, ранжированием (`Similarity`), кодеками и анализаторами.
- Зрелый стабильный API и огромная экосистема анализаторов.

**Минусы**
- Нет сервера, REST, распределённости — кластеризацию, HA и API строите сами.
- Крутой порог входа в анализаторы, кодеки и тюнинг сегментов/слияний.
- Сами отвечаете за бэкапы, репликацию, мониторинг.
- Не является хранилищем документов — для источника правды нужна основная БД.

## Реальные проектные сценарии

- **Встраиваемый поиск в desktop / CLI-инструмент** — подключаем Lucene в один JVM, индексируем локальные файлы, ищем офлайн без инфраструктуры.
- **Кастомный движок ранжирования** — реализуем собственный `Similarity` или бустинг на запрос для предметной релевантности (юр., мед., e-commerce).
- **Полнотекст внутри основного хранилища** — индексируем строки PostgreSQL в Lucene при записи, чтобы дать быстрый поиск `LIKE`/фраз без отдельного кластера.
- **Поиск по in-memory кэшу** — строим индекс на RAMDirectory поверх горячего набора данных для поиска за субмиллисекунды.
- **Свой поисковый сервер** — начинаем с Lucene, когда нужны шардинг/репликация, отличающиеся от ES/Solr.
- **R&D анализаторов** — прототипируем языковую токенизацию/стемминг (CJK, арабский) до выбора серверного продукта.
- **Индексация логов/событий** — индексируем структурированные события по токенизированному тексту сообщения для быстрого поиска паттернов в бэкенде.
- **Тесты/CI** — юнит-тесты логики запросов против in-process индекса Lucene вместо поднятия Elasticsearch.

## FAQ (интервью)

**В1. Lucene — библиотека, а не сервер. Что это значит на практике?**
Вы вызываете её Java API в своём процессе; нет HTTP-эндпоинта, кластера или демона. Вы отвечаете за конкурентность, распределённость и предоставление любого API.

**В2. В чём разница между `TextField` и `StringField`?**
`TextField` анализируется (токенизируется, lower-case) — по нему ищут *внутри*. `StringField` индексируется как один точный токен (без анализа) — используйте для ID, enum, тегов, которые match точно.

**В3. Почему сегменты неизменяемы и почему Lucene их сливает?**
Неизменяемые сегменты позволяют читателям открывать снимок безопасно и избегать блокировок при записи. Слияние освобождает удалённые документы и уменьшает число сегментов, что ускоряет поиск (меньше файлов) и возвращает диск.

**В4. Что такое NRT (Near Real Time) поиск?**
После `IndexWriter.commit()` или `openIfChanged` вновь открытый `DirectoryReader` видит недавно добавленные документы за ~1с без полного flush, обеспечивая поиск «только что проиндексированного» с низкой задержкой.

**В5. Почему один и тот же анализатор нужен и при индексации, и при запросе?**
Токены должны совпадать побайтово. Если проиндексировать "Lucene" в lower-case, а запрашивать в оригинале (или наоборот), термины не совпадут и выдача будет пустой.

**В6. BM25 против классического TF-IDF — что изменилось?**
BM25 ограничивает вклад повторов термина (насыщение через `k1`) и нормализует длину документа (`b`), снижая преимущество длинных, «набитых ключевыми словами» документов по сравнению с неограниченным TF-IDF.

## Cheat sheet

```
Ключевые классы   IndexWriter, IndexReader/DirectoryReader, IndexSearcher, Document, Field
Analyzer          CharFilter → Tokenizer → TokenFilter (один и тот же при индексации и запросе)
Поля              TextField = анализируется; StringField = точно; IntPoint/StoredField = числа
Структура индекса неизменяемые сегменты + delete-файлы + фоновая MergePolicy
Ранжирование      BM25 (по умолчанию), Similarity заменяется
Поиск             QueryParser, TermQuery, BooleanQuery, PhraseQuery, RangeQuery
NRT               DirectoryReader.openIfChanged(reader) чтобы увидеть свежие доки
Хранилище         Directory: RAMDirectory (тесты), FSDirectory (диск)
Сохранение        writer.commit() / close() сбрасывает сегменты на диск
```

> **См. также**
> - [74 — Elasticsearch](./74-elasticsearch.md) — распределённый поисковый сервер на базе Lucene.
> - [75 — Apache Solr](./75-solr.md) — enterprise-поисковый сервер на базе Lucene.
> - [68 — ELK Stack](./68-elk-stack.md) — стек логов/аналитики на базе Elasticsearch.
