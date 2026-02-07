package com.example.routes

import com.example.model.CreateUserRequest
import com.example.model.ErrorResponse
import com.example.model.PageResponse
import com.example.model.UserResponse
import com.example.service.UserService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.configureUserRoutes(userService: UserService) {
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

        val (users, total) = userService.getUsers(page, size)

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

        val user = userService.getUserById(id)

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

        val newUser = userService.createUser(request)

        call.respond(
            io.ktor.http.HttpStatusCode.Created,
            UserResponse(newUser)
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

        val deleted = userService.deleteUser(id)

        if (deleted) {
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        } else {
            call.respond(
                io.ktor.http.HttpStatusCode.NotFound,
                ErrorResponse(error = "User not found")
            )
        }
    }
}
