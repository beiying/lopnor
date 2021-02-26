易用高扩展日志组件Log

日志的保存
日志的可视化
日志打印对象
日志在线收集：辅助在线问题定位

Log库疑难点分析与架构设计
需求分析：
能够打印堆栈信息
支持任何数据类型的打印
支持实现日志可视化
能够实现文件打印模块
支持不同打印器的插拔

日志工作流程：
日志收集：
日志对外门面（对外接口）
日志管理器
日志配置
日志初始化

技术点分析：面向接口编程、设计模式、解耦设计
日志加工
堆栈信息加工
线程信息加工
日志模型转换
日志序列化

技术点分析：堆栈信息相关技术、序列化、日志格式化
日志打印：
控制台打印
视图打印
文件打印

技术点分析：文件IO技术、多线程技术（线程复用、线程同步）、UI及列表相关技术

疑难点分析：
堆栈信息获取
打印器插拔设计
线程复用防止频繁的创建线程
线程同步：

架构设计：
ByLog：提供日志对外接口
ByLogManager：日志管理类
ByLogPrinter：日志打印器
ByFilePrinter
ByViewPrinter
ByViewPrinterProvider
ByConsolePrinter
ByLogConfig：可以配置序列化库
ByLogFormatter：日志格式化
ByStackTraceFormatter
ByThreadFormatter
ByStackTraceUtil：堆栈工具类
ByLogModel：日志Model
ByLogType：日志类型
JsonParser：日志序列化接口


Log库堆栈信息打印与日志格式化功能实现


基于LogPrinter实现日志可视化模块