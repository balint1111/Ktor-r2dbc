package com.example.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jetbrains.exposed.sql.Database
import java.sql.DriverManager
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

fun createDatabase(pool: ConnectionPool): Database {
    return Database.connect(pool)
}

fun initializeDatabase() {
    DriverManager.getConnection(
        "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "sa",
        ""
    ).use { connection ->
        val database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(JdbcConnection(connection))
        Liquibase(
            "db/changelog/db.changelog-master.yaml",
            ClassLoaderResourceAccessor(),
            database
        ).use { liquibase ->
            liquibase.update()
        }
    }
}
