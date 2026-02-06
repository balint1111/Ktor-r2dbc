package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String
)

@Serializable
data class UserResponse(
    val user: User
)

@Serializable
data class UsersPageResponse(
    val users: List<User>,
    val page: Int,
    val size: Int,
    val total: Long
)

@Serializable
data class ErrorResponse(
    val error: String
)
