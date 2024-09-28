package com.rain.componentguard.tasks

import com.rain.componentguard.entensions.GuardExtension
import com.rain.componentguard.utils.LogProguard
import com.rain.componentguard.utils.allDependencyAndroidProjects
import com.rain.componentguard.utils.resDirs
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Random
import javax.inject.Inject

/**
 * User: rain
 * Date: 2023/7/26
 */
open class ImageChangesRandomlyByOnePixel @Inject constructor(
    private val guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "componentproguard"
    }

    @TaskAction
    fun execute() {
        LogProguard.log("=============================")
        LogProguard.log("ImageChangesRandomlyByOnePixel")
        LogProguard.log("=============================")
        val androidProjects = allDependencyAndroidProjects()
        androidProjects.forEach { it ->
            val res = it.project.resDirs(variantName)
            LogProguard.log("res=$res")

            res.forEach { file ->
                file.walk().filter {
                    it.isFile
                }
                    .filter {
                        it.extension in listOf("png", "jpg", "webp")
                    }
                    .forEach {
                        randomImg(it.toString())
                    }
            }
        }


    }

    private fun randomImg(path: String) {
        //加载 JPEG 图片
        val inputFile = File(path)
        LogProguard.log("randomImg:${inputFile.path}")
        val image = javax.imageio.ImageIO.read(inputFile)
        LogProguard.log("image:$image")

        // 获取指定像素的 RGB 值
        for (i in 0 until 3) {
            val x = Random().nextInt(image.getWidth() - 1);
            val y = Random().nextInt(image.getHeight() - 1);
            val color = java.awt.Color(image.getRGB(x, y), true)
            image.setRGB(x, y, color.brighter().getRGB())
        }
        val type = path.substring(path.lastIndexOf(".") + 1, path.length)
        javax.imageio.ImageIO.write(image, type, inputFile)
    }
}