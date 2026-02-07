package com.example.model

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object Users : IntIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
}
