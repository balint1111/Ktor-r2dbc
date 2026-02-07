package com.example.repository

import com.example.model.CreateUserRequest
import com.example.model.User
import com.example.model.Users
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class UserRepository(private val database: R2dbcDatabase) {
    suspend fun fetchUsers(page: Int, size: Int): Pair<List<User>, Long> {
        val offset = (page - 1L) * size
        return suspendTransaction(db = database) {
            val totalCount = Users.selectAll().count()
            val pageUsers = Users.selectAll()
                .limit(size)
                .offset(offset)
                .map { row ->
                    User(
                        id = row[Users.id].value,
                        name = row[Users.name],
                        email = row[Users.email]
                    )
                }
                .toList()
            pageUsers to totalCount
        }
    }

    suspend fun findUserById(id: Int): User? {
        return suspendTransaction(db = database) {
            Users.selectAll()
                .where { Users.id eq id }
                .map { row ->
                    User(
                        id = row[Users.id].value,
                        name = row[Users.name],
                        email = row[Users.email]
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun createUser(request: CreateUserRequest): User {
        val newUserId = suspendTransaction(db = database) {
            Users.insertAndGetId { row ->
                row[name] = request.name
                row[email] = request.email
            }.value
        }
        return User(id = newUserId, name = request.name, email = request.email)
    }

    suspend fun deleteUser(id: Int): Boolean {
        val rowsDeleted = suspendTransaction(db = database) {
            Users.deleteWhere { Users.id eq id }
        }
        return rowsDeleted > 0
    }
}
