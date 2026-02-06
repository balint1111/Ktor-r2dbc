package com.example.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import java.time.Duration

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
