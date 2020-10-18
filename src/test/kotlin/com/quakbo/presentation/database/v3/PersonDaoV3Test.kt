package com.quakbo.presentation.database.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup_kotlin.dbSetup
import com.ninja_squad.dbsetup_kotlin.launchWith
import com.ninja_squad.dbsetup_kotlin.mappedValues
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonDaoV3Test : AbstractMySQLContainerTestBaseV3() {
    companion object {
        private fun createDao(): PersonDaoV3 = Jdbi.create(datasource)
            .installPlugin(KotlinPlugin())
            .installPlugin(KotlinSqlObjectPlugin())
            .onDemand(PersonDaoV3::class.java)

        private val dbSetupTracker: DbSetupTracker = DbSetupTracker()
    }

    @BeforeEach
    fun prepareDatabase() {
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
        }.launchWith(dbSetupTracker)
    }

    private val dao = createDao()

    @Test
    internal fun `persons have a default fullName`() {
        dbSetupTracker.skipNextLaunch()

        val person = dao.findById(1L)
        assertThat(
            person,
            present(
                equalTo(
                    Person(
                        id = 1L,
                        firstName = "Natasha",
                        lastName = "Romanov",
                        fullName = "Natasha Romanov"
                    )
                )
            )
        )
    }
}