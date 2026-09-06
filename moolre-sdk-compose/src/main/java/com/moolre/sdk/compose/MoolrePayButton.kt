package com.moolre.sdk.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moolre.sdk.MoolrePaymentResult
import com.moolre.sdk.model.MoolreConfig
import com.moolre.sdk.model.MoolrePaymentRequest
import java.math.RoundingMode

/**
 * Branded Compose payment button backed by the shared Moolre checkout flow.
 */
@Composable
fun MoolrePayButton(
    config: MoolreConfig,
    payment: MoolrePaymentRequest,
    modifier: Modifier = Modifier,
    text: String = "Pay with Moolre",
    enabled: Boolean = true,
    showAmount: Boolean = false,
    containerColor: Color = MoolreBrandColors.buttonBackground,
    contentColor: Color = MoolreBrandColors.buttonContent,
    onResult: (MoolrePaymentResult) -> Unit = {},
    borderColor: Color = MoolreBrandColors.orange,
    borderWidth: Dp = 1.dp,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    val launcher = rememberMoolrePaymentLauncher(config, onResult)
    val label = if (showAmount) {
        "$text • ${payment.currency} ${payment.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()}"
    } else {
        text
    }

    Button(
        onClick = { launcher.launch(payment) },
        enabled = enabled && !launcher.isProcessing,
        modifier = modifier.heightIn(min = 48.dp),
        shape = shape,
        border = BorderStroke(borderWidth, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MoolreBrandColors.buttonDisabledBackground,
            disabledContentColor = MoolreBrandColors.buttonDisabledContent
        )
    ) {
        if (launcher.isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(if (launcher.isProcessing) "Processing payment" else label)
    }
}
