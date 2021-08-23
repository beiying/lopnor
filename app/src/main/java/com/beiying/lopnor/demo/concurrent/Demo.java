package com.beiying.lopnor.demo.concurrent;

import org.jetbrains.annotations.NotNull;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;

class Demo {
    public static void main(String[] args) {
        CoroutinesDemo coroutinesDemo = new CoroutinesDemo();
        coroutinesDemo.request1(new Continuation<String>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return null;
            }

            @Override
            public void resumeWith(@NotNull Object o) {

            }
        });
    }
}
