package com.example.service

import com.example.model.CreateUserRequest
import com.example.model.User
import com.example.repository.UserRepository

class UserService(private val userRepository: UserRepository) {
    suspend fun getUsers(page: Int, size: Int): Pair<List<User>, Long> {
        return userRepository.fetchUsers(page, size)
    }

    suspend fun getUserById(id: Int): User? {
        return userRepository.findUserById(id)
    }

    suspend fun createUser(request: CreateUserRequest): User {
        return userRepository.createUser(request)
    }

    suspend fun deleteUser(id: Int): Boolean {
        return userRepository.deleteUser(id)
    }
}
