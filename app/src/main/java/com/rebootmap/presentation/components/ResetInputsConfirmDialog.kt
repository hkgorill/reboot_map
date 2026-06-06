package com.rebootmap.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ResetInputsConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "입력 정보 초기화",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = "저장된 모든 입력값이 삭제되고 온보딩 화면으로 돌아갑니다.\n이 작업은 되돌릴 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("초기화")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}
