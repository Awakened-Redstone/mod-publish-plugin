package me.modmuss50.mpp.test.modrinth

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class ModrinthTest : IntegrationTest {
    @Test
    fun uploadModrinth() {
        val server = MockWebServer(MockModrinthApi())

        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
            
                modrinth {
                    accessToken = "123"
                    projectId = "123456"
                    minecraftVersions.add("1.20.1")
                    
                    requires {
                        projectId = "P7dR8mSH"
                    }
                    
                    apiEndpoint = "${server.endpoint}"
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
    }

    @Test
    fun uploadModrinthWithOptions() {
        val server = MockWebServer(MockModrinthApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                
                    // Common options that can be re-used between diffrent modrinth tasks
                    val modrinthOptions = modrinthOptions {
                        accessToken = "123"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    modrinth("modrinthFabric") {
                        from(modrinthOptions)
                        file = tasks.jar.flatMap { it.archiveFile }
                        projectId = "123456"
                        modLoaders.add("fabric")
                        requires {
                           projectId = "P7dR8mSH" // fabric-api
                        }
                    }
                    
                    modrinth("modrinthForge") {
                        from(modrinthOptions)
                        file = tasks.jar.flatMap { it.archiveFile }
                        projectId = "789123"
                        modLoaders.add("forge")
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinthFabric")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinthForge")!!.outcome)
    }

    @Test
    fun dryRunModrinth() {
        val result = gradleTest()
            .buildScript(
                """
            publishMods {
                file = tasks.jar.flatMap { it.archiveFile }
                changelog = "Hello!"
                version = "1.0.0"
                type = STABLE
                modLoaders.add("fabric")
                dryRun = true

                modrinth {
                    accessToken = providers.environmentVariable("TEST_TOKEN_THAT_DOES_NOT_EXISTS")
                    projectId = "123456"
                    minecraftVersions.add("1.20.1")
                }
            }
                """.trimIndent(),
            )
            .run("publishModrinth")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishModrinth")!!.outcome)
    }
}