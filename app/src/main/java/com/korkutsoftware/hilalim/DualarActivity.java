package com.korkutsoftware.hilalim;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.korkutsoftware.hilalim.util.ShareCardHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class DualarActivity extends BaseActivity {

    private final List<DuaItem> allDuas = new ArrayList<>();
    private final List<DuaItem> filteredDuas = new ArrayList<>();
    private DuaAdapter adapter;
    private String selectedCategory = "";
    private String searchQuery = "";
    private final Locale trLocale = new Locale("tr");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dualar);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        selectedCategory = getString(R.string.dualar_all);

        RecyclerView rv = findViewById(R.id.rv_dualar);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DuaAdapter(filteredDuas);
        rv.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.et_search_dua);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().toLowerCase(trLocale).trim() : "";
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadDuasAsync();
    }

    private void loadDuasAsync() {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<DuaItem> loaded = readDuasFromAssets(this);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed() || loaded == null) return;
                allDuas.clear();
                allDuas.addAll(loaded);
                setupCategoryChips();
                applyFilter();
            });
        });
    }

    public static List<DuaItem> readDuasFromAssets(Context context) {
        try (InputStream is = context.getAssets().open("dualar.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int offset = 0;
            while (offset < size) {
                int read = is.read(buffer, offset, size - offset);
                if (read == -1) break;
                offset += read;
            }
            String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
            List<DuaItem> list = new Gson().fromJson(json, new TypeToken<List<DuaItem>>() {}.getType());
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void setupCategoryChips() {
        ChipGroup chipGroup = findViewById(R.id.chip_group_categories);
        chipGroup.removeAllViews();

        Set<String> categories = new LinkedHashSet<>();
        String allLabel = getString(R.string.dualar_all);
        categories.add(allLabel);
        for (DuaItem item : allDuas) {
            if (item.category != null && !item.category.isEmpty()) {
                categories.add(item.category);
            }
        }

        for (String cat : categories) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChecked(cat.equals(allLabel));
            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                applyFilter();
            });
            chipGroup.addView(chip);
        }
    }

    private void applyFilter() {
        String allLabel = getString(R.string.dualar_all);
        filteredDuas.clear();
        for (DuaItem item : allDuas) {
            boolean matchesCat = selectedCategory.equals(allLabel) || selectedCategory.equals(item.category);
            if (!matchesCat) continue;

            if (searchQuery.isEmpty()) {
                filteredDuas.add(item);
            } else {
                String haystack = (item.title + " " + item.category + " " + item.okunus + " " + item.anlam)
                        .toLowerCase(trLocale);
                if (haystack.contains(searchQuery)) {
                    filteredDuas.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    public static class DuaItem {
        public int id;
        public String category;
        public String title;
        public String arabic;
        public String okunus;
        public String anlam;
        public String kaynak;
        @com.google.gson.annotations.SerializedName("not")
        public String note;
    }

    private class DuaAdapter extends RecyclerView.Adapter<DuaAdapter.ViewHolder> {
        private final List<DuaItem> items;

        DuaAdapter(List<DuaItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dua, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DuaItem item = items.get(position);
            holder.txtCategory.setText(item.category);
            holder.txtTitle.setText(item.title);
            holder.txtArabic.setText(item.arabic);
            holder.txtOkunus.setText(getString(R.string.dua_reading_label) + " " + item.okunus);
            holder.txtAnlam.setText(getString(R.string.dua_meaning_label) + " " + item.anlam);
            holder.txtKaynak.setText(item.kaynak);

            if (item.note != null && !item.note.trim().isEmpty()) {
                holder.txtNot.setVisibility(View.VISIBLE);
                holder.txtNot.setText(item.note);
            } else {
                holder.txtNot.setVisibility(View.GONE);
            }

            if (holder.btnShare != null) {
                holder.btnShare.setOnClickListener(v -> ShareCardHelper.showShareOptionsDialog(
                        DualarActivity.this,
                        item.title,
                        item.arabic,
                        item.anlam,
                        item.kaynak
                ));
            }

            holder.btnCopy.setOnClickListener(v -> {
                String fullText = item.title + "\n\n" + item.arabic + "\n\n" + item.okunus + "\n\n" + item.anlam + "\n(" + item.kaynak + ")";
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(item.title, fullText));
                    Toast.makeText(DualarActivity.this, R.string.dua_copied, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtCategory, txtTitle, txtArabic, txtOkunus, txtAnlam, txtNot, txtKaynak;
            ImageView btnCopy, btnShare;

            ViewHolder(View v) {
                super(v);
                txtCategory = v.findViewById(R.id.txt_dua_category);
                txtTitle = v.findViewById(R.id.txt_dua_title);
                txtArabic = v.findViewById(R.id.txt_dua_arabic);
                txtOkunus = v.findViewById(R.id.txt_dua_okunus);
                txtAnlam = v.findViewById(R.id.txt_dua_anlam);
                txtNot = v.findViewById(R.id.txt_dua_not);
                txtKaynak = v.findViewById(R.id.txt_dua_kaynak);
                btnShare = v.findViewById(R.id.btn_share_dua);
                btnCopy = v.findViewById(R.id.btn_copy_dua);
            }
        }
    }
}
