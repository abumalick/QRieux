package net.hilson.qrieux.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.hilson.qrieux.R
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
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()
    val isLastStep = pagerState.currentPage == steps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(steps[page].imageRes),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(steps[page].titleRes),
                        color = Color.White,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(steps[page].descRes),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isLastStep) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(
                        if (isLastStep) R.string.onboarding_get_started
                        else R.string.onboarding_next
                    ),
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onFinish) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp
                )
            }
        }
    }
}
