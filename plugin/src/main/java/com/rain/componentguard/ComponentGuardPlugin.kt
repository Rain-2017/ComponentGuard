package com.rain.componentguard

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.rain.componentguard.entensions.GuardExtension
import com.rain.componentguard.tasks.ImageChangesRandomlyByOnePixel
import com.rain.componentguard.tasks.NewBranch
import com.rain.componentguard.tasks.ReNameComponent
import com.rain.componentguard.tasks.ReNameDrawable
import com.rain.componentguard.tasks.ReNameRaw
import com.rain.componentguard.tasks.ReNameXml
import com.rain.componentguard.tasks.RunAllProguard
import com.rain.componentguard.utils.AgpVersion
import com.rain.componentguard.utils.LogProguard
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import kotlin.reflect.KClass

/**
 * User: rain
 * Date: 2023/7/26
 * Time: 22:00
 */
class ComponentGuardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkApplicationPlugin(project)
        LogProguard.log("ComponentGuard version is $version, agpVersion=${AgpVersion.agpVersion}")
        val guardExt = project.extensions.create("ComponentGuard", GuardExtension::class.java)

        val android = project.extensions.getByName("android") as AppExtension
        project.afterEvaluate {
            android.applicationVariants.all { variant ->
                it.createTasks(guardExt, variant)
            }
        }
    }

    private fun Project.createTasks(guardExt: GuardExtension, variant: ApplicationVariant) {
        val variantName = variant.name.capitalize()
        createTask("RunAllProguard$variantName", RunAllProguard::class, guardExt, variantName)
        createTask("NewBranch$variantName", NewBranch::class, guardExt, variantName)
        createTask("ReNameDrawable$variantName", ReNameDrawable::class, guardExt, variantName)
        createTask("ReNameRaw$variantName", ReNameRaw::class, guardExt, variantName)
        createTask("ReNameXml$variantName", ReNameXml::class, guardExt, variantName)
        createTask("ReNameComponent$variantName", ReNameComponent::class, guardExt, variantName)
        createTask(
            "ImageChangesRandomlyByOnePixel$variantName",
            ImageChangesRandomlyByOnePixel::class,
            guardExt,
            variantName
        )
    }

    private fun checkApplicationPlugin(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("Android Application plugin required")
        }
    }

    private fun <T : Task> Project.createTask(
        taskName: String,
        taskClass: KClass<T>,
        vararg params: Any
    ): Task = tasks.findByName(taskName) ?: tasks.create(taskName, taskClass.java, *params)
}