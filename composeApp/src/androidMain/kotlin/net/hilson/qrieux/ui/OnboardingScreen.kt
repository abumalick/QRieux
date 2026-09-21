package net.hilson.qrieux.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.hilson.qrieux.R
import net.hilson.qrieux.ui.theme.QRieuxUiConfig
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import qr_scanner.composeapp.generated.resources.Res
import qr_scanner.composeapp.generated.resources.*

private data class OnboardingStep(
    val titleRes: Int,
    val descRes: Int,
    val imageRes: DrawableResource
)

private val steps = listOf(
    OnboardingStep(R.string.onboarding_step1_title, R.string.onboarding_step1_desc, Res.drawable.onboarding_qr_codes),
    OnboardingStep(R.string.onboarding_step2_title, R.string.onboarding_step2_desc, Res.drawable.onboarding_scan),
    OnboardingStep(R.string.onboarding_step3_title, R.string.onboarding_step3_desc, Res.drawable.onboarding_gallery),
    OnboardingStep(R.string.onboarding_step4_title, R.string.onboarding_step4_desc, Res.drawable.onboarding_create_qr),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()
    val isLastStep = pagerState.currentPage == steps.lastIndex
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                // The dots and the two buttons are the same size either way, so on a
                // landscape phone they eat most of the height. Spending less of it on
                // gaps is what leaves the page enough room for its words.
                .padding(horizontal = 32.dp, vertical = if (isLandscape) 16.dp else 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(steps[page], isLandscape)
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(if (index == pagerState.currentPage) 12.dp else 10.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

            Button(
                onClick = {
                    if (isLastStep) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(QRieuxUiConfig.controlHeight),
                shape = RoundedCornerShape(QRieuxUiConfig.controlCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(
                        if (isLastStep) R.string.onboarding_get_started
                        else R.string.onboarding_next
                    ),
                    fontSize = QRieuxUiConfig.buttonSize
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onFinish) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = QRieuxUiConfig.supportingSize
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(step: OnboardingStep, isLandscape: Boolean) {
    if (isLandscape) {
        // Stacked, a page barely a hundred dp tall showed the illustration and
        // nothing else — a first-run user saw four pictures and no words. Beside it
        // the text gets the height the picture was taking.
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Image(
                painter = painterResource(step.imageRes),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OnboardingText(step)
                }
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(step.imageRes),
                contentDescription = null,
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))

            OnboardingText(step)
        }
    }
}

@Composable
private fun OnboardingText(step: OnboardingStep) {
    Text(
        text = stringResource(step.titleRes),
        color = Color.White,
        fontSize = QRieuxUiConfig.titleSize,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(step.descRes),
        color = Color.White.copy(alpha = 0.8f),
        fontSize = QRieuxUiConfig.bodySize,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
