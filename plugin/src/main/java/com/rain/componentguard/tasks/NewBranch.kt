package com.rain.componentguard.tasks

import com.rain.componentguard.entensions.GuardExtension
import com.rain.componentguard.utils.LogProguard
import org.gradle.api.tasks.Exec
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject


open class NewBranch @Inject constructor(
    private val guardExtension: GuardExtension,
    private val variantName: String,
) : Exec() {

    init {
        group = "componentproguard"

        executable = "git"

        var componentGuardDate = Date(System.currentTimeMillis())
        val componentGuard = SimpleDateFormat("yyyyMMdd-HHmmss").format(componentGuardDate)
        LogProguard.log(componentGuard)
        args = listOf("checkout", "-b", componentGuard)

    }


}