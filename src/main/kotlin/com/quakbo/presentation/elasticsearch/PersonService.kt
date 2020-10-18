package com.quakbo.presentation.elasticsearch

import com.quakbo.presentation.elasticsearch.FilterOp.NUMBER_EQ
import com.quakbo.presentation.elasticsearch.FilterOp.NUMBER_IN_RANGE
import com.quakbo.presentation.elasticsearch.FilterOp.STRING_EQ
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchAllQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermQueryBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder

@Serializable
sealed class ElasticsearchDocument

@Serializable
data class Person(val firstName: String, val lastName: String? = null, val age: Int? = null) : ElasticsearchDocument()

class PersonService(private val client: RestHighLevelClient, private val index: String) {
    fun query(query: Query): List<Person> {
        val request = SearchRequest(
            arrayOf(index),
            SearchSourceBuilder()
                .query(query.queryBuilder())
                .size(query.size)
        )
        val response = client.search(request, RequestOptions.DEFAULT)

        return response.hits.hits
            .mapNotNull(SearchHit::getSourceAsString)
            .map { Json.decodeFromString<ElasticsearchDocument>(it) as Person }
    }
}

sealed class Query(val size: Int = 10, val filters: List<Filter> = emptyList()) {
    abstract fun queryBuilder(): QueryBuilder
}

class MatchAllQuery(size: Int = 10) : Query(size = size) {
    override fun queryBuilder(): QueryBuilder = MatchAllQueryBuilder()
}

class FilterQuery(size: Int = 10, filters: List<Filter>) : Query(size, filters) {
    constructor(size: Int = 10, filter: Filter, vararg filters: Filter) : this(size, listOf(filter) + filters)

    override fun queryBuilder(): QueryBuilder {
        return BoolQueryBuilder().apply {
            filters.map { filter ->
                when (filter.op) {
                    NUMBER_EQ, STRING_EQ -> TermQueryBuilder(filter.field, filter.value)
                    else -> TODO("FilterOp.${filter.op} is not supported by FilterQuery.")
                }
            }.forEach(::must)
        }
    }
}

class RangeQuery(size: Int = 10, filters: List<Filter>) : Query(size, filters) {
    constructor(size: Int = 10, filter: Filter, vararg filters: Filter) : this(size, listOf(filter) + filters)

    override fun queryBuilder(): QueryBuilder {
        return BoolQueryBuilder().apply {
            filters.map { filter ->
                when (filter.op) {
                    NUMBER_IN_RANGE -> RangeQueryBuilder(filter.field)
                        .gte(filter.values.first())
                        .lte(filter.values.last())
                    else -> TODO("FilterOp.${filter.op} is not supported by RangeQuery.")
                }
            }.forEach(::must)
        }
    }
}

data class Filter(val field: String, val op: FilterOp, val values: List<Any>) {
    init {
        require(values.isNotEmpty())
    }

    constructor(field: String, op: FilterOp, value: Any) : this(field, op, listOf(value))

    val value: Any = values.first()
}

enum class FilterOp {
    NUMBER_EQ,
    NUMBER_IN_RANGE,
    STRING_EQ,
    ;
}

private fun <T> List<T>.last(): T = this[size - 1]