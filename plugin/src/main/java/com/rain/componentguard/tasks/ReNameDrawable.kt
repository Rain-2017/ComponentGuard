package com.rain.componentguard.tasks

import com.rain.componentguard.utils.LogProguard
import com.rain.componentguard.entensions.GuardExtension
import com.rain.componentguard.utils.StringUtil
import com.rain.componentguard.utils.allDependencyAndroidProjects
import com.rain.componentguard.utils.javaDirs
import com.rain.componentguard.utils.manifestFile
import com.rain.componentguard.utils.resDirs
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileFilter
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.random.Random

/**
 * User: rain
 * Date: 2023/7/26
 * Time: 22:00
 * todo:
 * 1 webp图片处理
 * 2 颜色值改名处理
 */
open class ReNameDrawable @Inject constructor(
    private val guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "componentproguard"
    }

    @TaskAction
    fun execute() {
        LogProguard.log("=============================")
        LogProguard.log("ReNameDrawable")
        LogProguard.log("=============================")


        val androidProjects = allDependencyAndroidProjects()
        var pngAndXmlFiles: MutableList<File> = mutableListOf()
        var layoutAndjavaCodes: MutableList<File> = mutableListOf()

        androidProjects.forEach {
            pngAndXmlFiles(it.project, pngAndXmlFiles)
        }

        val map: MutableMap<String, String> = mutableMapOf()
        pngAndXmlFilesHandler(map, pngAndXmlFiles)

        addLayoutAndjavaCode(androidProjects, layoutAndjavaCodes)
        layoutAndjavaCodesHandler(map, layoutAndjavaCodes)

    }

    private fun layoutAndjavaCodesHandler(
        map: MutableMap<String, String>,
        layoutAndjavaCodes: MutableList<File>
    ) {
        layoutAndjavaCodes?.forEach {
//            LogProguard.log("file:${it.path}")
            var content = it.readText()
//            LogProguard.log("content:$content")
            map.forEach { mapit ->
                val name = mapit.key
                val nameWithoutExtension = if (name.lastIndexOf('.') > 0) name.substring(
                    0,
                    name.lastIndexOf('.')
                ) else name

                content = content.replace(
                    Regex("@drawable/${nameWithoutExtension}(?=[^0-9a-zA-Z_])"),
                    "@drawable/${mapit.value}"
                )
                content = content.replace(
                    Regex("@mipmap/${nameWithoutExtension}(?=[^0-9a-zA-Z_])"),
                    "@mipmap/${mapit.value}"
                )
                content = content.replace(
                    Regex("R.drawable.${nameWithoutExtension}(?=[^0-9a-zA-Z_])"),
                    "R.drawable.${mapit.value}"
                )
                content = content.replace(
                    Regex("R.mipmap.${nameWithoutExtension}(?=[^0-9a-zA-Z_])"),
                    "R.mipmap.${mapit.value}"
                )
//                LogProguard.log("it->write:$content")
                it.writeText(content)
            }
        }
    }

    private fun pngAndXmlFilesHandler(
        map: MutableMap<String, String>,
        pngAndXmlFiles: MutableList<File>
    ) {
        pngAndXmlFiles.forEach {
            var name = it.name

//            LogProguard.log("pngAndXmlFiles->name:$name")
            val fileExtension =
                if (it.name.lastIndexOf('.') > 0) it.name.substring(it.name.lastIndexOf('.') + 1) else "" // 获取文件后缀名

            name = name?.let {
                name.substring(0, name.lastIndexOf('.'))
            }
            var newName = map.get(name)
            if (newName == null) {
                newName =
                    StringUtil.generateWords(2) + StringUtil.generateNonce(6 + Random.nextInt(10))
                LogProguard.log("newName:$newName")
                map.put(name, newName)
            }

            //todo 修改PNG图像，防止MD5一致

            //修改文件名
            it.renameTo(File(it.getParent(), newName + "." + fileExtension))

            LogProguard.log("pngAndXmlFiles: ${it.path} --> $newName")
        }
    }

    private fun pngAndXmlFiles(project: Project, pngAndXmlFiles: MutableList<File>) {
        var ress = project.resDirs(variantName)
        LogProguard.log("res=$ress")
        ress.forEach {
            if (it.exists()) {
                it.listFiles().forEach {
                    val pngFiles = it.listFiles(FileFilter {
                        val path = it.path.toString()
                        val pattern1 = ".+drawable.+.png"
                        val pattern2 = ".+drawable.+.xml"
                        val pattern3 = ".+drawable.+.jpg"
                        val pattern4 = ".+drawable.+.webp"
                        val pattern5 = ".+mipmap.+.webp"
                        val pattern6 = ".+mipmap.+.png"
                        val pattern7 = ".+mipmap.+.jpg"
                        val pattern8 = ".+mipmap.+.xml"

                        val pattern = arrayOf(
                            pattern1,
                            pattern2,
                            pattern3,
                            pattern4,
                            pattern5,
                            pattern6,
                            pattern7,
                            pattern8
                        )

                        var flag = false
                        pattern.forEach {
                            flag = flag || Pattern.matches(it, path)
                        }
                        LogProguard.log("content:${it.path.toString()}  isMatch:$flag")
                        flag
                    })
                    if (pngFiles != null && pngFiles.isNotEmpty()) {
                        pngAndXmlFiles?.addAll(pngFiles)
                    }
                }
            }
        }
    }

    private fun addLayoutAndjavaCode(
        androidProjects: List<Project>,
        layoutAndjavaCodes: MutableList<File>
    ) {
        androidProjects.forEach { project ->
            //src目录java、kt代码
            layoutAndjavaCode(project, layoutAndjavaCodes)
            //Androidmainfest.xml
            layoutAndjavaCodes.add(project.manifestFile())
            //res目录
            val res = project.resDirs(variantName)
            res.forEach {
                val fileTree: FileTreeWalk = it.walk()
                fileTree
                    .filter {
                        it.isFile
                    }
                    .filter {
//                        LogProguard.log("extension:${it.extension}")
                        it.extension in listOf("xml")
                    }
                    .forEach {
//                        LogProguard.log("filenewres->${it.path}")
                        layoutAndjavaCodes.add(it)
                    }
            }
        }
    }

    private fun layoutAndjavaCode(project: Project, layoutAndjavaCode: MutableList<File>) {
        var javaDirs = project.javaDirs(variantName)
        LogProguard.log("res=$javaDirs")
        javaDirs.forEach {
            if (it.exists() && it.isDirectory) {
                LogProguard.log("it:" + it.path)

                val fileTree: FileTreeWalk = it.walk()
                fileTree
                    .filter {
                        it.isFile
                    }
                    .filter {
//                        LogProguard.log("extension:${it.extension}")
                        it.extension in listOf("java", "kt", "c++", "xml")
                    }
                    .forEach {
                        layoutAndjavaCode?.add(it)
                    }
            }
        }
    }


}