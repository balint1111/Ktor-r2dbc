package com.example.config

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import java.sql.DriverManager
import java.time.Duration

private const val DatabaseName = "testdb"
private const val DatabaseUser = "sa"
private const val DatabasePassword = ""
private const val ChangelogPath = "db/changelog/db.changelog-master.yaml"

fun createConnectionPool(): ConnectionPool {
    val options = ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "h2")
        .option(ConnectionFactoryOptions.PROTOCOL, "mem")
        .option(ConnectionFactoryOptions.DATABASE, DatabaseName)
        .option(ConnectionFactoryOptions.USER, DatabaseUser)
        .option(ConnectionFactoryOptions.PASSWORD, DatabasePassword)
        .build()

    val connectionFactory: ConnectionFactory = ConnectionFactories.get(options)

    val poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
        .maxIdleTime(Duration.ofMinutes(30))
        .initialSize(5)
        .maxSize(20)
        .build()

    return ConnectionPool(poolConfig)
}

suspend fun initializeDatabase() {
    val jdbcUrl = "jdbc:h2:mem:$DatabaseName;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    DriverManager.getConnection(jdbcUrl, DatabaseUser, DatabasePassword).use { connection ->
        val liquibaseConnection = JdbcConnection(connection)
        Liquibase(ChangelogPath, ClassLoaderResourceAccessor(), liquibaseConnection)
            .update("")
    }
}
