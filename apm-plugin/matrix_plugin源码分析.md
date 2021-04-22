matrix gradle plugin的源码分析
1. MatrixPlugin
MatrixPlugin 只有apply一个方法，该方法做了 创建Extension 和 注册 Task两件事
    void apply(Project project) {
        //创建 extension
        project.extensions.create("matrix", MatrixExtension)
        project.matrix.extensions.create("trace", MatrixTraceExtension)
        project.matrix.extensions.create("removeUnusedResources", MatrixDelUnusedResConfiguration)
        ....
        project.afterEvaluate {
          ....
            android.applicationVariants.all { variant ->

                if (configuration.trace.enable) {
                    //注入MatrixTraceTransform 【见2.1】
                    com.tencent.matrix.trace.transform.MatrixTraceTransform.inject(project, configuration.trace, variant.getVariantData().getScope())
                }

                //移除无用资源 可用 【见7.1】
                if (configuration.removeUnusedResources.enable) {
                    if (Util.isNullOrNil(configuration.removeUnusedResources.variant) || variant.name.equalsIgnoreCase(configuration.removeUnusedResources.variant)) {
                        Log.i(TAG, "removeUnusedResources %s", configuration.removeUnusedResources)
                        RemoveUnusedResourcesTask removeUnusedResourcesTask = project.tasks.create("remove" + variant.name.capitalize() + "UnusedResources", RemoveUnusedResourcesTask)
                        removeUnusedResourcesTask.inputs.property(RemoveUnusedResourcesTask.BUILD_VARIANT, variant.name)
                        project.tasks.add(removeUnusedResourcesTask)
                        removeUnusedResourcesTask.dependsOn variant.packageApplication
                        variant.assemble.dependsOn removeUnusedResourcesTask
                    }
                }

            }
        }
    }
2. MatrixTraceTransform
MatrixTraceTransform 继承了 Transform， 该类中hook了 系统构建Dex 的 Transform 并配合 ASM 框架 ，插入方法执行时间记录的字节码，这一系列内容将是本文的重点。
2.1 MatrixTraceTransform.inject
   public static void inject(Project project, MatrixTraceExtension extension, VariantScope variantScope) {
        ...
        //收集配置信息
        Configuration config = new Configuration.Builder()
                .setPackageName(variant.getApplicationId())//包名
                .setBaseMethodMap(extension.getBaseMethodMapFile())//build.gradle 中配置的 baseMethodMapFile ,保存的是 我们指定需要被 插桩的方法
                .setBlackListFile(extension.getBlackListFile())//build.gradle 中配置的 blackListFile ，保存的是 不需要插桩的文件
                .setMethodMapFilePath(mappingOut + "/methodMapping.txt")// 记录插桩 methodId 和 method的 关系
                .setIgnoreMethodMapFilePath(mappingOut + "/ignoreMethodMapping.txt")// 记录 没有被 插桩的方法
                .setMappingPath(mappingOut) //mapping文件存储目录
                .setTraceClassOut(traceClassOut)//插桩后的 class存储目录
                .build();

        try {
            // 获取 TransformTask.. 具体名称 如：transformClassesWithDexBuilderForDebug 和 transformClassesWithDexForDebug
            // 具体是哪一个 应该和 gradle的版本有关
            // 在该 task之前  proguard 操作 已经完成
            String[] hardTask = getTransformTaskName(extension.getCustomDexTransformName(), variant.getName());
            for (Task task : project.getTasks()) {
                for (String str : hardTask) {
                    // 找到 task 并进行 hook
                    if (task.getName().equalsIgnoreCase(str) && task instanceof TransformTask) {
                        TransformTask transformTask = (TransformTask) task;
                        Log.i(TAG, "successfully inject task:" + transformTask.getName());
                        Field field = TransformTask.class.getDeclaredField("transform");
                        field.setAccessible(true);
                        // 将 系统的  "transformClassesWithDexBuilderFor.."和"transformClassesWithDexFor.."
                        // 中的 transform 替换为 MatrixTraceTransform(也就是当前类) 【见2.2】
                        field.set(task, new MatrixTraceTransform(config, transformTask.getTransform()));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

    }
复制代码2.2 MatrixTraceTransform 构造方法
MatrixTraceTransform 中保存了，被hook的 Transform ，因为需要执行完  MatrixTraceTransform 的内容后，再恢复原流程。
    public MatrixTraceTransform(Configuration config, Transform origTransform) {
        //配置
        this.config = config;
        //原始Transform 也就是被 hook的 Transform
        this.origTransform = origTransform;
    }
复制代码3. MatrixTraceTransform.transform
上面将的MatrixTraceTransform.inject()发生在gradle 的评估期 ，也就是说在评估期已经确认了 整个 gradle Task的执行顺序，在运行期的话 gradle 会回调 Transform 的 transform方法，下面我么一起来看看。
    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
       ...
        try {
             //  【见3.1】
            doTransform(transformInvocation);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        ...
        //执行原来应该执行的 Transform 的 transform 方法
        origTransform.transform(transformInvocation);
        ...

    }
复制代码3.1 MatrixTraceTransform.doTransform
doTransform 方法的功能可被分为三步

解析mapping 文件记录 混淆前后方法的对应关系 并 替换文件目录
收集需要插桩和不需要插桩的方法记录在 mapping文件中 并 收集类之间的继承关系
进行字节码插桩

    private void doTransform(TransformInvocation transformInvocation) throws ExecutionException, InterruptedException {
        //是否增量编译
        final boolean isIncremental = transformInvocation.isIncremental() && this.isIncremental();

         /**
         * step 1
         * 1. 解析mapping 文件混淆后方法对应关系
         * 2. 替换文件目录
         */
        long start = System.currentTimeMillis();

        List<Future> futures = new LinkedList<>();

        // 存储 混淆前方法、混淆后方法的映射关系
        final MappingCollector mappingCollector = new MappingCollector();
        // methodId 计数器
        final AtomicInteger methodId = new AtomicInteger(0);
        // 存储 需要插桩的 方法名 和 方法的封装对象TraceMethod
        final ConcurrentHashMap<String, TraceMethod> collectedMethodMap = new ConcurrentHashMap<>();

        // 将 ParseMappingTask 放入线程池
        futures.add(executor.submit(new ParseMappingTask(mappingCollector, collectedMethodMap, methodId)));

        //存放原始 源文件 和 输出 源文件的 对应关系
        Map<File, File> dirInputOutMap = new ConcurrentHashMap<>();

        //存放原始jar文件和 输出jar文件 对应关系
        Map<File, File> jarInputOutMap = new ConcurrentHashMap<>();
        Collection<TransformInput> inputs = transformInvocation.getInputs();

        for (TransformInput input : inputs) {

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                 //【见4.1】
                futures.add(executor.submit(new CollectDirectoryInputTask(dirInputOutMap, directoryInput, isIncremental)));
            }

            for (JarInput inputJar : input.getJarInputs()) {
                 //【见4.3】
                futures.add(executor.submit(new CollectJarInputTask(inputJar, isIncremental, jarInputOutMap, dirInputOutMap)));
            }
        }

        for (Future future : futures) {
            // 等待所有线程 运行完毕
            future.get();
        }
        //清空任务
        futures.clear();

        Log.i(TAG, "[doTransform] Step(1)[Parse]... cost:%sms", System.currentTimeMillis() - start);


        /**
         * step 2
         * 1. 收集需要插桩和不需要插桩的方法，并记录在 mapping文件中
         * 2. 收集类之间的继承关系
         */
        start = System.currentTimeMillis();
        //收集需要插桩的方法信息，每个插桩信息封装成TraceMethod对象
        MethodCollector methodCollector = new MethodCollector(executor, mappingCollector, methodId, config, collectedMethodMap);
          //【见5.1】
        methodCollector.collect(dirInputOutMap.keySet(), jarInputOutMap.keySet());
        Log.i(TAG, "[doTransform] Step(2)[Collection]... cost:%sms", System.currentTimeMillis() - start);

        /**
         * step 3 插桩字节码
         */
        start = System.currentTimeMillis();
        //执行插桩逻辑，在需要插桩方法的入口、出口添加MethodBeat的i/o逻辑
        MethodTracer methodTracer = new MethodTracer(executor, mappingCollector, config, methodCollector.getCollectedMethodMap(), methodCollector.getCollectedClassExtendMap());
          //【见6.1】
        methodTracer.trace(dirInputOutMap, jarInputOutMap);
        Log.i(TAG, "[doTransform] Step(3)[Trace]... cost:%sms", System.currentTimeMillis() - start);

    }
复制代码4. CollectDirectoryInputTask
4.1 CollectDirectoryInputTask.run
public void run() {
            try {
                //【见4.2】
                handle();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Matrix." + getName(), "%s", e.toString());
            }
        }
复制代码4.2 CollectDirectoryInputTask.handle
该方法会通过反射 修改 输入文件的 属性，在增量编译模式下会修改 file 和
changedFiles两个属性 ，在全量编译模式下 只会修改   file  这一个属性
 private void handle() throws IOException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
            //获取原始文件
            final File dirInput = directoryInput.getFile();
            //创建输出文件
            final File dirOutput = new File(traceClassOut, dirInput.getName());
            final String inputFullPath = dirInput.getAbsolutePath();
            final String outputFullPath = dirOutput.getAbsolutePath();
             ....
            if (isIncremental) {//增量更新，只 操作有改动的文件
                Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();

                //保存输出文件和其状态的 map
                final Map<File, Status> outChangedFiles = new HashMap<>();

                for (Map.Entry<File, Status> entry : fileStatusMap.entrySet()) {
                    final Status status = entry.getValue();
                    final File changedFileInput = entry.getKey();

                    final String changedFileInputFullPath = changedFileInput.getAbsolutePath();
                    //增量编译模式下之前的build输出已经重定向到dirOutput；替换成output的目录
                    final File changedFileOutput = new File(changedFileInputFullPath.replace(inputFullPath, outputFullPath));

                    if (status == Status.ADDED || status == Status.CHANGED) {
                        //新增、修改的Class文件，此次需要扫描
                        dirInputOutMap.put(changedFileInput, changedFileOutput);
                    } else if (status == Status.REMOVED) {
                        //删除的Class文件，将文件直接删除
                        changedFileOutput.delete();
                    }
                    outChangedFiles.put(changedFileOutput, status);
                }

                //使用反射 替换directoryInput的  改动文件目录
                replaceChangedFile(directoryInput, outChangedFiles);

            } else {
                //全量编译模式下，所有的Class文件都需要扫描
                dirInputOutMap.put(dirInput, dirOutput);
            }
            //反射input，将dirOutput设置为其输出目录
            replaceFile(directoryInput, dirOutput);
        }
复制代码4.3 CollectJarInputTask.run
CollectJarInputTask的工作和 CollectDirectoryInputTask基本上是一样的，只不过操作目标从文件夹换成了 jar
        @Override
        public void run() {
            try {
                【见4.4】
                handle();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Matrix." + getName(), "%s", e.toString());
            }
        }
复制代码4.4 CollectJarInputTask.handle
 private void handle() throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException, IOException {
            // traceClassOut 文件夹地址
            String traceClassOut = config.traceClassOut;

            final File jarInput = inputJar.getFile();
            //创建唯一的 文件
            final File jarOutput = new File(traceClassOut, getUniqueJarName(jarInput));
            ....

            if (IOUtil.isRealZipOrJar(jarInput)) {
                if (isIncremental) {//是增量
                    if (inputJar.getStatus() == Status.ADDED || inputJar.getStatus() == Status.CHANGED) {
                        //存放到 jarInputOutMap 中
                        jarInputOutMap.put(jarInput, jarOutput);
                    } else if (inputJar.getStatus() == Status.REMOVED) {
                        jarOutput.delete();
                    }

                } else {
                    //存放到 jarInputOutMap 中
                    jarInputOutMap.put(jarInput, jarOutput);
                }

            } else {// 专门用于 处理 WeChat AutoDex.jar 文件 可以略过，意义不大
               ....
            }

            //将 inputJar 的 file 属性替换为 jarOutput
            replaceFile(inputJar, jarOutput);

        }
复制代码5. MethodCollector
5.1 MethodCollector.collect
    //存储 类->父类 的map（用于查找Activity的子类）
    private final ConcurrentHashMap<String, String> collectedClassExtendMap = new ConcurrentHashMap<>();
    //存储 被忽略方法名 -> 该方法TraceMethod 的映射关系
    private final ConcurrentHashMap<String, TraceMethod> collectedIgnoreMethodMap = new ConcurrentHashMap<>();
    //存储 需要插桩方法名 -> 该方法TraceMethod 的映射关系
    private final ConcurrentHashMap<String, TraceMethod> collectedMethodMap;
    private final Configuration configuration;
    private final AtomicInteger methodId;
    // 被忽略方法计数器
    private final AtomicInteger ignoreCount = new AtomicInteger();
    //需要插桩方法 计数器
    private final AtomicInteger incrementCount = new AtomicInteger();
....

 /**
     *
     * @param srcFolderList 原始文件集合
     * @param dependencyJarList 原始 jar 集合
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void collect(Set<File> srcFolderList, Set<File> dependencyJarList) throws ExecutionException, InterruptedException {
        List<Future> futures = new LinkedList<>();

        for (File srcFile : srcFolderList) {
            //将所有源文件添加到 classFileList 中
            ArrayList<File> classFileList = new ArrayList<>();
            if (srcFile.isDirectory()) {
                listClassFiles(classFileList, srcFile);
            } else {
                classFileList.add(srcFile);
            }

            //这里应该是个bug，这个for 应该防止撒谎给你吗那个for 的外面
            for (File classFile : classFileList) {
                // 每个源文件执行 CollectSrcTask  【见5.2】
                futures.add(executor.submit(new CollectSrcTask(classFile)));
            }
        }

        for (File jarFile : dependencyJarList) {
            // 每个jar 源文件执行 CollectJarTask  【见5.5】
            futures.add(executor.submit(new CollectJarTask(jarFile)));
        }

        for (Future future : futures) {
            future.get();
        }
        futures.clear();

        futures.add(executor.submit(new Runnable() {
            @Override
            public void run() {
                //存储不需要插桩的方法信息到文件（包括黑名单中的方法） 【见5.6】
                saveIgnoreCollectedMethod(mappingCollector);
            }
        }));

        futures.add(executor.submit(new Runnable() {
            @Override
            public void run() {
                //存储待插桩的方法信息到文件  【见5.7】
                saveCollectedMethod(mappingCollector);
            }
        }));

        for (Future future : futures) {
            future.get();
        }
        futures.clear();

    }
复制代码5.2 CollectSrcTask.run
        public void run() {
            InputStream is = null;
            try {
                is = new FileInputStream(classFile);
                ClassReader classReader = new ClassReader(is);
                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                //收集Method信息  【见5.3】
                ClassVisitor visitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
                classReader.accept(visitor, 0);

            } catch (Exception e) {
            ...
        }
复制代码5.3 TraceClassAdapter.visit
到 TraceClassAdapter 类的时候就到了 ASM 框架 发挥作用的时候了，ASM 在扫描类的时候 会依次回调 visit 和 visitMethod 方法
    private class TraceClassAdapter extends ClassVisitor {
         ....
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
            //如果是虚拟类或者接口 isABSClass =true
            if ((access & Opcodes.ACC_ABSTRACT) > 0 || (access & Opcodes.ACC_INTERFACE) > 0) {
                this.isABSClass = true;
            }
            //存到 collectedClassExtendMap 中
            collectedClassExtendMap.put(className, superName);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            if (isABSClass) {//如果是虚拟类或者接口 就不管
                return super.visitMethod(access, name, desc, signature, exceptions);
            } else {
                if (!hasWindowFocusMethod) {
                    //该方法是否与onWindowFocusChange方法的签名一致
                    // （该类中是否复写了onWindowFocusChange方法，Activity不用考虑Class混淆)
                    hasWindowFocusMethod = isWindowFocusChangeMethod(name, desc);
                }
                //CollectMethodNode中执行method收集操作 【见5.4】
                return new CollectMethodNode(className, access, name, desc, signature, exceptions);
            }
        }
    }
复制代码5.4 CollectMethodNode
CollectMethodNode继承了MethodNode ，ASM框架在扫描方法的时候会回调 MethodNode  中的 visitEnd 方法
private class CollectMethodNode extends MethodNode {
        ....
        @Override
        public void visitEnd() {
            super.visitEnd();
            //创建TraceMethod
            TraceMethod traceMethod = TraceMethod.create(0, access, className, name, desc);

            //如果是构造方法
            if ("<init>".equals(name)) {
                isConstructor = true;
            }

            //判断类是否 被配置在了 黑名单中
            boolean isNeedTrace = isNeedTrace(configuration, traceMethod.className, mappingCollector);
            //忽略空方法、get/set方法、没有局部变量的简单方法
            if ((isEmptyMethod() || isGetSetMethod() || isSingleMethod())
                    && isNeedTrace) {
                //忽略方法递增
                ignoreCount.incrementAndGet();
                //加入到被忽略方法 map
                collectedIgnoreMethodMap.put(traceMethod.getMethodName(), traceMethod);
                return;
            }

            //不在黑名单中而且没在在methodMapping中配置过的方法加入待插桩的集合；
            if (isNeedTrace && !collectedMethodMap.containsKey(traceMethod.getMethodName())) {
                traceMethod.id = methodId.incrementAndGet();
                collectedMethodMap.put(traceMethod.getMethodName(), traceMethod);
                incrementCount.incrementAndGet();
            } else if (!isNeedTrace && !collectedIgnoreMethodMap.containsKey(traceMethod.className)) {//在黑名单中而且没在在methodMapping中配置过的方法加入ignore插桩的集合
                ignoreCount.incrementAndGet();
                collectedIgnoreMethodMap.put(traceMethod.getMethodName(), traceMethod);
            }

        }
.....
}
复制代码5.5 CollectJarTask.run
CollectJarTask 和 CollectSrcTask 一样都会 调用到 TraceClassAdapter进行方法的扫描
    public void run() {
            ZipFile zipFile = null;

            try {
                zipFile = new ZipFile(fromJar);
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry zipEntry = enumeration.nextElement();
                    String zipEntryName = zipEntry.getName();
                    if (isNeedTraceFile(zipEntryName)) {//是需要被插桩的文件
                        InputStream inputStream = zipFile.getInputStream(zipEntry);
                        ClassReader classReader = new ClassReader(inputStream);
                        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        //进行扫描 【见5.3】
                        ClassVisitor visitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
                        classReader.accept(visitor, 0);
                    }
                }
            }
        ....
复制代码5.6 MethodCollector.saveIgnoreCollectedMethod
saveIgnoreCollectedMethod 方法很简单，就是将前面手机的 被忽略的方法内容写到 ignoreMethodMapping.txt 中
 /**
     * 将被忽略的 方法名 存入 ignoreMethodMapping.txt 中
     * @param mappingCollector
     */
    private void saveIgnoreCollectedMethod(MappingCollector mappingCollector) {

        //创建 ignoreMethodMapping.txt 文件对象
        File methodMapFile = new File(configuration.ignoreMethodMapFilePath);
        //如果他爸不存在就创建
        if (!methodMapFile.getParentFile().exists()) {
            methodMapFile.getParentFile().mkdirs();
        }
        List<TraceMethod> ignoreMethodList = new ArrayList<>();
        ignoreMethodList.addAll(collectedIgnoreMethodMap.values());
        Log.i(TAG, "[saveIgnoreCollectedMethod] size:%s path:%s", collectedIgnoreMethodMap.size(), methodMapFile.getAbsolutePath());

        //通过class名字进行排序
        Collections.sort(ignoreMethodList, new Comparator<TraceMethod>() {
            @Override
            public int compare(TraceMethod o1, TraceMethod o2) {
                return o1.className.compareTo(o2.className);
            }
        });

        PrintWriter pw = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(methodMapFile, false);
            Writer w = new OutputStreamWriter(fileOutputStream, "UTF-8");
            pw = new PrintWriter(w);
            pw.println("ignore methods:");
            for (TraceMethod traceMethod : ignoreMethodList) {
                //将 混淆过的数据 转换为 原始数据
                traceMethod.revert(mappingCollector);
                //输出忽略信息到 文件中
                pw.println(traceMethod.toIgnoreString());
            }
        } catch (Exception e) {
            Log.e(TAG, "write method map Exception:%s", e.getMessage());
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.flush();
                pw.close();
            }
        }
    }
复制代码5.7 MethodCollector.saveCollectedMethod
saveCollectedMethod是将需要插桩的方法写入  methodMapping.txt
 /**
     * 将被插桩的 方法名 存入 methodMapping.txt 中
     * @param mappingCollector
     */
    private void saveCollectedMethod(MappingCollector mappingCollector) {
        File methodMapFile = new File(configuration.methodMapFilePath);
        if (!methodMapFile.getParentFile().exists()) {
            methodMapFile.getParentFile().mkdirs();
        }
        List<TraceMethod> methodList = new ArrayList<>();

        //因为Android包下的 都不会被插装，但是我们需要 dispatchMessage 方法的执行时间
        //所以将这个例外 加进去
        TraceMethod extra = TraceMethod.create(TraceBuildConstants.METHOD_ID_DISPATCH, Opcodes.ACC_PUBLIC, "android.os.Handler",
                "dispatchMessage", "(Landroid.os.Message;)V");
        collectedMethodMap.put(extra.getMethodName(), extra);

        methodList.addAll(collectedMethodMap.values());

        Log.i(TAG, "[saveCollectedMethod] size:%s incrementCount:%s path:%s", collectedMethodMap.size(), incrementCount.get(), methodMapFile.getAbsolutePath());

        //通过ID 进行排序
        Collections.sort(methodList, new Comparator<TraceMethod>() {
            @Override
            public int compare(TraceMethod o1, TraceMethod o2) {
                return o1.id - o2.id;
            }
        });

        PrintWriter pw = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(methodMapFile, false);
            Writer w = new OutputStreamWriter(fileOutputStream, "UTF-8");
            pw = new PrintWriter(w);
            for (TraceMethod traceMethod : methodList) {
                traceMethod.revert(mappingCollector);
                pw.println(traceMethod.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "write method map Exception:%s", e.getMessage());
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.flush();
                pw.close();
            }
        }
    }
复制代码6. MethodTracer.trace
trace 方法就是真正开始插桩
    public void trace(Map<File, File> srcFolderList, Map<File, File> dependencyJarList) throws ExecutionException, InterruptedException {
        List<Future> futures = new LinkedList<>();
        //对源文件进行插桩 【见6.1】
        traceMethodFromSrc(srcFolderList, futures);
        //对jar进行插桩 【见6.5】
        traceMethodFromJar(dependencyJarList, futures);
        for (Future future : futures) {
            future.get();
        }
        futures.clear();
    }
复制代码6.1  MethodTracer.traceMethodFromSrc
    private void traceMethodFromSrc(Map<File, File> srcMap, List<Future> futures) {
        if (null != srcMap) {
            for (Map.Entry<File, File> entry : srcMap.entrySet()) {
                futures.add(executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        //【见6.2】
                        innerTraceMethodFromSrc(entry.getKey(), entry.getValue());
                    }
                }));
            }
        }
    }
复制代码6.2  MethodTracer.innerTraceMethodFromSrc
private void innerTraceMethodFromSrc(File input, File output) {
for (File classFile : classFileList) {
            InputStream is = null;
            FileOutputStream os = null;
            try {
                //原始文件全路径
                final String changedFileInputFullPath = classFile.getAbsolutePath();
                //插桩后文件
                final File changedFileOutput = new File(changedFileInputFullPath.replace(input.getAbsolutePath(), output.getAbsolutePath()));
                if (!changedFileOutput.exists()) {
                    changedFileOutput.getParentFile().mkdirs();
                }
                changedFileOutput.createNewFile();

                if (MethodCollector.isNeedTraceFile(classFile.getName())) {//需要插桩
                    is = new FileInputStream(classFile);
                    ClassReader classReader = new ClassReader(is);
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    // TraceClassAdapter 进行插桩 【见6.3】
                    ClassVisitor classVisitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                    is.close();

                    if (output.isDirectory()) {
                        os = new FileOutputStream(changedFileOutput);
                    } else {
                        os = new FileOutputStream(output);
                    }
                    //将修改后的内容写入到 插装后的文件中
                    os.write(classWriter.toByteArray());
                    os.close();
                } else {//不需要插桩，直接copy
                    FileUtil.copyFileUsingStream(classFile, changedFileOutput);
                }
            } catch (Exception e) {
     }
}
}
复制代码6.3 TraceClassAdapter
 private class TraceClassAdapter extends ClassVisitor {

        private String className;
        private boolean isABSClass = false;
        private boolean hasWindowFocusMethod = false;
        private boolean isActivityOrSubClass;
        private boolean isNeedTrace;

        TraceClassAdapter(int i, ClassVisitor classVisitor) {
            super(i, classVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
            //是否是 activity 或者其 子类
            this.isActivityOrSubClass = isActivityOrSubClass(className, collectedClassExtendMap);
            //是否需要被插桩
            this.isNeedTrace = MethodCollector.isNeedTrace(configuration, className, mappingCollector);
            //是否是抽象类、接口
            if ((access & Opcodes.ACC_ABSTRACT) > 0 || (access & Opcodes.ACC_INTERFACE) > 0) {
                this.isABSClass = true;
            }

        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {

            //抽象类、接口不插桩
            if (isABSClass) {
                return super.visitMethod(access, name, desc, signature, exceptions);
            } else {
                if (!hasWindowFocusMethod) {
                    //是否是onWindowFocusChange方法
                    hasWindowFocusMethod = MethodCollector.isWindowFocusChangeMethod(name, desc);
                }
                MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
                //【见6.4】
                return new TraceMethodAdapter(api, methodVisitor, access, name, desc, this.className,
                        hasWindowFocusMethod, isActivityOrSubClass, isNeedTrace);
            }
        }


        @Override
        public void visitEnd() {
            //如果Activity的子类没有onWindowFocusChange方法，插入一个onWindowFocusChange方法
            if (!hasWindowFocusMethod && isActivityOrSubClass && isNeedTrace) {
                insertWindowFocusChangeMethod(cv, className);
            }
            super.visitEnd();
        }
    }
复制代码6.4 TraceMethodAdapter
 private class TraceMethodAdapter extends AdviceAdapter {

       .....

        //函数入口处添加 AppMethodBeat.i（）方法
        @Override
        protected void onMethodEnter() {
            TraceMethod traceMethod = collectedMethodMap.get(methodName);
            if (traceMethod != null) {
                //traceMethodCount +1
                traceMethodCount.incrementAndGet();
                mv.visitLdcInsn(traceMethod.id);
                mv.visitMethodInsn(INVOKESTATIC, TraceBuildConstants.MATRIX_TRACE_CLASS, "i", "(I)V", false);
            }
        }



        //函数出口处添加 AppMethodBeat.O（）方法
        @Override
        protected void onMethodExit(int opcode) {
            TraceMethod traceMethod = collectedMethodMap.get(methodName);
            if (traceMethod != null) {
                //是 onWindowFocusChanged 方法 则在出口添加 AppMethodBeat.at()
                if (hasWindowFocusMethod && isActivityOrSubClass && isNeedTrace) {
                    TraceMethod windowFocusChangeMethod = TraceMethod.create(-1, Opcodes.ACC_PUBLIC, className,
                            TraceBuildConstants.MATRIX_TRACE_ON_WINDOW_FOCUS_METHOD, TraceBuildConstants.MATRIX_TRACE_ON_WINDOW_FOCUS_METHOD_ARGS);
                    if (windowFocusChangeMethod.equals(traceMethod)) {
                        traceWindowFocusChangeMethod(mv, className);
                    }
                }

                //traceMethodCount +1
                traceMethodCount.incrementAndGet();
                mv.visitLdcInsn(traceMethod.id);
                mv.visitMethodInsn(INVOKESTATIC, TraceBuildConstants.MATRIX_TRACE_CLASS, "o", "(I)V", false);
            }
        }
    }
复制代码6.5 MethodTracer.traceMethodFromJar
    private void traceMethodFromJar(Map<File, File> dependencyMap, List<Future> futures) {
        if (null != dependencyMap) {
            for (Map.Entry<File, File> entry : dependencyMap.entrySet()) {
                futures.add(executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        //【见6.6】
                        innerTraceMethodFromJar(entry.getKey(), entry.getValue());
                    }
                }));
            }
        }
    }
 6.6 MethodTracer.innerTraceMethodFromJar
 private void innerTraceMethodFromJar(File input, File output) {
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String zipEntryName = zipEntry.getName();
                if (MethodCollector.isNeedTraceFile(zipEntryName)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    ClassReader classReader = new ClassReader(inputStream);
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    // 【见6.3】
                    ClassVisitor classVisitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                    byte[] data = classWriter.toByteArray();
                    InputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    FileUtil.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream);
                } else {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    //直接copy jar 到插装过后的 存放区
                    FileUtil.addZipEntry(zipOutputStream, newZipEntry, inputStream);
                }
            }
}