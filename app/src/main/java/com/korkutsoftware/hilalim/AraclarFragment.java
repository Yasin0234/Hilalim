package com.korkutsoftware.hilalim;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.korkutsoftware.hilalim.util.AdManager;

import java.util.Calendar;

public class AraclarFragment extends Fragment {

    public AraclarFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_araclar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_location).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LocationActivity.class));
        });

        View.OnClickListener openMosquesListener = v -> openNearestMosquesOnMap();
        
        View cardMosque = view.findViewById(R.id.card_nearest_mosque);
        if (cardMosque != null) {
            cardMosque.setOnClickListener(openMosquesListener);
        }

        View btnMaps = view.findViewById(R.id.btn_open_maps);
        if (btnMaps != null) {
            btnMaps.setOnClickListener(openMosquesListener);
        }

        view.findViewById(R.id.card_esma).setOnClickListener(v -> {
            openToolWithAd(EsmaHusnaActivity.class);
        });

        view.findViewById(R.id.card_zikirmatik).setOnClickListener(v -> {
            openToolWithAd(ZikirmatikActivity.class);
        });

        view.findViewById(R.id.card_zekat).setOnClickListener(v -> {
            openToolWithAd(ZekatCalculatorActivity.class);
        });

        view.findViewById(R.id.card_dini_gunler).setOnClickListener(v -> {
            openToolWithAd(DiniGunlerActivity.class);
        });

        View btnSettings = view.findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                intent.putExtra("language_only", true);
                startActivity(intent);
            });
        }

        View cardAylik = view.findViewById(R.id.card_aylik_vakitler);
        if (cardAylik != null) {
            cardAylik.setOnClickListener(v -> {
                openToolWithAd(AylikVakitlerActivity.class);
            });
        }

        view.findViewById(R.id.card_namaz_rehberi).setOnClickListener(v -> {
            openToolWithAd(NamazRehberiActivity.class);
        });

        updateDailyDua(view);
    }

    private void openToolWithAd(Class<?> activityClass) {
        if (getActivity() == null) return;
        AdManager.showInterstitialIfReady(requireActivity(), () -> {
            if (isAdded()) {
                startActivity(new Intent(getActivity(), activityClass));
            }
        });
    }

    private void updateDailyDua(View root) {
        TextView txtArabic = root.findViewById(R.id.dua_arabic);
        TextView txtTranslation = root.findViewById(R.id.dua_turkish);
        if (txtArabic == null || txtTranslation == null) return;

        String[][] duas = {
                {"رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "\"Rabbimiz! Bize dünyada da iyilik ver, ahirette de iyilik ver ve bizi ateş azabından koru.\""},
                {"رَبِّ زِدْنِي عِلْمًا", "\"Rabbim! İlmimi artır.\""},
                {"رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً", "\"Rabbimiz! Bizi hidayete erdirdikten sonra kalplerimizi eğriltme ve katından bize rahmet bağışla.\""},
                {"حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "\"Allah bize yeter, O ne güzel vekildir.\""},
                {"اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ", "\"Allah'ım! Seni zikretmek, sana şükretmek ve sana güzelce ibadet etmek için bana yardım et.\""}
        };
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        String[] selected = duas[dayOfYear % duas.length];
        txtArabic.setText(selected[0]);
        txtTranslation.setText(selected[1]);
    }

    private void openNearestMosquesOnMap() {
        if (getContext() == null) return;
        try {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=cami");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/cami/"));
                startActivity(webIntent);
            }
        } catch (Exception e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/cami/"));
            startActivity(webIntent);
        }
    }
}
