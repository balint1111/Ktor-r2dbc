package com.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    
    // Initialize R2DBC connection pool with H2 in-memory database
    val connectionPool = createConnectionPool()
    
    // Initialize database schema
    runBlocking {
        initializeDatabase(connectionPool)
    }
    
    // Configure plugins
    install(ContentNegotiation) {
        json()
    }
    
    // Configure routing
    routing {
        configureRoutes(connectionPool)
    }
    
    logger.info("Application started successfully with R2DBC H2 in-memory database")
}

fun createConnectionPool(): ConnectionPool {
    val options = ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "h2")
        .option(ConnectionFactoryOptions.PROTOCOL, "mem")
        .option(ConnectionFactoryOptions.DATABASE, "testdb")
        .option(ConnectionFactoryOptions.USER, "sa")
        .option(ConnectionFactoryOptions.PASSWORD, "")
        .build()
    
    val connectionFactory: ConnectionFactory = ConnectionFactories.get(options)
    
    val poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
        .maxIdleTime(Duration.ofMinutes(30))
        .initialSize(5)
        .maxSize(20)
        .build()
    
    return ConnectionPool(poolConfig)
}

suspend fun initializeDatabase(pool: ConnectionPool) {
    val connection = pool.create().awaitSingle()
    try {
        connection.createStatement(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL UNIQUE
            )
            """
        ).execute().awaitFirstOrNull()
        
        // Insert sample data
        connection.createStatement(
            "INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com')"
        ).execute().awaitFirstOrNull()
        
        connection.createStatement(
            "INSERT INTO users (name, email) VALUES ('Jane Smith', 'jane@example.com')"
        ).execute().awaitFirstOrNull()
    } finally {
        mono { connection.close() }.awaitFirstOrNull()
    }
}

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

fun Routing.configureRoutes(pool: ConnectionPool) {
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
            call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create user"))
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
