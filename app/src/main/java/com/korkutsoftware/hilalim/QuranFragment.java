package com.korkutsoftware.hilalim;

import android.content.Intent;
import android.os.Bundle;
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

public class QuranFragment extends Fragment {

    private SurahAdapter adapter;

    public QuranFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quran, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rv_surah);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        List<SurahItem> items = loadSurahsFromJson();
        adapter = new SurahAdapter(items);
        rv.setAdapter(adapter);

        EditText etSearch = view.findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private List<SurahItem> loadSurahsFromJson() {
        try {
            InputStream is = requireContext().getAssets().open("surahs.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
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

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SurahDetailActivity.class);
                intent.putExtra("surah_id", item.id);
                intent.putExtra("surah_name", item.name);
                startActivity(intent);
            });
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
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (SurahItem item : itemsFull) {
                        if (item.name.toLowerCase().contains(filterPattern)) {
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
