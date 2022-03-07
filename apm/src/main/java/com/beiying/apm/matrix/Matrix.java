package com.beiying.apm.matrix;

import android.app.Application;

import com.beiying.apm.matrix.plugin.DefaultPluginListener;
import com.beiying.apm.matrix.plugin.Plugin;
import com.beiying.apm.matrix.plugin.PluginListener;

import java.util.HashSet;

public class Matrix {
    private static final String TAG = "Matrix.Matrix";
    private static volatile Matrix sInstance;
    private final HashSet<Plugin> plugins;
    private final Application application;
    private final PluginListener pluginListener;

    private Matrix(Application app, PluginListener listener, HashSet<Plugin> plugins) {
        this.application = app;
        this.pluginListener = listener;
        this.plugins = plugins;
        AppActiveMatrixDelegate.INSTANCE.init(application);
        for (Plugin plugin : plugins) {
            plugin.init(app, listener);
            pluginListener.onInit(plugin);
        }
    }

    public static void setLogIml(MatrixLog.MatrixLogImp imp) {
        MatrixLog.setMatrixLogImp(imp);
    }

    public static boolean isInstalled() {
        return sInstance != null;
    }

    public static Matrix init(Matrix matrix) {
        if (matrix == null) {
            throw new RuntimeException("Matrix init, Matrix should not be null.");
        }
        synchronized (Matrix.class) {
            if (sInstance == null) {
                sInstance = matrix;
            } else {
                MatrixLog.e(TAG, "Matrix instance is already set. this invoking will be ignored");
            }
        }
        return sInstance;
    }

    public static Matrix with() {
        if (sInstance == null) {
            throw new RuntimeException("you must init Matrix sdk first");
        }
        return sInstance;
    }

    public void startAllPlugins() {
        for (Plugin plugin : plugins) {
            plugin.start();
        }
    }

    public void stopAllPlugins() {
        for (Plugin plugin : plugins) {
            plugin.stop();
        }
    }

    public void destroyAllPlugins() {
        for (Plugin plugin : plugins) {
            plugin.destroy();
        }
    }

    public Application getApplication() {
        return application;
    }

    public HashSet<Plugin> getPlugins() {
        return plugins;
    }

    public Plugin getPluginByTag(String tag) {
        for (Plugin plugin : plugins) {
            if (plugin.getTag().equals(tag)) {
                return plugin;
            }
        }
        return null;
    }

    public <T extends Plugin> T getPluginByClass(Class<T> pluginClass) {
        String className = pluginClass.getName();
        for (Plugin plugin : plugins) {
            if (plugin.getClass().getName().equals(className)) {
                return (T) plugin;
            }
        }
        return null;
    }

    public static class Builder {
        private final Application application;
        private PluginListener pluginListener;

        private HashSet<Plugin> plugins = new HashSet<>();

        public Builder(Application app) {
            if (app == null) {
                throw new RuntimeException("matrix init, application is null");
            }
            this.application = app;
        }

        public Builder plugin(Plugin plugin) {
            String tag = plugin.getTag();
            for (Plugin exist : plugins) {
                if (tag.equals(exist.getTag())) {
                    throw new RuntimeException(String.format("plugin with tag %s is already exist", tag));
                }
            }
            plugins.add(plugin);
            return this;
        }

        public Builder pluginListener(PluginListener pluginListener) {
            this.pluginListener = pluginListener;
            return this;
        }

        public Matrix build() {
            if (pluginListener == null) {
                pluginListener = new DefaultPluginListener(application);
            }
            return new Matrix(application, pluginListener, plugins);
        }

    }
}
