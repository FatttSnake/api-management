package top.fatweb.apimanagement

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.io.File
import java.util.UUID

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@ConfigurationPropertiesScan("top.fatweb.apimanagement.properties")
class ApiManagementApplication

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("main")

    if (!File("data").isDirectory) {
        if (!File("data").mkdir()) {
            logger.error("Can not create directory 'data', please try again later.")
            return
        }
    }

    if (!File("data/db").isDirectory) {
        if (!File("data/db").mkdir()) {
            logger.error("Can not create directory 'data/db', please try again later.")
            return
        }
    }

    if (!File("data/db/sqlite.db").isFile || File("data/db/sqlite.db").inputStream()
            .use { it.readNBytes(15).toString(Charsets.UTF_8) != "SQLite format 3" }
    ) {
        logger.warn("The 'data/db/sqlite.db' database is lost or damaged, recreating...")
        if (File("data/db/sqlite.db").exists() && !File("data/db/sqlite.db").delete()) {
            logger.error("Can not recreate database 'data/db/sqlite.db', please try again later.")
        }
    }

    if (File("application-config.yml").exists() || File("data/application-config.yml").exists()) {
        runApplication<ApiManagementApplication>(*args)
    } else {
        logger.warn("File 'application-config.yml' cannot be found in the running path or the data path. The configuration file template 'application-config.example.yml' has been created in directory 'data'. Please change the configuration file content, move it to the running path, rename it to 'application-config.yml', and then restart the server.")
        ApiManagementApplication::class.java.getResource("/application-config-template.yml")?.readText()?.let {
            File("data/application-config.example.yml").writeText(
                it.replace(
                    $$"$uuid$", UUID.randomUUID().toString()
                )
            )
        }
    }
}
