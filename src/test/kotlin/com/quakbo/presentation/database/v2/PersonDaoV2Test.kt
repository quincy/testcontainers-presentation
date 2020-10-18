package com.quakbo.presentation.database.v2

import com.ninja_squad.dbsetup.DbSetupRuntimeException
import com.ninja_squad.dbsetup_kotlin.dbSetup
import com.ninja_squad.dbsetup_kotlin.mappedValues
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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