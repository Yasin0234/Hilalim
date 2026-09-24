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

public class DiniGunlerActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dini_gunler);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_days);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<DiniGunItem> items = loadDaysFromJson();
        rv.setAdapter(new DiniGunAdapter(items));
    }

    private List<DiniGunItem> loadDaysFromJson() {
        try {
            InputStream is = getAssets().open("dini_gunler.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            return new Gson().fromJson(json, new TypeToken<List<DiniGunItem>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static class DiniGunItem {
        String name, date;
    }

    private class DiniGunAdapter extends RecyclerView.Adapter<DiniGunAdapter.ViewHolder> {
        private List<DiniGunItem> items;

        DiniGunAdapter(List<DiniGunItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dini_gun, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DiniGunItem item = items.get(position);
            holder.txtName.setText(item.name);
            holder.txtDate.setText(item.date);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtDate;

            ViewHolder(View v) {
                super(v);
                txtName = v.findViewById(R.id.txt_day_name);
                txtDate = v.findViewById(R.id.txt_day_date);
            }
        }
    }
}