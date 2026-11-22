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

import java.util.Arrays;
import java.util.List;

public class NamazRehberiActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_namaz_rehberi);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_guide);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<GuideItem> items = Arrays.asList(
                new GuideItem("Abdestin Farzları", "1. Yüzü yıkamak\n2. Elleri dirseklerle beraber yıkamak\n3. Başın dörtte birini meshetmek\n4. Ayakları topuklarla beraber yıkamak"),
                new GuideItem("Sabah Namazı (4 Rekât)", "2 Rekât Sünnet + 2 Rekât Farz olarak kılınır."),
                new GuideItem("Öğle Namazı (10 Rekât)", "4 Rekât İlk Sünnet + 4 Rekât Farz + 2 Rekât Son Sünnet olarak kılınır."),
                new GuideItem("İkindi Namazı (8 Rekât)", "4 Rekât Sünnet + 4 Rekât Farz olarak kılınır."),
                new GuideItem("Akşam Namazı (5 Rekât)", "3 Rekât Farz + 2 Rekât Sünnet olarak kılınır."),
                new GuideItem("Yatsı ve Vitir Namazı (13 Rekât)", "4 Rekât İlk Sünnet + 4 Rekât Farz + 2 Rekât Son Sünnet + 3 Rekât Vitir Vacip olarak kılınır."),
                new GuideItem("Namazın Dışındaki Farzlar (Şartlar)", "1. Hadesten Taharet (Abdest/Gusül)\n2. Necasetten Taharet (Temizlik)\n3. Setr-i Avret (Örtünme)\n4. İstikbal-i Kıble (Kıbleye Yönelme)\n5. Vakit\n6. Niyet"),
                new GuideItem("Namazın İçindeki Farzlar (Rükünler)", "1. İftitah Tekbiri (Allahu Ekber)\n2. Kıyam (Ayakta durmak)\n3. Kıraat (Kur'an okumak)\n4. Rükû\n5. Secde\n6. Kade-i Ahire (Son oturuş)")
        );

        rv.setAdapter(new GuideAdapter(items));
    }

    private static class GuideItem {
        final String title;
        final String body;

        GuideItem(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }

    private static class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.ViewHolder> {
        private final List<GuideItem> items;

        GuideAdapter(List<GuideItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_namaz_rehber, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GuideItem item = items.get(position);
            holder.txtTitle.setText(item.title);
            holder.txtBody.setText(item.body);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtTitle, txtBody;

            ViewHolder(View v) {
                super(v);
                txtTitle = v.findViewById(R.id.txt_guide_title);
                txtBody = v.findViewById(R.id.txt_guide_body);
            }
        }
    }
}
