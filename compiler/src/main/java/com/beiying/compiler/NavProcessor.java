package com.beiying.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.beiying.annotations.Destination;
import com.google.auto.service.AutoService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.beiying.annotations.Destination"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class NavProcessor extends AbstractProcessor {
    private static final String PAGE_TYPE_ACTIVITY = "activity";
    private static final String PAGE_TYPE_FRAGMENT = "fragment";
    private static final String PAGE_TYPE_DIALOG = "dialog";
    private static final String OUTPUT_FILE_NAME = "destination.json";
    private Messager messager;
    private Filer filer;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE, "navprocessor init...");
        filer = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        messager.printMessage(Diagnostic.Kind.NOTE, "navprocessor processing");
        Iterator iterator = annotations.iterator();
        while(iterator.hasNext()) {
            TypeElement element = (TypeElement) iterator.next();
            messager.printMessage(Diagnostic.Kind.NOTE, "navprocessor processing element:" + element.getQualifiedName());
        }
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Destination.class);
        if (!elements.isEmpty()) {
            HashMap<String, JSONObject> destMap = new HashMap<>();
            handleDestination(elements, Destination.class, destMap);

            try {
                FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", OUTPUT_FILE_NAME);
                String resourcePath = fileObject.toUri().getPath();
                String appPath = resourcePath.substring(0, resourcePath.indexOf("app") + 4);
                String assetsPath = appPath  + "/src/main/assets";
                File file = new File(assetsPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String content = JSON.toJSONString(destMap);
                File outputFile = new File(assetsPath, OUTPUT_FILE_NAME);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                outputFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(outputFile);
                OutputStreamWriter writer = new OutputStreamWriter(fos);
                writer.write(content);
                writer.flush();

                fos.close();
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void handleDestination(Set<? extends Element> elements, Class<Destination> annotationClazz, HashMap<String, JSONObject> destMap) {
        for (Element element : elements) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                String elementName = typeElement.getQualifiedName().toString();

                Destination destination = typeElement.getAnnotation(annotationClazz);
                String pageUrl = destination.pageUrl();
                boolean asStarter = destination.asStarter();
                int id = Math.abs(elementName.hashCode());
                String destType = getDestinationType(typeElement);

                if (destMap.containsKey(pageUrl)) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "不同的页面不允许使用相同的pageUrl:" + pageUrl);
                } else {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("clazzName", elementName);
                    jsonObject.put("pageUrl", pageUrl);
                    jsonObject.put("asStarter", asStarter);
                    jsonObject.put("id", id);
                    jsonObject.put("destType", destType);

                    destMap.put(pageUrl, jsonObject);
                }
            }
        }
    }

    private String getDestinationType(TypeElement typeElement) {
        TypeMirror typeMirror = typeElement.getSuperclass();
        String superClazzName = typeMirror.toString();
        if (superClazzName.contains(PAGE_TYPE_ACTIVITY.toLowerCase())) {
            return PAGE_TYPE_ACTIVITY;
        } else if (superClazzName.contains(PAGE_TYPE_FRAGMENT.toLowerCase())) {
            return PAGE_TYPE_FRAGMENT;
        } else if (superClazzName.contains(PAGE_TYPE_DIALOG.toLowerCase())) {
            return PAGE_TYPE_DIALOG;
        }

        if (typeMirror instanceof DeclaredType) {
            Element element = ((DeclaredType) typeMirror).asElement();
            if (element instanceof TypeElement) {
                return getDestinationType((TypeElement) element);
            }
        }
        return "";
    }
}

