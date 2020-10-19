package com.quakbo.presentation.database.v1

import org.jdbi.v3.sqlobject.statement.SqlQuery

data class Address(val streetAddress: String, val city: String, val state: String, val zipCode: String, val id: Long? = null)

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