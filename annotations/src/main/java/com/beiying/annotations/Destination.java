package com.beiying.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface Destination {
    /**
     * 页面在路由中的名称
     */
    String pageUrl();

    /**
     * 是否作为路由中的第一个启动页面
     */
    boolean asStarter() default false;
}
