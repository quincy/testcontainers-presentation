package com.quakbo.presentation.database.v1

import com.mysql.cj.jdbc.MysqlDataSource
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource


internal class KMySQLContainer(imageName: String) : MySQLContainer<KMySQLContainer>(imageName)

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
            val connection = mySqlContainer.createConnection("?username=$liquibaseUser&password=$liquibasePassword")

            val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))

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
