package com.rain.componentguard.tasks

import com.rain.componentguard.utils.LogProguard
import com.rain.componentguard.entensions.GuardExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * User: rain
 * Date: 2023/7/26
 * Time: 22:00
 * todo:
 */
open class RunAllProguard @Inject constructor(
    private val guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "componentproguard"
        dependsOn(
            "ReNameDrawable$variantName",
            "ReNameRaw$variantName",
            "ReNameXml$variantName",
            "ReNameComponent$variantName",
            "ImageChangesRandomlyByOnePixel$variantName",
            "NewBranch$variantName"
        )
    }

    @TaskAction
    fun execute() {
        LogProguard.log("=============================")
        LogProguard.log("RunAllProguard")
        LogProguard.log("=============================")
    }


}