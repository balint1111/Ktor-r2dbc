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
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.limit
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Routing.configureUserRoutes(database: Database) {
    get("/") {
        call.respondText("Ktor R2DBC with H2 In-Memory Database")
    }

    get("/users") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
        if (page < 1 || size < 1) {
            call.respond(
                io.ktor.http.HttpStatusCode.BadRequest,
                ErrorResponse(error = "page and size must be positive integers")
            )
            return@get
        }

        val offset = (page - 1L) * size
        val (users, total) = newSuspendedTransaction(db = database) {
            val totalCount = Users.selectAll().count()
            val pageUsers = Users.selectAll()
                .limit(size, offset)
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
                items = users,
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

        val user = newSuspendedTransaction(db = database) {
            Users.select { Users.id eq id }
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

        val newUserId = newSuspendedTransaction(db = database) {
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

        val rowsDeleted = newSuspendedTransaction(db = database) {
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
