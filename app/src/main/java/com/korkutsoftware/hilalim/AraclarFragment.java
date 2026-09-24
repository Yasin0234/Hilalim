package com.korkutsoftware.hilalim;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
            startActivity(new Intent(getActivity(), EsmaHusnaActivity.class));
        });

        view.findViewById(R.id.card_zikirmatik).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ZikirmatikActivity.class));
        });

        view.findViewById(R.id.card_zekat).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ZekatCalculatorActivity.class));
        });

        view.findViewById(R.id.card_dini_gunler).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), DiniGunlerActivity.class));
        });

        View cardAylik = view.findViewById(R.id.card_aylik_vakitler);
        if (cardAylik != null) {
            cardAylik.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Aylık vakitler yakında eklenecektir.", Toast.LENGTH_SHORT).show();
            });
        }

        view.findViewById(R.id.card_namaz_rehberi).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Namaz Rehberi yakında eklenecektir.", Toast.LENGTH_SHORT).show();
        });
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
