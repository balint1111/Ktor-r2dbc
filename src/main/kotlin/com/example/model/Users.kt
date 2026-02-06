package com.example.model

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
}
