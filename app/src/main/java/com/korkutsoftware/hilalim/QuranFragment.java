package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class QuranFragment extends Fragment {

    public static final String PREFS_QURAN = "hilalim_quran_prefs";
    public static final String KEY_LAST_SURAH_ID = "last_surah_id";
    public static final String KEY_LAST_SURAH_NAME = "last_surah_name";

    private SurahAdapter adapter;
    private View cardContinueReading;
    private View btnClearSearch;
    private TextView txtLastSurahName;
    private int lastSurahId = 1;
    private String lastSurahName = "Fatiha";

    public QuranFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quran, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardContinueReading = view.findViewById(R.id.card_continue_reading);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        txtLastSurahName = view.findViewById(R.id.txt_last_surah_name);

        if (cardContinueReading != null) {
            cardContinueReading.setOnClickListener(v -> openSurah(lastSurahId, lastSurahName));
        }

        RecyclerView rv = view.findViewById(R.id.rv_surah);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SurahAdapter(new ArrayList<>());
        rv.setAdapter(adapter);
        loadSurahsAsync();

        EditText etSearch = view.findViewById(R.id.et_search);
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasQuery = s != null && s.length() > 0;
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                }
                if (cardContinueReading != null) {
                    cardContinueReading.setVisibility(hasQuery ? View.GONE : View.VISIBLE);
                }
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        refreshContinueReadingCard();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContinueReadingCard();
    }

    private void refreshContinueReadingCard() {
        Context ctx = getContext();
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE);
        lastSurahId = prefs.getInt(KEY_LAST_SURAH_ID, 1);
        lastSurahName = prefs.getString(KEY_LAST_SURAH_NAME, "Fatiha");
        if (txtLastSurahName != null) {
            txtLastSurahName.setText(getString(R.string.quran_surah_format, lastSurahId) + " • " + lastSurahName);
        }
    }

    private void openSurah(int surahId, String surahName) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), SurahDetailActivity.class);
        intent.putExtra("surah_id", surahId);
        intent.putExtra("surah_name", surahName);
        startActivity(intent);
    }

    private void loadSurahsAsync() {
        Context appContext = requireContext().getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SurahItem> loaded = loadSurahsFromJson(appContext);
            mainHandler.post(() -> {
                if (isAdded() && adapter != null && loaded != null) {
                    adapter.setItems(loaded);
                }
            });
        });
    }

    private List<SurahItem> loadSurahsFromJson(Context context) {
        try (InputStream is = context.getAssets().open("surahs.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int offset = 0;
            while (offset < size) {
                int read = is.read(buffer, offset, size - offset);
                if (read == -1) break;
                offset += read;
            }
            String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
            return new Gson().fromJson(json, new TypeToken<List<SurahItem>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static class SurahItem {
        int id;
        String name, name_ar, desc;
    }

    private class SurahAdapter extends RecyclerView.Adapter<SurahAdapter.ViewHolder> implements Filterable {
        private final List<SurahItem> items;
        private final List<SurahItem> itemsFull;

        SurahAdapter(List<SurahItem> items) {
            this.items = new ArrayList<>(items);
            this.itemsFull = new ArrayList<>(items);
        }

        void setItems(List<SurahItem> newItems) {
            items.clear();
            items.addAll(newItems);
            itemsFull.clear();
            itemsFull.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_surah, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SurahItem item = items.get(position);
            holder.txtId.setText(String.valueOf(item.id));
            holder.txtNameTr.setText(item.name);
            holder.txtNameAr.setText(item.name_ar);
            holder.txtDesc.setText(item.desc);

            holder.itemView.setOnClickListener(v -> openSurah(item.id, item.name));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public Filter getFilter() {
            return surahFilter;
        }

        private final Filter surahFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<SurahItem> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(itemsFull);
                } else {
                    Locale trLocale = new Locale("tr");
                    String filterPattern = constraint.toString().toLowerCase(trLocale).trim();
                    for (SurahItem item : itemsFull) {
                        boolean matchName = item.name != null && item.name.toLowerCase(trLocale).contains(filterPattern);
                        boolean matchId = String.valueOf(item.id).equals(filterPattern);
                        boolean matchAr = item.name_ar != null && item.name_ar.contains(filterPattern);
                        boolean matchDesc = item.desc != null && item.desc.toLowerCase(trLocale).contains(filterPattern);
                        if (matchName || matchId || matchAr || matchDesc) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                items.clear();
                items.addAll((List<SurahItem>) results.values);
                notifyDataSetChanged();
            }
        };

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtId, txtNameTr, txtNameAr, txtDesc;

            ViewHolder(View v) {
                super(v);
                txtId = v.findViewById(R.id.surah_id);
                txtNameTr = v.findViewById(R.id.surah_name_tr);
                txtNameAr = v.findViewById(R.id.surah_name_ar);
                txtDesc = v.findViewById(R.id.surah_desc);
            }
        }
    }
}
