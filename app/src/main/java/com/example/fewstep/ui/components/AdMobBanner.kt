package com.example.fewstep.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError

import com.example.fewstep.BuildConfig

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        factory = { context ->
            AdView(context).apply {
                // Use Standard Banner size
                setAdSize(AdSize.BANNER)
                // Production Ad Unit ID
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        android.util.Log.d("AdMob", "✅ Banner Ad Loaded successfully")
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        android.util.Log.e("AdMob", "❌ Banner Ad Failed to load: ${adError.message}")
                        android.util.Log.e("AdMob", "Error code: ${adError.code}")
                    }

                    override fun onAdOpened() {
                        android.util.Log.d("AdMob", "ðŸ‘€ Banner Ad Opened")
                    }
                }
                
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
