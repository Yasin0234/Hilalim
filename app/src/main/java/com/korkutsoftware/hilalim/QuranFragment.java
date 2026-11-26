package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
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

import com.google.android.material.chip.Chip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.korkutsoftware.hilalim.data.db.AppDatabase;
import com.korkutsoftware.hilalim.data.db.QuranDao;
import com.korkutsoftware.hilalim.data.db.QuranVerseEntity;
import com.korkutsoftware.hilalim.util.JuzHelper;
import com.korkutsoftware.hilalim.util.QuranImportHelper;
import com.korkutsoftware.hilalim.util.QuranPrefsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuranFragment extends Fragment {

    public static final String PREFS_QURAN = QuranPrefsHelper.PREFS_QURAN;
    public static final String KEY_LAST_SURAH_ID = QuranPrefsHelper.KEY_LAST_SURAH_ID;
    public static final String KEY_LAST_SURAH_NAME = QuranPrefsHelper.KEY_LAST_SURAH_NAME;

    private static final int MODE_SURAHS = 0;
    private static final int MODE_SEARCH_VERSES = 1;
    private static final int MODE_JUZ = 2;
    private static final int MODE_BOOKMARKS = 3;

    private RecyclerView rv;
    private SurahAdapter surahAdapter;
    private VerseSearchAdapter verseSearchAdapter;
    private JuzAdapter juzAdapter;
    private BookmarkAdapter bookmarkAdapter;
    private View cardContinueReading;
    private View btnClearSearch;
    private EditText etSearch;
    private TextView txtLastSurahName, txtBookmarksEmpty;
    private Chip chipSurahs, chipSearchVerses, chipJuz, chipBookmarks;

    private final SparseArray<String> surahNameMap = new SparseArray<>();
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    private int lastSurahId = 1;
    private String lastSurahName = "Fatiha";
    private int lastVerseId = 1;
    private int currentMode = MODE_SURAHS;
    private String currentSearchQuery = "";


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
        txtBookmarksEmpty = view.findViewById(R.id.txt_bookmarks_empty);
        chipSurahs = view.findViewById(R.id.chip_quran_surahs);
        chipSearchVerses = view.findViewById(R.id.chip_quran_search_verses);
        chipJuz = view.findViewById(R.id.chip_quran_juz);
        chipBookmarks = view.findViewById(R.id.chip_quran_bookmarks);
        etSearch = view.findViewById(R.id.et_search);

        if (cardContinueReading != null) {
            cardContinueReading.setOnClickListener(v -> openSurah(lastSurahId, lastSurahName, lastVerseId));
        }

        rv = view.findViewById(R.id.rv_surah);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        surahAdapter = new SurahAdapter(new ArrayList<>());
        verseSearchAdapter = new VerseSearchAdapter(new ArrayList<>());
        juzAdapter = new JuzAdapter(JuzHelper.getAllJuzList());
        bookmarkAdapter = new BookmarkAdapter(new ArrayList<>());
        rv.setAdapter(surahAdapter);

        QuranImportHelper.importIfNeededAsync(requireContext());
        loadSurahsAsync();

        if (chipSurahs != null) chipSurahs.setOnClickListener(v -> switchMode(MODE_SURAHS));
        if (chipSearchVerses != null) chipSearchVerses.setOnClickListener(v -> switchMode(MODE_SEARCH_VERSES));
        if (chipJuz != null) chipJuz.setOnClickListener(v -> switchMode(MODE_JUZ));
        if (chipBookmarks != null) chipBookmarks.setOnClickListener(v -> switchMode(MODE_BOOKMARKS));

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString() : "";
                boolean hasQuery = currentSearchQuery.length() > 0;
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                }
                updateContinueCardVisibility();
                applyCurrentFilter();
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
        if (currentMode == MODE_BOOKMARKS) {
            loadBookmarksIntoAdapter();
        }
    }

    @Override
    public void onDestroyView() {
        if (pendingSearchRunnable != null) {
            mainHandler.removeCallbacks(pendingSearchRunnable);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        searchExecutor.shutdown();
        super.onDestroy();
    }

    private void switchMode(int mode) {
        currentMode = mode;
        updateContinueCardVisibility();
        if (etSearch != null) {
            etSearch.setHint(mode == MODE_SEARCH_VERSES
                    ? R.string.quran_search_verse_hint
                    : R.string.sure_ara);
        }

        if (mode == MODE_BOOKMARKS) {
            loadBookmarksIntoAdapter();
            rv.setAdapter(bookmarkAdapter);
        } else if (mode == MODE_SEARCH_VERSES) {
            rv.setAdapter(verseSearchAdapter);
            applyCurrentFilter();
        } else if (mode == MODE_JUZ) {
            if (txtBookmarksEmpty != null) txtBookmarksEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setAdapter(juzAdapter);
            juzAdapter.filter(currentSearchQuery);
        } else {
            if (txtBookmarksEmpty != null) txtBookmarksEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setAdapter(surahAdapter);
            surahAdapter.getFilter().filter(currentSearchQuery);
        }
    }

    private void applyCurrentFilter() {
        if (currentMode == MODE_BOOKMARKS) {
            bookmarkAdapter.filter(currentSearchQuery);
            updateEmptyState();
        } else if (currentMode == MODE_SEARCH_VERSES) {
            scheduleVerseSearch(currentSearchQuery);
        } else if (currentMode == MODE_JUZ) {
            juzAdapter.filter(currentSearchQuery);
        } else {
            surahAdapter.getFilter().filter(currentSearchQuery);
        }
    }

    private void scheduleVerseSearch(String rawQuery) {
        if (pendingSearchRunnable != null) {
            mainHandler.removeCallbacks(pendingSearchRunnable);
        }
        String trimmed = rawQuery != null ? rawQuery.trim() : "";
        if (trimmed.length() < 3) {
            verseSearchAdapter.setItems(new ArrayList<>());
            if (txtBookmarksEmpty != null) {
                txtBookmarksEmpty.setText(R.string.quran_search_min_chars);
                txtBookmarksEmpty.setVisibility(View.VISIBLE);
            }
            rv.setVisibility(View.GONE);
            return;
        }
        if (txtBookmarksEmpty != null) txtBookmarksEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);

        pendingSearchRunnable = () -> runVerseSearchAsync(trimmed);
        mainHandler.postDelayed(pendingSearchRunnable, 220);
    }

    private void runVerseSearchAsync(String query) {
        Context ctx = getContext();
        if (ctx == null) return;
        Context appCtx = ctx.getApplicationContext();

        searchExecutor.execute(() -> {
            if (!QuranImportHelper.isImported(appCtx)) {
                QuranImportHelper.importBlocking(appCtx);
            }
            QuranDao dao = AppDatabase.getInstance(appCtx).quranDao();
            String sanitized = query.replace("\"", "").replace("*", "").trim();
            List<QuranVerseEntity> results = new ArrayList<>();
            if (!sanitized.isEmpty()) {
                try {
                    results = dao.searchVersesFts(sanitized + "*");
                } catch (Exception ignored) {
                }
                if (results == null || results.isEmpty()) {
                    results = dao.searchVersesLike(sanitized);
                }
            }
            final List<QuranVerseEntity> finalResults = results != null ? results : new ArrayList<>();
            mainHandler.post(() -> {
                if (!isAdded() || currentMode != MODE_SEARCH_VERSES) return;
                verseSearchAdapter.setItems(finalResults);
                if (txtBookmarksEmpty != null) {
                    boolean empty = finalResults.isEmpty();
                    txtBookmarksEmpty.setText(R.string.quran_search_empty);
                    txtBookmarksEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rv.setVisibility(empty ? View.GONE : View.VISIBLE);
                }
            });
        });
    }

    private void loadBookmarksIntoAdapter() {
        if (getContext() == null) return;
        List<QuranPrefsHelper.Bookmark> list = QuranPrefsHelper.getBookmarks(getContext());
        bookmarkAdapter.setItems(list);
        bookmarkAdapter.filter(currentSearchQuery);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (currentMode != MODE_BOOKMARKS || txtBookmarksEmpty == null) return;
        boolean empty = bookmarkAdapter.getItemCount() == 0;
        txtBookmarksEmpty.setText(R.string.quran_bookmarks_empty);
        txtBookmarksEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateContinueCardVisibility() {
        if (cardContinueReading == null) return;
        boolean hasQuery = currentSearchQuery != null && currentSearchQuery.length() > 0;
        cardContinueReading.setVisibility((hasQuery || currentMode != MODE_SURAHS) ? View.GONE : View.VISIBLE);
    }

    private void refreshContinueReadingCard() {
        Context ctx = getContext();
        if (ctx == null) return;
        lastSurahId = QuranPrefsHelper.getLastSurahId(ctx);
        lastSurahName = QuranPrefsHelper.getLastSurahName(ctx);
        lastVerseId = QuranPrefsHelper.getLastVerseId(ctx);
        if (txtLastSurahName != null) {
            String base = getString(R.string.quran_surah_format, lastSurahId) + " • " + lastSurahName;
            if (lastVerseId > 1) {
                base += " • " + getString(R.string.quran_verse_format, lastVerseId);
            }
            txtLastSurahName.setText(base);
        }
        if (chipBookmarks != null) {
            int count = QuranPrefsHelper.getBookmarks(ctx).size();
            chipBookmarks.setText(count > 0
                    ? getString(R.string.quran_tab_bookmarks_count, count)
                    : getString(R.string.quran_tab_bookmarks));
        }
    }

    private void openSurah(int surahId, String surahName, int verseId) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), SurahDetailActivity.class);
        intent.putExtra("surah_id", surahId);
        intent.putExtra("surah_name", surahName);
        if (verseId > 0) {
            intent.putExtra("verse_id", verseId);
        }
        startActivity(intent);
    }

    private void loadSurahsAsync() {
        Context appContext = requireContext().getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SurahItem> loaded = loadSurahsFromJson(appContext);
            mainHandler.post(() -> {
                if (isAdded() && surahAdapter != null && loaded != null) {
                    surahNameMap.clear();
                    for (SurahItem s : loaded) {
                        surahNameMap.put(s.id, s.name);
                    }
                    surahAdapter.setItems(loaded);
                    if (juzAdapter != null) {
                        juzAdapter.notifyDataSetChanged();
                    }
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

            holder.itemView.setOnClickListener(v -> openSurah(item.id, item.name, 0));
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

    private class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
        private final List<QuranPrefsHelper.Bookmark> fullItems = new ArrayList<>();
        private final List<QuranPrefsHelper.Bookmark> visibleItems = new ArrayList<>();

        BookmarkAdapter(List<QuranPrefsHelper.Bookmark> items) {
            setItems(items);
        }

        void setItems(List<QuranPrefsHelper.Bookmark> items) {
            fullItems.clear();
            if (items != null) fullItems.addAll(items);
            filter(currentSearchQuery);
        }

        void filter(String query) {
            visibleItems.clear();
            if (query == null || query.trim().isEmpty()) {
                visibleItems.addAll(fullItems);
            } else {
                Locale trLocale = new Locale("tr");
                String q = query.toLowerCase(trLocale).trim();
                for (QuranPrefsHelper.Bookmark b : fullItems) {
                    String hay = ((b.surahName != null ? b.surahName : "") + " " + (b.tr != null ? b.tr : ""))
                            .toLowerCase(trLocale);
                    if (hay.contains(q) || (b.ar != null && b.ar.contains(q))) {
                        visibleItems.add(b);
                    }
                }
            }
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
            QuranPrefsHelper.Bookmark b = visibleItems.get(position);
            holder.txtId.setText(b.surahId + ":" + b.verseId);
            holder.txtNameTr.setText(b.surahName + " • " + getString(R.string.quran_verse_format, b.verseId));
            holder.txtNameAr.setText("");
            holder.txtDesc.setText(b.tr != null ? b.tr : "");
            holder.itemView.setOnClickListener(v -> openSurah(b.surahId, b.surahName, b.verseId));
        }

        @Override
        public int getItemCount() {
            return visibleItems.size();
        }

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

    private class VerseSearchAdapter extends RecyclerView.Adapter<VerseSearchAdapter.ViewHolder> {
        private final List<QuranVerseEntity> items = new ArrayList<>();

        VerseSearchAdapter(List<QuranVerseEntity> initial) {
            if (initial != null) items.addAll(initial);
        }

        void setItems(List<QuranVerseEntity> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
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
            QuranVerseEntity v = items.get(position);
            String surahName = surahNameMap.get(v.surahId);
            if (surahName == null || surahName.isEmpty()) {
                surahName = getString(R.string.quran_surah_format, v.surahId);
            }
            holder.txtId.setText(v.surahId + ":" + v.verseId);
            holder.txtNameTr.setText(surahName + " • " + getString(R.string.quran_verse_format, v.verseId));
            holder.txtNameAr.setText("");
            holder.txtDesc.setText(v.trText != null ? v.trText : "");
            String finalName = surahName;
            holder.itemView.setOnClickListener(view -> openSurah(v.surahId, finalName, v.verseId));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

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

    private class JuzAdapter extends RecyclerView.Adapter<JuzAdapter.ViewHolder> {
        private final List<JuzHelper.JuzInfo> fullItems = new ArrayList<>();
        private final List<JuzHelper.JuzInfo> visibleItems = new ArrayList<>();

        JuzAdapter(List<JuzHelper.JuzInfo> items) {
            if (items != null) {
                fullItems.addAll(items);
                visibleItems.addAll(items);
            }
        }

        void filter(String query) {
            visibleItems.clear();
            if (query == null || query.trim().isEmpty()) {
                visibleItems.addAll(fullItems);
            } else {
                Locale trLocale = new Locale("tr");
                String q = query.toLowerCase(trLocale).trim();
                for (JuzHelper.JuzInfo j : fullItems) {
                    String sName = surahNameMap.get(j.startSurahId, "");
                    String label = getString(R.string.quran_juz_format, j.juzNumber) + " " + sName;
                    if (label.toLowerCase(trLocale).contains(q) || String.valueOf(j.juzNumber).equals(q)) {
                        visibleItems.add(j);
                    }
                }
            }
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
            JuzHelper.JuzInfo j = visibleItems.get(position);
            String surahName = surahNameMap.get(j.startSurahId);
            if (surahName == null || surahName.isEmpty()) {
                surahName = getString(R.string.quran_surah_format, j.startSurahId);
            }
            holder.txtId.setText(String.valueOf(j.juzNumber));
            holder.txtNameTr.setText(getString(R.string.quran_juz_format, j.juzNumber));
            holder.txtNameAr.setText("");
            holder.txtDesc.setText(getString(R.string.quran_juz_start_format, surahName, j.startVerseId));
            String finalName = surahName;
            holder.itemView.setOnClickListener(v -> openSurah(j.startSurahId, finalName, j.startVerseId));
        }

        @Override
        public int getItemCount() {
            return visibleItems.size();
        }

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
