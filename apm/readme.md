************************************************************************************************************************
************************************************************************************************************************
************************************************************************************************************************
************************************************************************************************************************

三分钟快速集成ArgusAPM
一. Gradle配置 在 Project 的 build.gradle 文件中添加ArgusAPM的相关配置，示例如下：

在项目根目录的 build.gradle（注意：不是 app/build.gradle） 中添加以下配置：

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:2.2.3'
	    classpath 'com.qihoo360.argusapm:argus-apm-gradle-asm:3.0.1.1001'
    }
}

allprojects {
    repositories {
        jcenter()
    }
}
在app的build.gradle 文件中添加插件引用，示例如下：

apply plugin: 'argusapm'

//在android的配置代码块里面(为了兼容Android6.0系统):
android {
    useLibrary ‘org.apache.http.legacy‘
}
二. AndroidManifest.xml配置

a. 权限相关

<!--需要申请如下权限-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.BATTERY_STATS" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
b. 组件使用 需要在AndroidManifest.xml里添加如下组件声明：

    <provider
        android:name="com.argusapm.android.core.storage.ApmProvider"
		android:authorities="{当前应用的applicationId}.apm.storage"
		android:exported="false" />
三. 一个简单的SDK初始化代码

在项目的Application的attachBaseContext里调用如下代码即可

boolean isUi = TextUtils.equals(getPackageName(), ProcessUtils.getCurrentProcessName());
        Config.ConfigBuilder builder = new Config.ConfigBuilder()
                .setAppContext(this)
                .setRuleRequest(new RuleSyncRequest())
                .setUpload(new CollectDataSyncUpload())
                .setAppName("apm_demo")
                .setAppVersion("1.0.0")
                .setApmid("apm_demo");//该ID是在APM的后台进行申请的

        //单进程应用可忽略builder.setDisabled相关配置。
        if (!isUi) { //除了“主进程”，其他进程不需要进行数据上报、清理等逻辑。“主进程”通常为常驻进行，如果无常驻进程，即为UI进程。
            builder.setDisabled(ApmTask.FLAG_DATA_CLEAN)
                    .setDisabled(ApmTask.FLAG_CLOUD_UPDATE)
                    .setDisabled(ApmTask.FLAG_DATA_UPLOAD)
                    .setDisabled(ApmTask.FLAG_COLLECT_ANR)
                    .setDisabled(ApmTask.FLAG_COLLECT_FILE_INFO);
        }
        //builder.setEnabled(ApmTask.FLAG_COLLECT_ACTIVITY_AOP); //activity采用aop方案时打开，默认关闭即可。
        builder.setEnabled(ApmTask.FLAG_LOCAL_DEBUG); //是否读取本地配置，默认关闭即可。
        Client.attach(builder.build());
        Client.isDebugOpen(true);//设置成true的时候将会打开悬浮窗
        Client.startWork();
注意：

apmid(appkey)名称必须保证唯一性，由Argus APM统一分配，请勿随意填写。

停止对外接入服务，请创建自己的数据服务端。

上面的初始化只是针对单进程的APP，若是多进程请参考详细接入文档。

如此，移动性能监控 SDK就接入完成,更多技巧请参考详细接入文档。

四. 接入成功日志输出

接入完毕，运行应用，如果看到以下log，说明接入成功:



五. 混淆相关

无需任何混淆配置，因为ArgusAPM的AAR自带Proguard文件，直接接入ArgusAPM-SDK即可生效。




************************************************************************************************************************
************************************************************************************************************************
************************************************************************************************************************
************************************************************************************************************************
************************************************************************************************************************


深度剖析360移动性能监控平台ArgusAPM
背景介绍

ArgusAPM是360手机卫士基于Java和Kotlin开发的一套移动性能监控平台，致力于监控和管理应用软件性能和可用性，通过监测和诊断复杂应用程序的性能问题，来保证软件应用程序的良好运行(预期的服务)。

Argus是古希腊神话中的一个百眼巨人，可以观察到各个方向发生的事情，APM是Application Performance Management的缩写，是应用性能管理的意思。通过组合Argus和APM，我们是希望ArgusAPM能像Argus一样能够时时刻刻的去监控应用性能，当有损耗性能的问题出现时，能够及时的得到捕获和反馈，进而去解决这些问题，从而提升用户的体验。

整体架构

本篇文章采用由整体到局部的方式来讲解ArgusAPM，通过逐步细化的讲解，让大家更清晰的认识ArgusAPM。

首先，让我们来看一下整体架构是如何设计的，如下图：



从图中我们知晓，整体架构总共分为两部分，分别为左边的蓝色部分，代表性能采集的具体实现；右边的绿色部分，代表辅助功能插件，APP在接入ArgusAPM的时候只接入插件即可，因为我们已经将性能采集模块作为依赖库配置到了插件里面。下面我们针对这两部分做分别的介绍：

一. 性能采集模块（Java语言实现）

该模块整体分为五个Module，并最终生成三个aar，分别为argusapm-main.aar、argusapm-aop.aar、argusapm-okhttp.aar。看到这块，不知道大家是否在想，一个SDK为什么要分那么多的Module呢？做过SDK的小伙伴可能已经猜出来了，就是为了实现模块的可插拔，因为我们提供的SDK功能会随着业务的增长变得越来越多，如果都集中在一个Module里面的话就会导致包体积的增大，而有的应用可能只是想用我们的一些核心的数据采集，并不想要过多其他多余的功能，这个时候我们就可以通过开关来控制是否接入这些功能。例如，我的项目比较老，网络框架并未采用OKHTTP等目前比较主流的网络库，这个时候，我们就可以把采集OKHTTP的功能给关闭，这样也就不会引入相应的依赖库了，也就不再采集相应的数据。

我们知道，任何一个SDK基本上都会对外暴露一个入口，通过入口，我们可以做一些个性化配置，而我们的ArgusAPM遵循同样的规则，入口则是Client类。



在讲解这块之前，我先提一些问题，通过这些问题我们再逐步的去深入了解ArgusAPM，最终去窥探整个APM的完整面貌。

问题：

我们如何采集数据？
按照什么规则去采集，这些规则可否自定义配置？
采集完数据存放在哪，存放的数据量会不会越来越大？
何时上传至云端？
首先，我们会根据自己的业务规则定义个性化的配置，然后借助Client类的attach方法传递给Manager类，这样我们就能够根据用户自定义的规则来做相应的初始化。通过上图我们也可知晓，我们的Manager类用用来管理各个SubManager的，例如TaskManager就是用来管理我们实现的ITask的，每个ITask的实现都代表了一种类型的采集点，比如ActivityTask则是用来采集Activity性能相关信息的，AnrTask则是用来采集ANR的，MemoryTask则是采集内存使用状况的。如果我们想要新增一个采集点的实现，这时我们就需要去实现ITask，并注册进TaskManager，通过TaskManager来统一管理我们的Task，例如Task的开启和关闭，这也回答了我们的第一个问题。当然了，除了实现各个采集点的ITask之外，我们还需要做一些其他的工作，才能让我们的ArgusAPM更好的工作，例如我想通过云规则配置来控制每个ITask的开启，或者更改各个ITask的性能指标，这时候我们就要用到ArgusApmConfigManager类了，借助ArgusApmConfigManager类，我们可以对云规则做相应的初始化，并读取最新的云规则配置文件。如果用户想在某个版本更改云规则配置，只需要在云后端去更改规则即可，当APP下次再启动的时候就会读取最新的配置，并根据最新的配置去做相应的数据采集，这也是第二个问题的答案。我们既然要采集数据，肯定也会对数据做相应的存储，我们目前采用的是数据库存储，每当采集到新的数据，我们都会存储到数据库里，然后在某个合适的时机将数据上传至云端，如果上传成功，则会对本地的数据做个清理操作，防止下次再次上传和减少本地存储文件的大小，这也对第三个和第四个问题做出了回答。

关于云规则的获取和最终采集数据的上传，我们已经抽象成接口，如果用户想要自己做一个云规则后台，或者想要自己去维护采集到的数据，那么只需要实现相应的接口即可，然后再通过ConfigBuilder类配置具体实现即可。

为什么要有个性化配置？

根据业务场景的不同，各个APP的应用环境并不相同，有的是单进程架构，有的则是多进程架构，因此我们有必要对进程做出区分，尽可能的让常驻进程去做数据的上传和清理工作，其他进程则无需开启这些任务，这时候，我们就要针对不同的进程做不同的配置。

至此，我们简单的介绍了性能采集模块的工作流程，如果想深入的了解，请Fork我们的项目查看源码或者加入我们的官方QQ群进行讨论（QQ群二维码在文章末）。

二. 辅助功能模块（Kotlin语言实现）

该模块最终实现的是一个Gradle Plugin，通过对插件的实现，提供一些辅助功能操作，虽然是辅助功能，但也是SDK开发过程中必不可少的一部分。

该模块主要具备两个作用：

支持AOP编程，方便ArgusAPM能够在编译期织入一些性能采集的代码；

通过Gradle插件来管理依赖库，使用户接入ArgusAPM更简单。



最终，我们在接入ArgusAPM的时候，只需要简单的应用插件即可，而不需要再单独的去依赖各个aar文件。

针对上面的两个作用我们可能会有如下的两个疑问：

该插件是如何织入代码的呢？

该插件又是如何管理这些依赖库的呢？

首先，我们分析一下代码是如何织入的，这块涉及到的知识点相对来说比较杂，我做了一下简单的汇总：

名称	说明
Gradle Plugin	对如何实现一个Gradle Plugin有一定的了解
Transform	知道Transform是什么，工作时机
AspectJ 编译器	熟悉AspectJ编译期及其参数
Variants	productFlavors和buildTypes的结合体
如果大家对上面所列的知识点有所了解了，就能大概的猜出来我们是如何进行代码织入的。关于AOP编程，业界的实现方式也是比较多的，我们选取了AspectJ来实现我们想要的切入功能。说到切入，无非就是对AspectJ编译器有个比较深入的了解，然后通过配置AspectJ编译器的参数来实现对代码的切入功能，那么又是何时切入的呢？没错，就是在JavaC编译之后，Dex之前进行的操作，这个时机的Hook点，就是我们需要了解的Transform，通过Transform我们能够做很多的操作，例如我代码织入的时候是否支持增量编译，所要织入代码的作用域是哪些等。可又为什么需要了解Variants呢？这是因为如果有的项目配置了buildTypes和productFlavors，就会同时生成多个变体的apk或者aar，如果我们的项目不做相应的处理，那么最终生成的多个变体apk或者aar就不会都完全包含我们ArgusAPM的所有功能，最起码AOP相关的功能就会缺少。说了这么多，我们来看看这块功能的具体实现：



首先，我们自定义AspectJTransform并继承Transform抽象类，然后重写里面的方法，并实现相应的业务逻辑。考虑到插件的工作效率，我们实现了对插件的增量编译和并发编译，只要项目依赖的jar包越多，我们AspectJ织入的速度也会越快。在此，我们引入了一个切割的概念，我们通过对系统的所有class文件进行切割分组，并分别存放在aspectjs、include_dir、excluede_dir三个目录下。其中aspectjs目录下存放的是我们做AOP切入时的切面文件，include_dir目录下存放的是需要切入的文件，exclude_dir目录下存放的是不参与aspectj的织入，直接跳过即可。

切割完毕之后，我们就需要借助AspectJ编译器去实现代码的织入功能，针对每个jar包，我们都对应一个织入器，从而可以采用并发的方式进行织入，最终我们把织入后的文件存放在指定的目录即可，这样我们整个代码的织入功能就实现了。

那我们又是如何实现依赖库管理的呢？

其实，实现起来也比较简单，无非就是将在build.gradle脚本里应该做的操作，放在了插件里面来实现，这样实现的好处是，用户在接入SDK的时候，只需要应用插件即可，而不需要再单独的去配置依赖库，这样SDK内部的依赖库版本号都由SDK开发者自己来维护，除此之外，我们还针对依赖库的管理提供了自定义的配置功能，方便用户更好的使用该插件。

具体实现代码如下：

val COMPILE_CONFIGURATIONS = arrayOf("api", "compile")

/**
 * 兼容Compile模式
 */
fun Project.compatCompile(depLib: Any) {
    COMPILE_CONFIGURATIONS.find { configurations.findByName(it) != null }?.let {
        dependencies.add(it, depLib)
    }
}

class ArgusDependencyResolutionListener(val project: Project) : DependencyResolutionListener {
    override fun beforeResolve(dependencies: ResolvableDependencies?) {
        if (PluginConfig.argusApmConfig().dependencyEnabled) {
            if (PluginConfig.argusApmConfig().debugDependencies.isEmpty() && PluginConfig.argusApmConfig().moduleDependencies.isEmpty()) {
                project.compatCompile("com.qihoo360.argusapm:argus-apm-main:${AppConstant.VER}")
                project.compatCompile("com.qihoo360.argusapm:argus-apm-aop:${AppConstant.VER}")

                if (PluginConfig.argusApmConfig().okhttpEnabled) {
                    project.compatCompile("com.qihoo360.argusapm:argus-apm-okhttp:${AppConstant.VER}")
                }
            } else {
                //配置本地Module库，方便断点调试
                if (PluginConfig.argusApmConfig().moduleDependencies.isNotEmpty()) {
                    PluginConfig.argusApmConfig().moduleDependencies.forEach { moduleLib: String ->
                        project.compatCompile(project.project(moduleLib))
                    }
                }

                //发布Release版本之前，可以使用Debug库测试
                if (PluginConfig.argusApmConfig().debugDependencies.isNotEmpty()) {
                    project.repositories.mavenLocal()
                    //方便在测试的时候使用，不再需要单独的Gradle发版本
                    PluginConfig.argusApmConfig().debugDependencies.forEach { debugLib: String ->
                        project.compatCompile(debugLib)
                    }
                }
            }
        }
        project.gradle.removeListener(this)
    }

    override fun afterResolve(dependencies: ResolvableDependencies?) {
    }

}
至此，我们将ArgusAPM的性能采集模块和辅助模块都讲解完了，更多的细节，请参考源码。

写在最后

ArgusAPM目前已经在360公司内部得到广泛的应用，而且已经在GitHub开源。如有问题，请大家提issue或者反馈到我们的官方QQ群，我们会第一时间进行处理。当然了，开源是一个持续不断的过程，只有通过不断的改进，产品才能趋于完善，希望大家能够积极的加入我们，关注我们，让我们一起为解决移动性能而努力。

开源地址

https://github.com/Qihoo360/ArgusAPM





************************************************************************************************************************
************************************************************************************************************************
************************************************************************************************************************
************************************************************************************************************************
************************************************************************************************************************

360开源又一力作——ArgusAPM移动性能监控平台

公告

由于公司业务调整及成本等原因，ArgusAPM停止支持服务端的免费接入服务。对于已经接入的产品不受影响（可以继续免费使用），只是不再新增了！ 本项目是个技术类的开源项目，停止服务端接入不会对开源项目本身产生影响，之前的接入文档大家也可以在自己项目里做参考。

项目背景

ArgusAPM是360手机卫士客户端团队继RePlugin之后开源的又一个重量级开源项目。ArgusAPM是360移动端产品使用的可视化性能监控平台，为移动端APP提供性能监控与管理，可以迅速发现和定位各类APP性能和使用问题，帮助APP不断的提升用户体验。

产品价值

实时掌控应用性能
降低性能定位成本
有效提升用户体验
监控模块

ArgusAPM目前支持如下性能指标：

交互分析：分析Activity生命周期耗时，帮助提升页面打开速度，优化用户UI体验
网络请求分析：监控流量使用情况，发现并定位各种网络问题
内存分析：全面监控内存使用情况，降低内存占用
进程监控：针对多进程应用，统计进程启动情况，发现启动异常（耗电、存活率等）
文件监控：监控APP私有文件大小/变化，避免私有文件过大导致的卡顿、存储空间占用等问题
卡顿分析：监控并发现卡顿原因，代码堆栈精准定位问题，解决明显的卡顿体验
ANR分析：捕获ANR异常，解决APP的“未响应”问题
ArgusAPM特性

非侵入式
​ 无需修改原有工程结构，无侵入接入，接入成本低。

无性能损耗
​ ArgusAPM针对各个性能采集模块，优化了采集时机，在不影响原有性能的基础上进行性能的采集和分析。

监控全面
​ 目前支持UI性能、网络性能、内存、进程、文件、卡顿、ANR等各个维度的性能数据分析，后续还会继续增加新的性能维度。

Debug模式
​ 独有的Debug模式，支持开发和测试阶段、实时采集性能数据，实时本地分析的能力，帮助开发和测试人员在上线前解决性能问题。

支持插件化方案
​ 在初始化阶段进行设置，可支持插件接入，目前360手机卫士采用的就是在RePlugin插件中接入ArgusAPM，并且性能方面无影响。

支持多进程采集
​ 针对多进程的情况，我们做了相应的数据采集及优化方案，使ArgusAPM即适合单进程APP也适合多进程APP。

节省用户流量
​ ArgusAPM使用wifi状态下上传性能数据，这样避免了频繁网络请求带来的耗电问题及用户流量的消耗。

ArgusAPM项目结构图



整体架构分为两部分：一是左边蓝色的部分：性能采集模块，一是右边的绿色部分：Gradle Plugin模块。

下面分别针对这两部分做简单的介绍：

一. 性能采集模块

该模块总共分为五个Module，并最终生成三个aar文件，即：

argus-apm-main.aar：APM项目的核心业务模块

argus-apm-aop.aar：AOP代码的织入模块

argus-apm-okhttp.aar：采集OKHTTP网络性能

其中之所以拆分那么多的模块，是为了能够让我们可插拔式的去使用里面的功能，例如，如果我项目中没有使用OKHTTP相关的功能，那么我们就可以关闭相应的依赖。

二. Gradle Plugin模块

该模块主要具备两个作用：

支持AOP编程，方便ArgusAPM能够在编译期织入一些性能采集的代码；

通过Gradle插件来管理依赖库，使用户接入ArgusAPM更简单。



最终，我们在接入ArgusAPM的时候，只需要简单的应用插件即可，而不需要再单独的去依赖各个aar文件。

如何使用

如果您想快速的接入ArgusAPM，请参考《三分钟快速接入ArgusAPM》，依照文章指引，快速接入；

如果您想了解更多的ArgusAPM的使用技巧，请参考《详细接入教程》；

如果您想参查看官方的Sample，进而了解具体的用法，请点击这里查看《Sample》;

如果您在接入ArgusAPM的过程中遇到问题，请点击这里阅读《FAQ》，也可加入我们官方的QQ群，进行咨询。