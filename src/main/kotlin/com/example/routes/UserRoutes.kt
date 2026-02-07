package com.example.routes

import com.example.model.CreateUserRequest
import com.example.model.ErrorResponse
import com.example.model.User
import com.example.model.UserResponse
import com.example.model.Users
import com.example.model.PageResponse
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

fun Routing.configureUserRoutes(database: R2dbcDatabase) {
    get("/") {
        call.respondText("Ktor R2DBC with H2 In-Memory Database")
    }

    get("/users") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
        println("page: $page, size: $size")
        if (page < 1 || size < 1) {
            call.respond(
                io.ktor.http.HttpStatusCode.BadRequest,
                ErrorResponse(error = "page and size must be positive integers")
            )
            return@get
        }

        val offset = (page - 1L) * size
        val (users, total) = suspendTransaction(db = database) {
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
            pageUsers to totalCount
        }

        call.respond(
            PageResponse(
                items = users.toList(),
                page = page,
                size = size,
                total = total
            )
        )
    }

    get("/users/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(
                io.ktor.http.HttpStatusCode.BadRequest,
                ErrorResponse(error = "Invalid ID")
            )
            return@get
        }

        val user = suspendTransaction(db = database) {
            Users.selectAll()
                .where{ Users.id eq id }
                .map { row ->
                    User(
                        id = row[Users.id].value,
                        name = row[Users.name],
                        email = row[Users.email]
                    )
                }
                .singleOrNull()
        }

        if (user != null) {
            call.respond(UserResponse(user))
        } else {
            call.respond(
                io.ktor.http.HttpStatusCode.NotFound,
                ErrorResponse(error = "User not found")
            )
        }
    }

    post("/users") {
        val request = call.receive<CreateUserRequest>()

        val newUserId = suspendTransaction(db = database) {
            Users.insertAndGetId { row ->
                row[name] = request.name
                row[email] = request.email
            }.value
        }

        call.respond(
            io.ktor.http.HttpStatusCode.Created,
            UserResponse(User(id = newUserId, name = request.name, email = request.email))
        )
    }

    delete("/users/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(
                io.ktor.http.HttpStatusCode.BadRequest,
                ErrorResponse(error = "Invalid ID")
            )
            return@delete
        }

        val rowsDeleted = suspendTransaction(db = database) {
            Users.deleteWhere { Users.id eq id }
        }

        if (rowsDeleted > 0) {
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        } else {
            call.respond(
                io.ktor.http.HttpStatusCode.NotFound,
                ErrorResponse(error = "User not found")
            )
        }
    }
}
