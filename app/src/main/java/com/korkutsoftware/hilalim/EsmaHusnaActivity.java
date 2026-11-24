package com.korkutsoftware.hilalim;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class EsmaHusnaActivity extends BaseActivity {

    private final Locale trLocale = new Locale("tr");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_esma_husna);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_esma);
        rv.setLayoutManager(new LinearLayoutManager(this));

        EsmaAdapter adapter = new EsmaAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.et_search_esma);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.filter(s != null ? s.toString() : "");
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<EsmaItem> items = loadEsmaFromJson();
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed() && items != null) {
                    adapter.setItems(items);
                }
            });
        });
    }

    private void showEsmaActionDialog(EsmaItem item) {
        new AlertDialog.Builder(this)
                .setTitle(item.arabic + "  •  " + item.turkish)
                .setMessage(item.meaning)
                .setPositiveButton(R.string.esma_open_in_zikirmatik, (d, w) -> {
                    Intent intent = new Intent(this, ZikirmatikActivity.class);
                    intent.putExtra(ZikirmatikActivity.EXTRA_CUSTOM_ZIKIR_TR, "Ya " + item.turkish);
                    intent.putExtra(ZikirmatikActivity.EXTRA_CUSTOM_ZIKIR_AR, "يَا " + item.arabic);
                    intent.putExtra(ZikirmatikActivity.EXTRA_CUSTOM_TARGET, 99);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private List<EsmaItem> loadEsmaFromJson() {
        try (InputStream is = getAssets().open("esma.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int offset = 0;
            while (offset < size) {
                int read = is.read(buffer, offset, size - offset);
                if (read == -1) break;
                offset += read;
            }
            String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
            return new Gson().fromJson(json, new TypeToken<List<EsmaItem>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static class EsmaItem {
        int id;
        String arabic, turkish, meaning;
    }

    private class EsmaAdapter extends RecyclerView.Adapter<EsmaAdapter.ViewHolder> {
        private final List<EsmaItem> itemsFull = new ArrayList<>();
        private final List<EsmaItem> items = new ArrayList<>();

        EsmaAdapter(List<EsmaItem> initial) {
            setItems(initial);
        }

        void setItems(List<EsmaItem> newItems) {
            itemsFull.clear();
            if (newItems != null) itemsFull.addAll(newItems);
            items.clear();
            items.addAll(itemsFull);
            notifyDataSetChanged();
        }

        void filter(String query) {
            items.clear();
            String q = query == null ? "" : query.toLowerCase(trLocale).trim();
            if (q.isEmpty()) {
                items.addAll(itemsFull);
            } else {
                for (EsmaItem item : itemsFull) {
                    String hay = ((item.turkish != null ? item.turkish : "") + " "
                            + (item.meaning != null ? item.meaning : "")).toLowerCase(trLocale);
                    if (hay.contains(q)
                            || String.valueOf(item.id).equals(q)
                            || (item.arabic != null && item.arabic.contains(q))) {
                        items.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_esma, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EsmaItem item = items.get(position);
            holder.txtId.setText(String.valueOf(item.id));
            holder.txtArabic.setText(item.arabic);
            holder.txtTurkish.setText(item.turkish);
            holder.txtMeaning.setText(item.meaning);
            holder.itemView.setOnClickListener(v -> showEsmaActionDialog(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtId, txtArabic, txtTurkish, txtMeaning;

            ViewHolder(View v) {
                super(v);
                txtId = v.findViewById(R.id.txt_id);
                txtArabic = v.findViewById(R.id.txt_arabic);
                txtTurkish = v.findViewById(R.id.txt_turkish);
                txtMeaning = v.findViewById(R.id.txt_meaning);
            }
        }
    }
}
