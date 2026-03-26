package com.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.expensetracker.ui.screen.batchdetail.BatchDetailScreen
import com.expensetracker.ui.screen.dashboard.DashboardScreen
import com.expensetracker.ui.screen.expense.AddEditExpenseScreen
import com.expensetracker.ui.screen.mileage.AddEditMileageScreen
import com.expensetracker.ui.screen.review.ReviewSubmitScreen
import com.expensetracker.ui.screen.settings.ControlPanelScreen
import com.expensetracker.ui.screen.signature.SignatureCaptureScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Dashboard) {
        composable<Dashboard> {
            DashboardScreen(
                onBatchClick = { batchId ->
                    navController.navigate(BatchDetail(batchId))
                },
                onSettingsClick = {
                    navController.navigate(ControlPanel)
                }
            )
        }

        composable<BatchDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<BatchDetail>()
            BatchDetailScreen(
                batchId = route.batchId,
                onBack = { navController.popBackStack() },
                onAddExpense = { batchId ->
                    navController.navigate(AddEditExpense(batchId))
                },
                onEditExpense = { batchId, expenseId ->
                    navController.navigate(AddEditExpense(batchId, expenseId))
                },
                onAddMileage = { batchId ->
                    navController.navigate(AddEditMileage(batchId))
                },
                onEditMileage = { batchId, mileageId ->
                    navController.navigate(AddEditMileage(batchId, mileageId))
                },
                onReviewSubmit = { batchId ->
                    navController.navigate(ReviewSubmit(batchId))
                }
            )
        }

        composable<AddEditExpense> { backStackEntry ->
            val route = backStackEntry.toRoute<AddEditExpense>()
            AddEditExpenseScreen(
                batchId = route.batchId,
                expenseId = if (route.expenseId == -1L) null else route.expenseId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AddEditMileage> { backStackEntry ->
            val route = backStackEntry.toRoute<AddEditMileage>()
            AddEditMileageScreen(
                batchId = route.batchId,
                mileageId = if (route.mileageId == -1L) null else route.mileageId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<ReviewSubmit> { backStackEntry ->
            val route = backStackEntry.toRoute<ReviewSubmit>()
            ReviewSubmitScreen(
                batchId = route.batchId,
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.popBackStack(Dashboard, inclusive = false)
                }
            )
        }

        composable<ControlPanel> {
            ControlPanelScreen(
                onBack = { navController.popBackStack() },
                onCaptureSignature = {
                    navController.navigate(SignatureCapture)
                }
            )
        }

        composable<SignatureCapture> {
            SignatureCaptureScreen(
                onBack = { navController.popBackStack() },
                onSignatureSaved = {
                    navController.popBackStack()
                }
            )
        }
    }
}
