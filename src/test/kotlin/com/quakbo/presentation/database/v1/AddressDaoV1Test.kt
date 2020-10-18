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

internal class AddressDaoV1Test : AbstractMySQLContainerTestBaseV1() {
    companion object {
        private fun createDao(): AddressDaoV1 = Jdbi.create(datasource)
            .installPlugin(KotlinPlugin())
            .installPlugin(KotlinSqlObjectPlugin())
            .onDemand(AddressDaoV1::class.java)

        private val dbSetupTracker: DbSetupTracker = DbSetupTracker()
    }

    @BeforeEach
    fun prepareDatabase() {
        dbSetup(to = datasource) {

            deleteAllFrom(allTables)

            insertInto("Address") {
                mappedValues(
                    "id" to 1L,
                    "streetAddress" to "123 Main St",
                    "city" to "Podunk",
                    "state" to "ID",
                    "zipCode" to "12345-6789"
                )
            }
        }.launchWith(dbSetupTracker)
    }

    private val dao = createDao()

    @Test
    fun `verify an existing address can be found by its id`() {
        dbSetupTracker.skipNextLaunch()

        val address = dao.findById(1L)
        assertThat(
            address,
            present(
                equalTo(
                    Address(
                        id = 1L,
                        streetAddress = "123 Main St",
                        city = "Podunk",
                        state = "ID",
                        zipCode = "12345-6789"
                    )
                )
            )
        )
    }

    @Test
    internal fun `verify an existing address can be found by its city`() {
        dbSetupTracker.skipNextLaunch()

        val address = dao.findByCity("Podunk")
        assertThat(
            address,
            present(
                equalTo(
                    setOf(
                        Address(
                            id = 1L,
                            streetAddress = "123 Main St",
                            city = "Podunk",
                            state = "ID",
                            zipCode = "12345-6789"
                        )
                    )
                )
            )
        )
    }
}
