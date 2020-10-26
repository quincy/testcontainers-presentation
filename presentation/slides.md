---
title: 'Unit Testing Interactions With External Services'
theme: league
verticalSeparator: "^\n\n"
---
## Unit Testing Interactions With External Services
### Testcontainers to the rescue!
#### Quincy Bowers
#### AppDetex
<!--
### Abstract
How can you verify that you’re integrating with an external service like SQL,
Elasticsearch, or Webdriver before deploying any code or setting up any
infrastructure? In this talk we’ll explore the Testcontainers library and level
up your testing using ephemeral Docker containers.

Ever wanted to verify that an Elasticsearch DSL query returns the kind of
documents you think it will, without having to deploy your code into the Test
environment? How about building a user focused regression test system using
Webdriver, without ever having to deploy code or maintain test environment or
Selenium cluster?

The Testcontainers project helps you launch Docker containers for use in your
tests. Expensive integration tests just got faster, cheaper, and a whole lot
more reliable. In this talk we’ll explore real life testing scenarios and how
Testcontainers can improve your life and make you a testing hero.
-->


## Agenda
* What is it?
* Why test external service interactions?
* Areas of testing which benefit
* Setup
* Examples
  + Database schema migration
  + Verifying Elasticsearch queries
  + Webdriver Testing
* Conclusion


## What is it?


Testcontainers is a Java library which helps you run Docker containers in your test suite


These containers can be used to stand in for any external service you need to interact with


The library can manage the lifecycle of the containers for you so you only need to focus on adding value with your tests


## Why test external service interactions?


You need to have some kind of integration testing


You don't want to wait until you deploy your application to find problems


Once you know your application is interacting with the external services correctly,
how do you ensure that it continues to do so?


Unit tests can guard your code from regressions that can happen when you...
* Upgrade to a new version
* Update the database schema
* Decide to use a library from a different vendor


## Areas of testing which benefit


### Data access layer test


No complex setup of environment on developer machines or CI server


You can test schema migration using liquibase, flyway, etc.


Verify complex queries or stored procedures work as expected


Ensure your application continues to work as you migrate from MySQL to Postgres


### Application integration tests


Test environment is short-lived


Reliably available only when you need it


### UI/Acceptance testing


No need to manage an always-on Selenium cluster


Each test runs in a fresh browser instance with no previous state


Test failures give you a video recording of the failure


### Not a Silver Hammer


Running a docker container is far more expensive than a unit test


A mock of the DAO or client layer will suffice in 99% of your tests


## Setup


### Docker


### JVM Testing Framework
* JUnit 4
* JUnit 5
* Spock
* Full manual


### Continuous Integration
You will need to setup docker on your build server


### Dependencies
```xml
<dependencies>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>${test-containers.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>${test-containers.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>${test-containers.version}</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```


## Examples


### Database schema migration


Database schema changes over time


When we change the schema, we need to be sure it is backwards compatible


We'd also like to be able to catch a bad change before trying to run it in a real environment


With a Testcontainer, we can run the real migration, against real data, to verify it works as expected


### Testing Strategy

1. Create MySQL container
2. Apply schema
3. Insert test data
4. Execute the production code we want to test
5. Verify results


### Domain Objects


#### Address
```kotlin
data class Address(
    val streetAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val id: Long? = null
)

interface AddressDaoV1 {
    @SqlQuery(
        """SELECT id, streetAddress, city, state, zipCode
           FROM Address
           WHERE id = :id"""
    )
    fun findById(id: Long): Address?

    @SqlQuery(
        """SELECT id, streetAddress, city, state, zipCode
           FROM Address
           WHERE city = :city"""
    )
    fun findByCity(city: String): Set<Address>
}
```
<!-- .element: style="font-size: 0.45em" -->


#### Person
```kotlin
data class Person(
    val firstName: String,
    val lastName: String,
    val id: Long? = null,
    val address: Address? = null
)

interface PersonDaoV1 {
    @SqlQuery(
        """SELECT
               p.id, p.firstName, p.lastName
             , a.id as addressId, a.streetAddress, a.city, a.state, a.zipCode
           FROM Person as p
           LEFT OUTER JOIN Address as a on a.id = p.addressId
           WHERE p.id = :id"""
    )
    @RegisterRowMapper(PersonMapperV1::class)
    fun findById(id: Long): Person?
}
```
<!-- .element: style="font-size: 0.45em" -->


#### PersonMapper
```kotlin
class PersonMapperV1 : RowMapper<Person> {
    override fun map(rs: ResultSet, ctx: StatementContext): Person {
        val address = rs.takeUnless { it.getString("streetAddress") == null }
            ?.let {
                Address(
                    id = it.getLong("addressId"),
                    streetAddress = it.getString("streetAddress"),
                    city = it.getString("city"),
                    state = it.getString("state"),
                    zipCode = it.getString("zipCode")
                )
            }
        return Person(
            id = rs.getLong("id"),
            firstName = rs.getString("firstName"),
            lastName = rs.getString("lastName"),
            address = address
        )
    }
}
```
<!-- .element: style="font-size: 0.45em" -->


We want to verify that the JOIN in the query, as well as the PersonMapper logic is correct


#### A base class for our tests that use a MySQL container
```kotlin
internal class KMySQLContainer(imageName: String)
    : MySQLContainer<KMySQLContainer>(imageName)

internal abstract class AbstractMySQLContainerTestBaseV1 {
    companion object {
        private val mySqlContainer: MySQLContainer<KMySQLContainer> =
            KMySQLContainer("mysql:8.0.19")

        private const val presentationUser = "presentation_user"
        private const val presentationPassword = "password"
        private const val liquibaseUser = "presentation_lbuser"
        private const val liquibasePassword = "lbpassword"

        init {
            mySqlContainer.also {
                it.withDatabaseName("presentationdb")
                it.withUsername(presentationUser)
                it.withPassword(presentationPassword)
                it.start()
                it.waitingFor(Wait.forListeningPort())
            }
            
            runSchemaMigration()
        }

        private fun runSchemaMigration() {
            val connection = mySqlContainer
                .createConnection("?username=$liquibaseUser&password=$liquibasePassword")

            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(connection))

            val changelog = "db/changelog/db.changelog-master.yaml"

            Liquibase(changelog, ClassLoaderResourceAccessor(), database)
                .update("")
        }

        val allTables = listOf("Person", "Address")

        val datasource: DataSource = MysqlDataSource().apply {
            this.user = presentationUser
            this.password = presentationPassword
            this.setUrl(mySqlContainer.jdbcUrl)
        }
    }
}
```
<!-- .element: style="font-size: 0.45em" -->


#### Testing PersonDao
```kotlin
internal class PersonDaoV1Test : AbstractMySQLContainerTestBaseV1() {
    companion object {
        private fun createDao(): PersonDaoV1 = Jdbi.create(datasource)
            .installPlugin(KotlinPlugin())
            .installPlugin(KotlinSqlObjectPlugin())
            .onDemand(PersonDaoV1::class.java)

        private val dbSetupTracker: DbSetupTracker = DbSetupTracker()
    }

    @BeforeEach
    fun prepareDatabase() {
        dbSetup(to = datasource) {

            deleteAllFrom(allTables)

            insertInto("Address") {
                mappedValues(
                    "id" to 1L,
                    "streetAddress" to "10880 Malibu Point",
                    "city" to "Malibu",
                    "state" to "CA",
                    "zipCode" to "90265"
                )
            }

            insertInto("Person") {
                mappedValues(
                    "id" to 1L,
                    "addressId" to 1L,
                    "firstName" to "Tony",
                    "lastName" to "Stark"
                )
                mappedValues(
                    "id" to 2L,
                    "addressId" to null,
                    "firstName" to "Bruce",
                    "lastName" to "Banner"
                )
            }
        }.launchWith(dbSetupTracker)
    }

    private val dao = createDao()

    @Test
    internal fun `find person by id loads their address too`() {
        dbSetupTracker.skipNextLaunch()

        val person = dao.findById(1L)
        assertThat(
            person,
            present(
                equalTo(
                    Person(
                        id = 1L,
                        firstName = "Tony",
                        lastName = "Stark",
                        address = Address(
                            id = 1L,
                            streetAddress = "10880 Malibu Point",
                            city = "Malibu",
                            state = "CA",
                            zipCode = "90265"
                        )
                    )
                )
            )
        )
    }
}
```
<!-- .element: style="font-size: 0.45em" -->


#### Let's Test a Schema Change
```sql
ALTER TABLE Person
    ADD fullName VARCHAR(14)
        GENERATED ALWAYS AS (CONCAT(firstName, ' ', lastName)) NOT NULL;
```
<!-- .element: style="font-size: 0.45em" -->

```kotlin
data class Person(
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val id: Long? = null,
    val address: Address? = null
)
```
<!-- .element: style="font-size: 0.45em" -->


#### Data Truncation Error
```kotlin
internal class PersonDaoV2Test : AbstractMySQLContainerTestBaseV2() {
    @Test
    internal fun `this test will cause a DataTruncationException to be thrown`() {
        assertThrows<DbSetupRuntimeException> {
            dbSetup(to = datasource) {

                deleteAllFrom(allTables)

                insertInto("Person") {
                    mappedValues(
                        "id" to 1L,
                        "addressId" to null,
                        "firstName" to "Natasha",
                        "lastName" to "Romanov"
                    )
                }
            }.launch()
        }
    }
}
```
<!-- .element: style="font-size: 0.45em" -->


This sort of testing relies on you having some good test data to insert into your database


### Verifying Elasticsearch Queries


Elasticsearch queries are long and complicated


The way you write the query can effect the results


The way you create your index mappings can effect the results


With Testcontainers we can verify that our production code works with a minimum of fuss


### Testing Strategy

1. Start Elasticsearch container
2. Upload index templates
3. Create an index and index some documents
4. Execute production code to run our query
5. Verify the results


```kotlin
abstract class ElasticsearchContainerTest {
    companion object {
        private const val ELASTICSEARCH_TEMPLATES_DIR = "/elasticsearch/templates"
        private val container = ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch-oss:7.9.2"
        )

        lateinit var client: RestHighLevelClient

        @JvmStatic
        @BeforeAll
        internal fun setUp() {
            container.start()  // 1. Start container
            container.waitingFor(Wait.forListeningPort())
            client = RestHighLevelClient(RestClient.builder(
                HttpHost(container.host, container.firstMappedPort))
            )

            createTemplates() // 2. Upload templates
        }

        @JvmStatic
        @AfterAll
        internal fun tearDown() {
            client.close()
            container.stop()
        }

        /** 
         * Reads all `*.json` files in the [ELASTICSEARCH_TEMPLATES_DIR] and uploads them
         * to the Elasticsearch container using the templates API.
         */
        private fun createTemplates() {
            val templatesDir =
                File(this::class.java.getResource(ELASTICSEARCH_TEMPLATES_DIR).file)
            templatesDir.walk().maxDepth(1)
                .filter { it.name.endsWith(".json") }
                .map { file -> file.name.removeSuffix(".json") to file.readText() }
                .map { (name, content) ->
                    PutIndexTemplateRequest(name)
                        .apply { source(content, XContentType.JSON) }
                } .forEach { request ->
                    client.indices()
                        .putTemplate(request, RequestOptions.DEFAULT)
                }
        }

        /**
         * Creates an index using the given [template] reachable through the
         * returned alias.
         */
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

            // Give ES a second to finish indexing
            Thread.sleep(Duration.ofSeconds(1).toMillis())
        }
    }
}
```
<!-- .element: style="font-size: 0.45em" -->


#### Avengers Assemble

```kotlin
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
}
```
<!-- .element: style="font-size: 0.45em" -->


```kotlin
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
            equalTo(listOf("Peter Parker"))
        )
    }
```
<!-- .element: style="font-size: 0.45em" -->


### Webdriver Testing

Finally, let's do some black box UI testing with Wikipedia


### Test Strategy
* Start container
* Using Webdriver
  + We're going to search for 'Rick Astley'
  + Then find a link with the text 'rickrolling'
  + And from that linked page we'll verify that we find the word 'meme'

[Adapted from a blog post by Richard North](https://rnorth.org/better-junit-selenium-testing-with-docker-and-testcontainers)
<!-- .element: style="font-size: 0.45em" -->


#### Firefox Container Test
```kotlin
class KBrowserWebDriverContainer(imageName: String)
    : BrowserWebDriverContainer<KBrowserWebDriverContainer>(imageName)

private const val recordingDir = "/home/.../target/webdriver-recordings"

@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class FirefoxContainerTest {

    @Container
    val container = KBrowserWebDriverContainer(
        "selenium/standalone-firefox-debug:3.141.59"
    ).withCapabilities(FirefoxOptions())
        .waitingFor(Wait.forListeningPort())
        .withRecordingMode(RECORD_ALL, File(recordingDir))
        .withRecordingFileFactory(
            customRecordingFileFactory(
                "Wikipedia-Rick-Astley-page-mentions-rickrolling", "firefox"
        )
    )

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            with(File(recordingDir)) {
                if (!exists()) {
                    mkdir()
                }
            }
        }
    }

    @Test
    internal fun `Wikipedia Rick Astley page mentions rickrolling`() {
        runTest(container)
    }
}

private fun runTest(container: KBrowserWebDriverContainer) {
    val driver = container.webDriver
    driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS)

    driver.get("https://www.wikipedia.org")
    driver.findElement(By.name("search")).apply {
        sendKeys("Rick Astley")
        submit()
    }

    driver.findElement(By.linkText("rickrolling")).click()
    val foundExpectedText = driver.findElements(By.cssSelector("p"))
        .map { it.text }
        .any { "meme" in it }

    assertThat(foundExpectedText, equalTo(true))
}
```
<!-- .element: style="font-size: 0.45em" -->


# Show the video


## Conclusion


Some interactions with external services have been historically difficult to test


Testcontainers gives us a relatively lightweight and reliable method of testing these interactions


With a little bit of effort to get your container initialized for your application, the actual tests become fairly simple to write


# Questions?

Slides and code available at

https://github.com/quincy/testcontainers-presentation

https://www.testcontainers.org/
