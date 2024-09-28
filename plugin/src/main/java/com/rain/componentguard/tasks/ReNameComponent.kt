package com.rain.componentguard.tasks

import com.rain.componentguard.utils.LogProguard
import com.rain.componentguard.entensions.GuardExtension
import com.rain.componentguard.utils.StringUtil
import com.rain.componentguard.utils.findPackage
import com.rain.componentguard.utils.javaDirs
import com.rain.componentguard.utils.manifestFile
import groovy.namespace.QName
import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * User: rain
 * Date: 2023/7/26
 */
open class ReNameComponent @Inject constructor(
    private val guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {


    init {
        group = "componentproguard"
    }

    @TaskAction
    fun execute() {
        LogProguard.log("=============================")
        LogProguard.log("ReNameComponent")
        LogProguard.log("=============================")
        val pkgMapping: MutableMap<String, String> = mutableMapOf()

        val manifestfile = project.manifestFile()
        val androidManifest = XmlParser().parse(manifestfile)
        val packageName = project.findPackage()

        val fullClassNameMapping: MutableMap<String, String> = mutableMapOf()
        val classNameMap: MutableMap<String, String> = mutableMapOf()
        val pkgMap: MutableMap<String, String> = mutableMapOf()

        val java: MutableList<File> = mutableListOf()
        parseJavaDirs(java)
        handlerJavaDirs(java, pkgMapping, fullClassNameMapping)

        val kotlin: MutableList<File> = mutableListOf()
        parseKotlin(kotlin)
        handlerKotlin(kotlin, pkgMapping, fullClassNameMapping)


        fullClassNameMapping.forEach { (key, value) ->
            val className = key.substring(key.lastIndexOf(".") + 1)
            val pkg = key.substring(0, key.lastIndexOf("."))
            val newClassName = value.substring(value.lastIndexOf(".") + 1)
            val newPkg = value.substring(0, value.lastIndexOf("."))
            classNameMap.put(className, newClassName)
            pkgMap.put(pkg, newPkg)
        }

        //application
        val application: NodeList = androidManifest.value() as NodeList
        val activity: NodeList = application.getAt("activity")
        val service: NodeList = application.getAt("service")
        val receiver: NodeList = application.getAt("receiver")
        val provider: NodeList = application.getAt("provider")
        renameComponentName(application, packageName, fullClassNameMapping)
        renameComponentName(activity, packageName, fullClassNameMapping)
        renameComponentName(service, packageName, fullClassNameMapping)
        renameComponentName(receiver, packageName, fullClassNameMapping)
        renameComponentName(provider, packageName, fullClassNameMapping)
        //修改清单文件
        XmlNodePrinter(PrintWriter(FileWriter(manifestfile))).print(androidManifest)

        val javaCode: MutableList<File> = mutableListOf()
        parseJavaCodesDirs(javaCode)
        handlerJavaCoesDirs(javaCode, fullClassNameMapping, pkgMap, classNameMap, packageName)
    }

    private fun renameComponentName(
        activitys: NodeList,
        packageName: String,
        fullClassNameMapping: MutableMap<String, String>
    ) {
        LogProguard.log("renameComponentName:$activitys")
        LogProguard.log("packageName:$packageName")
        activitys.forEach { activity ->

            val map = (activity as Node).attributes()
            val attributes: MutableMap<String, String> = mutableMapOf()
            // 遍历 LinkedHashMap，并打印键值对和键的类型

            map.forEach { key, value ->
                val keyName: QName = key as QName
                attributes.put(keyName.localPart, value.toString())
                if (keyName.localPart == "name") {
                    // 获取 <activity> 元素的 android:name 属性值
                    var activityName = value.toString()
                    LogProguard.log("activityName:$activityName")


                    if (activityName.startsWith(".")) {
                        var activity_suffix = ""
                        var activity_pre =
                            if (activityName.contains("$")) {
                                val aa = activityName.split("$")
                                activity_suffix = aa[1]
                                aa[0]
                            } else {
                                activityName
                            }

                        activity_pre = packageName + activity_pre

                        var newName = if (activity_pre.isNotEmpty()) {
                            fullClassNameMapping.get(activity_pre)
                        } else {
                            activityName
                        }

                        if (newName != null && newName != "") {

                            if (activity_suffix.isNotEmpty()) {
                                newName = newName + "$" + activity_suffix
                            }
                            activity.attributes().put(key, newName)
                        }
                    } else {
                        var newName = fullClassNameMapping.get(activityName)
                        activity.attributes().put(key, newName)
                    }


                }
            }
        }


    }


    private fun parseJavaDirs(java: MutableList<File>) {
        project.javaDirs(variantName).forEach {
            val fileTree: FileTreeWalk = it.walk()
            fileTree
                .filter {
                    it.isFile
                }
                .filter {
                    it.extension in listOf("java")
                }
                .forEach {
                    java?.add(it)
                }
        }
    }

    private fun parseJavaCodesDirs(java: MutableList<File>) {
        project.javaDirs(variantName).forEach {
            val fileTree: FileTreeWalk = it.walk()
            fileTree
                .filter {
                    it.isFile
                }
                .filter {
                    it.extension in listOf("java", "kt", "c++", "xml")
                }
                .forEach {
                    java?.add(it)
                }
        }
    }

    private fun handlerJavaDirs(
        java: MutableList<File>,
        pkgMapping: MutableMap<String, String>,
        fullClassNameMapping: MutableMap<String, String>
    ) {
        java.forEach { file ->
            var path = file.absolutePath.replace(".java", "")
            var javaPath = project.javaDirs(variantName) + File.separator

            javaPath.forEach { name ->
                LogProguard.log("name:$name")
                if (name != null && name.toString().length > 1) {
                    if (path.contains(name.toString())) {
                        path = path.replace(name.toString(), "")
                    }
                }
            }
            LogProguard.log(path)
            path = path.replace("/", ".")
            val classPkg = path.substring(0, path.lastIndexOf("."))
            val className = path.substring(path.lastIndexOf(".") + 1, path.length)
            var pkg = pkgMapping.get(classPkg)
            LogProguard.log("pkg:$pkg")
            if (pkg == null || pkg == "") {
                pkg =
                    StringUtil.generateWords(6) + "." + StringUtil.generateWords(6) + "." + StringUtil.generateWords(
                        6
                    )
                LogProguard.log("classPkg:$classPkg  pkg:$pkg")
                pkgMapping.put(classPkg, pkg)
            }
            val newName = if (guardExtension.renameClassName) {
                pkg + "." + StringUtil.generateWords(7)
            } else {
                pkg + "." + className
            }
            //如果在keep名单里面，则不进行处理
            var inkeep = false
            guardExtension.keep.forEach {
                if (path.startsWith(it)) {
                    inkeep = true
                    LogProguard.log("keep: $path")
                }
            }
            if (!inkeep) {
                fullClassNameMapping.put(path, guardExtension.prefix + newName)
            }
        }
    }

    private fun handlerJavaCoesDirs(
        javaCode: MutableList<File>,
        fullClassNameMapping: MutableMap<String, String>,
        pkgMap: MutableMap<String, String>,
        classNameMap: MutableMap<String, String>, packageName: String
    ) {
        //同步源码文件
        javaCode.forEach { file ->
            // 处理满足条件的文件
            var content = file.readText()

            fullClassNameMapping.forEach { key, value ->
                content = content.replace(key, value)
            }
            pkgMap.forEach { old, newPkg ->
                LogProguard.log("old:$old   newPkg:$newPkg")
                var oldPkg = old
                oldPkg = oldPkg.replace(".", "\\.")
                var regex = Regex("\\s*package\\s+$oldPkg\\s*(;?)\n")
                content = content.replace(regex, "package $newPkg;\n")
                regex = Regex("(import\\s)($oldPkg\\.)([^\\.\\n]*\\n)")
                content =
                    content.replace(regex, "import $newPkg.*;\n")
            }
            classNameMap.forEach { oldClassName, newClassName ->
                var regex = Regex("(?<![a-zA-Z])$oldClassName(?![a-zA-Z])")
                content = content.replace(regex, newClassName)
            }

            //由于移动过代码包名了，需要添加对R文件的依赖
            val regex = Regex("(^package.*)")
            content = content.replace(regex, "\$1\nimport $packageName.*;")

            file.writeText(content)
        }

        //源码替换完了，移动文件
        fullClassNameMapping.forEach { key, value ->
            var javaPath = project.javaDirs(variantName) + File.separator
            javaPath.forEach { name ->
                if (name != null && name.toString().length > 1) {
                    var moveFile = File(name.toString(), key.replace(".", "/") + ".java")
                    if (!moveFile.exists()) {
                        moveFile = File(name.toString(), key.replace(".", "/") + ".kt")
                    }
                    if (moveFile.exists()) {
                        val newPath = value.replace(
                            ".",
                            "/"
                        ) + moveFile.name.substring(moveFile.name.lastIndexOf("."))
                        val dest = File(name.toString(), newPath)
                        dest.parentFile.mkdirs()
                        moveFile.renameTo(dest)
                    }
                }
            }
        }
        //删除以前文件
        fullClassNameMapping.forEach { (key, value) ->
            LogProguard.log("fullClassNameMapping:key->$key,value->$value")
            var javaPath = project.javaDirs(variantName) + File.separator
            javaPath.forEach { name ->
                if (name != null && name.toString().length > 1) {
                    var path = key.replace(".", "/")
                    path = path.substring(0, path.lastIndexOf("/"))
                    var moveFile = File(name.toString(), path)
                    LogProguard.log("moveFile:${moveFile.path}")
                    if (moveFile.isDirectory && moveFile.exists()) {
                        moveFile.delete()
                    }
                }
            }
        }
        //删除以前目录
        pkgMap.forEach { (key, value) ->
            var javaPath = project.javaDirs(variantName) + File.separator
            javaPath.forEach { name ->
                if (name != null && name.toString().length > 1) {
                    var path = key.replace(".", File.separator)
                    path = path.substring(0, path.lastIndexOf(File.separator))
                    var moveFile = File(name.toString(), path)
                    var parentFile = moveFile
                    while (parentFile != null && parentFile.exists() && parentFile.path != name.toString()) {
                        var tmpFile = parentFile
                        if (parentFile.isDirectory && parentFile.listFiles().size == 0) {
                            tmpFile = parentFile.parentFile
                            parentFile.delete()
                        } else {
                            tmpFile = parentFile.parentFile
                        }
                        parentFile = tmpFile
                    }
                }
            }
        }
    }

    private fun parseKotlin(kotlin: MutableList<File>) {
        project.javaDirs(variantName).forEach {
            val fileTree: FileTreeWalk = it.walk()
            fileTree
                .filter {
                    it.isFile
                }
                .filter {
                    it.extension in listOf("kt")
                }
                .forEach {
                    kotlin?.add(it)
                }
        }
    }

    private fun handlerKotlin(
        kotlin: MutableList<File>,
        pkgMapping: MutableMap<String, String>,
        fullClassNameMapping: MutableMap<String, String>
    ) {
        kotlin.forEach { file ->
            val content = file.readText()
            val pkgmatcher: Matcher = Pattern.compile("(^\\s*package\\s+)(.*)").matcher(content)
            var classPkg: String? = null
            if (pkgmatcher.find()) {
                classPkg = pkgmatcher.group(2)
            }

            val pattern: Pattern =
                Pattern.compile("(((internal)|(public)|(private))?)(\\s*)((data\\s+class)|(class)|(interface)|(object))(\\s+)([a-zA-Z0-9_]+)")
            val innerClassPattern: Pattern =
                Pattern.compile("(((internal)|(public)|(private))?)(\\s*)(inner\\s+class)(\\s+)([a-zA-Z0-9_]+)")
            val matcher: Matcher = pattern.matcher(content);
            val innerMatcher: Matcher = innerClassPattern.matcher(content);

            val matches: MutableList<String> = mutableListOf()
            while (matcher.find()) {
                matches.add(matcher.group(13))
            }

            val innerMatchers: MutableList<String> = mutableListOf()
            while (innerMatcher.find()) {
                innerMatchers.add(innerMatcher.group(9))
            }

            matches.removeAll(innerMatchers)

            for (className in matches) {
                val fullClassName = classPkg + "." + className
                var pkg = pkgMapping.get(classPkg)
                if (pkg == null || pkg == "") {
                    pkg =
                        StringUtil.generateWords(6) + "." + StringUtil.generateWords(6) + "." + StringUtil.generateWords(
                            6
                        )
                    if (classPkg != null && pkg != null) {
                        pkgMapping.put(classPkg, pkg!!)
                    }
                }
                val newName = if (guardExtension.renameClassName) {
                    pkg + "." + StringUtil.generateWords(7)
                } else {
                    pkg + "." + className
                }

                //如果在keep名单里面，则不进行处理
                var inkeep = false

                guardExtension.keep.forEach {
                    if (fullClassName.startsWith(it)) {
                        inkeep = true
                        LogProguard.log("keep: $fullClassName")
                    }
                }

                if (!inkeep) {
                    fullClassNameMapping.put(fullClassName, guardExtension.prefix + newName)
                }
            }
        }
    }
}