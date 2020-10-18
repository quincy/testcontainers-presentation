package com.quakbo.presentation.database.v1

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

    @Test
    internal fun `find person by id with no address loads null address`() {
        dbSetupTracker.skipNextLaunch()

        val person = dao.findById(2L)
        assertThat(
            person,
            present(
                equalTo(
                    Person(
                        id = 2L,
                        firstName = "Bruce",
                        lastName = "Banner",
                        address = null
                    )
                )
            )
        )
    }
}