
# ComponentGuard 简介

- `ComponentGuard`可混淆Android 4大组件、自定义View等任意类的插件
- `ComponentGuard`可以增加aab、apk反编译的难度
- `ComponentGuard`降低aab包查重率，避免上架`Google Play`因查重率过高，导致app下架或封号问题


# 使用插件

在项目级 build.gradle 文件中将插件添加到 classpath：

## 项目 build.gradle

```kotlin
buildscript {
    repositories {
        ......
        maven { url 'https://gitee.com/wp3355168/ComponentGuard/raw/master' }
    }
    dependencies {
	    ....
        classpath "com.rain.componentguard:ComponentGuard:xxxxx"
    }
    ...
}
```

注意：

1、maven地址拉取
```
maven { url 'https://gitee.com/wp3355168/ComponentGuard/raw/master' }
    } 
地址如果拉取不到可以尝试
maven { url 'https://raw.githubusercontent.com/wp3355168/ComponentGuard/master' } 或
maven { url 'https://raw.fastgit.org/wp3355168/ComponentGuard/master' }
```
2、com.rain.componentguard:ComponentGuard:xxxx中的xxxx换成对应的版本号比如1.0.0.0

## 模块 build.gradle
在模块级 build.gradle 文件中 apply 插件：
```kotlin
apply plugin: 'com.rain.componentguard'
```

# 插件生效

完成以上步骤并同步Gradle。点击下图右边的Sync Project with Gradle Files进行Gradle同步，看到Tasks中有标红componentproguard的Task说明集成成功。
![输入图片说明](../main/png/1.png)

## 使用插件
按需点击下面图片中的RunAllProguardDebug或者RunAllProguardRelease来进行整体插件的使用
![输入图片说明](../main/png/2.png)
