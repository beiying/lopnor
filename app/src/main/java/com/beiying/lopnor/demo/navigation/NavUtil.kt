package com.beiying.lopnor.demo.navigation

import android.content.ComponentName
import android.content.Context
import android.content.res.AssetManager
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.*
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.FragmentNavigator
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.TypeReference
import com.beiying.lopnor.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object NavUtil {

    fun parseJsonFile(context: Context, fileName: String): String? {
        val assetManager: AssetManager = context.assets
        try {
            val inputStream: InputStream = assetManager.open(fileName)
            val bufferedReader: BufferedReader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = null
            var sb: StringBuilder = StringBuilder()
            line = bufferedReader.readLine()
            while (line != null) {
                sb.append(line)
                line = bufferedReader.readLine()
            }
            inputStream.close()
            bufferedReader.close()
            return sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
    fun getDestination(context: Context): HashMap<String, ByNavDestination>? {
        return JSON.parseObject(parseJsonFile(context, "destination.json"), object: TypeReference<HashMap<String, ByNavDestination>>(){}.type)
    }

    fun buildBottomTab(navView: BottomNavigationView) {
        val content: String? = parseJsonFile(navView.context, "main_tabs_config.json")
        val bottomBar: BottomBar = JSON.parseObject(content, BottomBar::class.java)
        val destinations: HashMap<String, ByNavDestination>? = getDestination(navView.context)
        val menu: Menu = navView.menu
        for (tab in bottomBar.tabs) {
            if (!tab.enable) continue
            val destination: ByNavDestination? = destinations?.get(tab.pageUrl)
            destination?.let {
                val menuItem: MenuItem = menu.add(0, it.id, tab.index, tab.title)
                menuItem.setIcon(R.drawable.ic_home_black_24dp)
            }
        }
    }

    fun buildNavGraph(activity: FragmentActivity, childFragmentManager: FragmentManager, navController: NavController, containerId: Int) {
        val datas: HashMap<String, ByNavDestination>? = getDestination(activity)
        datas?.let {destinations ->
            val iterator: Iterator<ByNavDestination> = destinations.values.iterator()
            val navigatorProvider: NavigatorProvider = navController.navigatorProvider
            val byFragmentNavigator: ByFragmentNavigator = ByFragmentNavigator(activity, childFragmentManager, containerId);
            navigatorProvider.addNavigator(byFragmentNavigator);
            val navGraphNavigator: NavGraphNavigator = navigatorProvider.getNavigator(NavGraphNavigator::class.java)
            var navGraph: NavGraph = NavGraph(navGraphNavigator)
            while(iterator.hasNext()) {
                val navDestination: ByNavDestination = iterator.next()
                when(navDestination.destType) {
                    "activity" -> {
                        val navigator: ActivityNavigator = navigatorProvider.getNavigator(ActivityNavigator::class.java)
                        val node: ActivityNavigator.Destination = navigator.createDestination()
                        node.id = navDestination.id
                        node.setComponentName(ComponentName(activity.packageName, navDestination.clazzName))
                        navGraph.addDestination(node)
                    }
                    "fragment" -> {
                        val navigator: ByFragmentNavigator = navigatorProvider.getNavigator(ByFragmentNavigator::class.java)
                        val node: ByFragmentNavigator.Destination = navigator.createDestination()
                        node.id = navDestination.id
                        node.className = navDestination.clazzName
                        navGraph.addDestination(node)
                    }
                    "dialog" -> {
                        val navigator: DialogFragmentNavigator = navigatorProvider.getNavigator(DialogFragmentNavigator::class.java)
                        val node: DialogFragmentNavigator.Destination = navigator.createDestination()
                        node.id = navDestination.id
                        node.className = navDestination.clazzName
                        navGraph.addDestination(node)
                    }
                }
                if (navDestination.asStarter) {
                    navGraph.startDestination = navDestination.id
                }
            }
            navController.graph = navGraph

        }
    }
}