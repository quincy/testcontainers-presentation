package com.quakbo.presentation.database.v3

import com.quakbo.presentation.database.v1.Address
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import java.sql.ResultSet

data class Person(val firstName: String, val lastName: String, val fullName: String, val id: Long? = null, val address: Address? = null)

interface PersonDaoV3 {
    @SqlQuery(
        """SELECT p.id
                , p.firstName
                , p.lastName
                , p.fullName
                , a.id as addressId
                , a.streetAddress
                , a.city
                , a.state
                , a.zipCode
           FROM Person as p
           LEFT OUTER JOIN Address as a on a.id = p.addressId
           WHERE p.id = :id"""
    )
    @RegisterRowMapper(PersonMapperV2::class)
    fun findById(id: Long): Person?
}

class PersonMapperV2 : RowMapper<Person> {
    override fun map(rs: ResultSet, ctx: StatementContext): Person {
        val address = rs.takeUnless { it.getString("streetAddress") == null }?.let {
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
            fullName = rs.getString("fullName"),
            address = address
        )
    }
}
