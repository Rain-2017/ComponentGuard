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
import javax.inject.Inject
import kotlin.random.Random

/**
 * User: rain
 * Date: 2023/7/26
 * todo:
 * 1 raw目录查找，目前不够精准，raw可能不在main目录下
 * 2 不同module直接raw是否会重复
 * 3 不同module的raw下的文件相同
 */
open class ReNameRaw @Inject constructor(
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

        pngAndXmlFiles(androidProjects, pngAndXmlFiles)

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
                    Regex("R.raw.${nameWithoutExtension}"),
                    "R.raw.${mapit.value}"
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

    private fun pngAndXmlFiles(androidProjects: List<Project>, pngAndXmlFiles: MutableList<File>) {
        androidProjects.forEach {
            LogProguard.log("layout:" + it.layout.projectDirectory.toString())

            val raws =
                File(it.layout.projectDirectory.toString() + File.separator + "src" + File.separator + "main" + File.separator + "res" + File.separator + "raw")
//            LogProguard.log("raws:" + raws.path)
            raws.walk().filter {
                it.isFile
            }.forEach {
//                LogProguard.log("rawFile:" + it.path)
                pngAndXmlFiles.add(it)

            }
        }


    }

    private fun addLayoutAndjavaCode(
        androidProjects: List<Project>,
        layoutAndjavaCodes: MutableList<File>
    ) {
        androidProjects.forEach {
            //src目录java、kt代码
            layoutAndjavaCode(it.project, layoutAndjavaCodes)

//            LogProguard.log("manifestFile:${it.project.manifestFile()}")
            //Androidmainfest.xml
            layoutAndjavaCodes.add(it.project.manifestFile())


            //res目录
            val res = it.project.resDirs(variantName)
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