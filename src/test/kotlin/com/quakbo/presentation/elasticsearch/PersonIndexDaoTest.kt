package com.quakbo.presentation.elasticsearch

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.quakbo.presentation.elasticsearch.FilterOp.NUMBER_IN_RANGE
import com.quakbo.presentation.elasticsearch.FilterOp.STRING_EQ
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.http.HttpHost
import org.elasticsearch.action.admin.indices.alias.Alias
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.PutIndexTemplateRequest
import org.elasticsearch.common.xcontent.XContentType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.elasticsearch.ElasticsearchContainer
import java.io.File
import java.time.Duration
import java.util.UUID

abstract class ElasticsearchContainerTest {
    companion object {
        private const val ELASTICSEARCH_TEMPLATES_DIR = "/elasticsearch/templates"
        private val container = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:7.9.2")

        lateinit var client: RestHighLevelClient

        @JvmStatic
        @BeforeAll
        internal fun setUp() {
            container.start()
            container.waitingFor(Wait.forListeningPort())
            client = RestHighLevelClient(RestClient.builder(HttpHost(container.host, container.firstMappedPort)))

            createTemplates()
        }

        @JvmStatic
        @AfterAll
        internal fun tearDown() {
            client.close()
            container.stop()
        }

        /** Reads all `*.json` files in the [ELASTICSEARCH_TEMPLATES_DIR] and uploads them to the Elasticsearch container using the templates API. */
        private fun createTemplates() {
            val templatesDir = File(this::class.java.getResource(ELASTICSEARCH_TEMPLATES_DIR).file)
            templatesDir.walk().maxDepth(1)
                .filter { it.name.endsWith(".json") }
                .map { file -> file.name.removeSuffix(".json") to file.readText() }
                .map { (name, content) -> PutIndexTemplateRequest(name).apply { source(content, XContentType.JSON) } }
                .forEach { request -> client.indices().putTemplate(request, RequestOptions.DEFAULT) }
        }

        /** Creates an index using the given [template] reachable through the returned alias. */
        fun createIndex(template: String): String {
            val alias = "$template-${UUID.randomUUID()}"
            client.indices()
                .create(
                    CreateIndexRequest("$template-${UUID.randomUUID()}")
                        .alias(Alias(alias)),
                    RequestOptions.DEFAULT
                )
            return alias
        }

        /** Indexes the given [docs] into the given index [alias]. */
        fun index(alias: String, docs: Collection<ElasticsearchDocument>) {
            docs.map { doc -> Json.encodeToString(doc) }
                .map { json -> IndexRequest(alias).source(json, XContentType.JSON) }
                .forEach { request -> client.index(request, RequestOptions.DEFAULT) }
            Thread.sleep(Duration.ofSeconds(1).toMillis()) // Give ES a second to finish indexing
        }
    }
}

class PersonIndexDaoTest : ElasticsearchContainerTest() {
    private val avengers = listOf(
        Person(firstName = "Bruce", lastName = "Banner", age = 49),
        Person(firstName = "Hulk", age = 13),
        Person(firstName = "Thor"),
        Person(firstName = "Clinton", lastName = "Barton", age = 47),
        Person(firstName = "James", lastName = "Barnes", age = 101),
        Person(firstName = "James", lastName = "Rhodes", age = 50),
        Person(firstName = "Maria", lastName = "Hill", age = 36),
        Person(firstName = "Natasha", lastName = "Romanov", age = 34),
        Person(firstName = "Nick", lastName = "Fury", age = 67),
        Person(firstName = "Peter", lastName = "Parker", age = 16),
        Person(firstName = "Peter", lastName = "Quill", age = 38),
        Person(firstName = "Sam", lastName = "Wilson", age = 40),
        Person(firstName = "Scott", lastName = "Lang", age = 49),
        Person(firstName = "Stephen", lastName = "Strange", age = 42),
        Person(firstName = "Steve", lastName = "Rogers", age = 100),
        Person(firstName = "T'Challa", age = 42),
        Person(firstName = "Tony", lastName = "Stark", age = 48),
    )

    @Test
    internal fun `match_all query returns all documents`() {
        val index = createIndex("person-index")
        index(index, avengers)

        val actual = PersonService(client, index)
            .query(MatchAllQuery(size = 10_000))

        assertThat(actual, hasSize(equalTo(17)))
    }

    @Test
    internal fun `filter query can find Avengers named Peter`() {
        val index = createIndex("person-index")
        index(index, avengers)

        val actual = PersonService(client, index)
            .query(FilterQuery(
                filter = Filter(
                    field = "firstName",
                    op = STRING_EQ,
                    value = "Peter")))

        assertThat(
            actual.map { it.lastName }.toSet(),
            equalTo(setOf("Parker", "Quill"))
        )
    }

    @Test
    internal fun `filter query can find Peter Parker`() {
        val index = createIndex("person-index")
        index(index, avengers)

        val actual = PersonService(client, index)
            .query(FilterQuery(
                filters = listOf(
                    Filter(field = "firstName", op = STRING_EQ, value = "Peter"),
                    Filter(field = "lastName", op = STRING_EQ, value = "Parker"),
                )))

        assertThat(
            actual.map { "${it.firstName} ${it.lastName}" },
            equalTo("Peter Parker")
        )
    }

    @Test
    internal fun `range query can find Avengers in their thirties`() {
        val index = createIndex("person-index")
        index(index, avengers)

        val actual = PersonService(client, index)
            .query(RangeQuery(
                filter = Filter(
                    field = "age",
                    op = NUMBER_IN_RANGE,
                    values = (30..39).toList())))

        assertThat(
            actual.map { it.age }.toSet(),
            equalTo(setOf(34, 36, 38))
        )
    }
}