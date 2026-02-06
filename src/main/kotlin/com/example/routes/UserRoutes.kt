package com.example.routes

import com.example.model.CreateUserRequest
import com.example.model.User
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.r2dbc.pool.ConnectionPool
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Flux

fun Routing.configureUserRoutes(pool: ConnectionPool) {
    get("/") {
        call.respondText("Ktor R2DBC with H2 In-Memory Database")
    }

    get("/users") {
        val users = mutableListOf<User>()

        val connection = pool.create().awaitSingle()
        try {
            val result = connection.createStatement("SELECT id, name, email FROM users")
                .execute()
                .awaitSingle()

            Flux.from(result.map { row, _ ->
                User(
                    id = row.get("id", Integer::class.java)?.toInt() ?: 0,
                    name = row.get("name", String::class.java) ?: "",
                    email = row.get("email", String::class.java) ?: ""
                )
            }).collectList().awaitSingle().forEach { users.add(it) }
        } finally {
            mono { connection.close() }.awaitFirstOrNull()
        }

        call.respond(users)
    }

    get("/users/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            return@get
        }

        var user: User? = null

        val connection = pool.create().awaitSingle()
        try {
            val result = connection.createStatement("SELECT id, name, email FROM users WHERE id = $1")
                .bind("$1", id)
                .execute()
                .awaitFirstOrNull()

            if (result != null) {
                user = Flux.from(result.map { row, _ ->
                    User(
                        id = row.get("id", Integer::class.java)?.toInt() ?: 0,
                        name = row.get("name", String::class.java) ?: "",
                        email = row.get("email", String::class.java) ?: ""
                    )
                }).collectList().awaitSingle().firstOrNull()
            }
        } finally {
            mono { connection.close() }.awaitFirstOrNull()
        }

        if (user != null) {
            call.respond(user)
        } else {
            call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "User not found"))
        }
    }

    post("/users") {
        val request = call.receive<CreateUserRequest>()

        val connection = pool.create().awaitSingle()
        val newUserId = try {
            val result = connection.createStatement(
                "INSERT INTO users (name, email) VALUES ($1, $2)"
            )
                .bind("$1", request.name)
                .bind("$2", request.email)
                .execute()
                .awaitSingle()

            result.rowsUpdated.awaitFirstOrNull()

            // Get the newly created user ID
            val idResult = connection.createStatement("SELECT MAX(id) as id FROM users")
                .execute()
                .awaitSingle()

            Flux.from(idResult.map { row, _ ->
                row.get("id", Integer::class.java)?.toInt()
            }).collectList().awaitSingle().firstOrNull()
        } finally {
            mono { connection.close() }.awaitFirstOrNull()
        }

        if (newUserId != null) {
            call.respond(
                io.ktor.http.HttpStatusCode.Created,
                User(id = newUserId, name = request.name, email = request.email)
            )
        } else {
            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to create user")
            )
        }
    }

    delete("/users/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            return@delete
        }

        val connection = pool.create().awaitSingle()
        val rowsDeleted = try {
            val result = connection.createStatement("DELETE FROM users WHERE id = $1")
                .bind("$1", id)
                .execute()
                .awaitSingle()

            result.rowsUpdated.awaitSingle()
        } finally {
            mono { connection.close() }.awaitFirstOrNull()
        }

        if (rowsDeleted > 0) {
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        } else {
            call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "User not found"))
        }
    }
}
