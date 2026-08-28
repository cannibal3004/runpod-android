package com.canni.runpod.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.canni.runpod.data.auth.ApiKeyStore
import com.canni.runpod.ui.billing.BillingScreen
import com.canni.runpod.ui.create.CreatePodScreen
import com.canni.runpod.ui.logs.LogsScreen
import com.canni.runpod.ui.pod.PodDetailScreen
import com.canni.runpod.ui.pods.PodsScreen
import com.canni.runpod.ui.secrets.SecretsScreen
import com.canni.runpod.ui.serverless.EndpointFormScreen
import com.canni.runpod.ui.serverless.ServerlessDetailScreen
import com.canni.runpod.ui.serverless.ServerlessScreen
import com.canni.runpod.ui.settings.SettingsScreen
import com.canni.runpod.ui.setup.ApiKeyScreen
import com.canni.runpod.ui.storage.StorageScreen

object Routes {
    const val SETUP = "setup"
    const val SETUP_CHANGE = "setup/change"
    const val PODS = "pods"
    const val POD_DETAIL = "pod/{podId}"
    const val CREATE = "create"
    const val LOGS = "logs/{podId}"
    const val SERVERLESS = "serverless"
    const val SERVERLESS_DETAIL = "serverless/{endpointId}"
    const val SERVERLESS_CREATE = "serverless/create"
    const val SERVERLESS_EDIT = "serverless/{endpointId}/edit"
    const val SERVERLESS_WORKER_LOGS = "serverless/{endpointId}/logs/{workerId}"
    const val BILLING = "billing"
    const val STORAGE = "storage"
    const val SECRETS = "secrets"
    const val SETTINGS = "settings"

    fun podDetail(id: String) = "pod/$id"
    fun logs(id: String) = "logs/$id"
    fun serverlessDetail(id: String) = "serverless/$id"
    fun serverlessWorkerLogs(endpointId: String, workerId: String) = "serverless/$endpointId/logs/$workerId"
}

@Composable
fun AppNav(
    navController: NavHostController,
    keyStore: ApiKeyStore,
) {
    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.PODS) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (keyStore.hasKey) Routes.PODS else Routes.SETUP,
    ) {
        composable(Routes.SETUP) {
            ApiKeyScreen(
                onConnected = {
                    navController.navigate(Routes.PODS) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SETUP_CHANGE) {
            ApiKeyScreen(
                changed = true,
                onBack = { navController.popBackStack() },
                onConnected = { navController.popBackStack() },
            )
        }
        composable(Routes.PODS) {
            PodsScreen(
                onPodClick = { id -> navController.navigate(Routes.podDetail(id)) },
                onCreate = { navController.navigate(Routes.CREATE) },
                onNavigateTopLevel = navigateTopLevel,
            )
        }
        composable(Routes.CREATE) {
            CreatePodScreen(
                onBack = { navController.popBackStack() },
                onCreated = { podId ->
                    navController.navigate(Routes.podDetail(podId)) {
                        popUpTo(Routes.CREATE) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.POD_DETAIL,
            arguments = listOf(navArgument("podId") { type = NavType.StringType }),
        ) { entry ->
            val podId = entry.arguments?.getString("podId").orEmpty()
            PodDetailScreen(
                podId = podId,
                onBack = { navController.popBackStack() },
                onOpenLogs = { navController.navigate(Routes.logs(podId)) },
            )
        }
        composable(
            route = Routes.LOGS,
            arguments = listOf(navArgument("podId") { type = NavType.StringType }),
        ) {
            LogsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SERVERLESS) {
            ServerlessScreen(
                onEndpointClick = { id -> navController.navigate(Routes.serverlessDetail(id)) },
                onCreate = { navController.navigate(Routes.SERVERLESS_CREATE) },
                onNavigateTopLevel = navigateTopLevel,
            )
        }
        composable(Routes.SERVERLESS_CREATE) {
            EndpointFormScreen(
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.navigate(Routes.serverlessDetail(id)) {
                        popUpTo(Routes.SERVERLESS_CREATE) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.SERVERLESS_DETAIL,
            arguments = listOf(navArgument("endpointId") { type = NavType.StringType }),
        ) { entry ->
            val endpointId = entry.arguments?.getString("endpointId").orEmpty()
            ServerlessDetailScreen(
                endpointId = endpointId,
                onBack = { navController.popBackStack() },
                onEdit = {
                    navController.navigate("serverless/$endpointId/edit")
                },
                onOpenWorkerLogs = { workerId ->
                    navController.navigate(Routes.serverlessWorkerLogs(endpointId, workerId))
                },
            )
        }
        composable(
            route = Routes.SERVERLESS_EDIT,
            arguments = listOf(navArgument("endpointId") { type = NavType.StringType }),
        ) {
            EndpointFormScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.SERVERLESS_WORKER_LOGS,
            arguments = listOf(
                navArgument("endpointId") { type = NavType.StringType },
                navArgument("workerId") { type = NavType.StringType },
            ),
        ) {
            LogsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BILLING) {
            BillingScreen(onNavigateTopLevel = navigateTopLevel)
        }
        composable(Routes.STORAGE) {
            StorageScreen(onNavigateTopLevel = navigateTopLevel)
        }
        composable(Routes.SECRETS) {
            SecretsScreen(onNavigateTopLevel = navigateTopLevel)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onChangeKey = { navController.navigate(Routes.SETUP_CHANGE) },
                onRemoveKey = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateTopLevel = navigateTopLevel,
            )
        }
    }
}
