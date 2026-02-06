package com.example

import com.example.config.createConnectionPool
import com.example.config.createDatabase
import com.example.config.initializeDatabase
import com.example.routes.configureUserRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*
import io.ktor.server.swagger.*
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, port = 9090, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    
    // Initialize R2DBC connection pool with H2 in-memory database
    val connectionPool = createConnectionPool()
    val database = createDatabase(connectionPool)
    
    // Initialize database schema
    initializeDatabase()
    
    // Configure plugins
    install(ContentNegotiation) {
        json()
    }
    
    // Configure routing
    routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        configureUserRoutes(database)
    }
    
    logger.info("Application started successfully with R2DBC H2 in-memory database")
}
