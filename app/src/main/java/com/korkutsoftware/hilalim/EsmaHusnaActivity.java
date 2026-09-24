package com.korkutsoftware.hilalim;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EsmaHusnaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_esma_husna);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_esma);
        rv.setLayoutManager(new LinearLayoutManager(this));
        
        List<EsmaItem> items = loadEsmaFromJson();
        rv.setAdapter(new EsmaAdapter(items));
    }

    private List<EsmaItem> loadEsmaFromJson() {
        try {
            InputStream is = getAssets().open("esma.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
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
        private List<EsmaItem> items;

        EsmaAdapter(List<EsmaItem> items) {
            this.items = items;
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