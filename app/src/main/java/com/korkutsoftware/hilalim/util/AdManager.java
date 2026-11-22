package com.korkutsoftware.hilalim.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.korkutsoftware.hilalim.BuildConfig;

public class AdManager {

    private static final long INTERSTITIAL_COOLDOWN_MS = 90_000L;
    private static boolean isInitialized = false;
    private static InterstitialAd interstitialAd = null;
    private static boolean isLoadingInterstitial = false;
    private static long lastInterstitialShownMs = 0L;

    public static void init(@NonNull Context context) {
        if (isInitialized) return;
        isInitialized = true;
        try {
            Context appContext = context.getApplicationContext();
            MobileAds.initialize(appContext, initializationStatus ->
                    new Handler(Looper.getMainLooper()).post(() -> preloadInterstitial(appContext))
            );
        } catch (Throwable ignored) {
        }
    }

    public static AdView loadBanner(@NonNull Activity activity, @NonNull FrameLayout container) {
        try {
            init(activity);
            AdView adView = new AdView(activity);
            adView.setAdUnitId(BuildConfig.ADMOB_BANNER_ID);
            adView.setAdSize(getAdaptiveBannerSize(activity));

            container.setVisibility(View.GONE);
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    container.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    container.setVisibility(View.GONE);
                }
            });

            container.removeAllViews();
            container.addView(adView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            adView.loadAd(new AdRequest.Builder().build());
            return adView;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static AdSize getAdaptiveBannerSize(@NonNull Activity activity) {
        DisplayMetrics outMetrics = activity.getResources().getDisplayMetrics();
        int adWidthPixels = outMetrics.widthPixels;
        int adWidth = (int) (adWidthPixels / outMetrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    public static void preloadInterstitial(@NonNull Context context) {
        if (interstitialAd != null || isLoadingInterstitial) return;
        isLoadingInterstitial = true;

        try {
            Context appContext = context.getApplicationContext();
            InterstitialAd.load(
                    appContext,
                    BuildConfig.ADMOB_INTERSTITIAL_ID,
                    new AdRequest.Builder().build(),
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd ad) {
                            interstitialAd = ad;
                            isLoadingInterstitial = false;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            interstitialAd = null;
                            isLoadingInterstitial = false;
                        }
                    }
            );
        } catch (Throwable ignored) {
            isLoadingInterstitial = false;
        }
    }

    public static void showInterstitialIfReady(@NonNull Activity activity, @NonNull Runnable onProceed) {
        long now = System.currentTimeMillis();
        if (interstitialAd == null || (now - lastInterstitialShownMs) < INTERSTITIAL_COOLDOWN_MS) {
            preloadInterstitial(activity);
            onProceed.run();
            return;
        }

        InterstitialAd adToShow = interstitialAd;
        interstitialAd = null;
        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                lastInterstitialShownMs = System.currentTimeMillis();
                preloadInterstitial(activity);
                onProceed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                preloadInterstitial(activity);
                onProceed.run();
            }
        });
        adToShow.show(activity);
    }
}
