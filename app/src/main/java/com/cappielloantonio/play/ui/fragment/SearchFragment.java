package com.cappielloantonio.play.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cappielloantonio.play.R;
import com.cappielloantonio.play.adapter.AlbumCatalogueAdapter;
import com.cappielloantonio.play.adapter.ArtistCatalogueAdapter;
import com.cappielloantonio.play.adapter.RecentSearchAdapter;
import com.cappielloantonio.play.adapter.SongResultSearchAdapter;
import com.cappielloantonio.play.databinding.FragmentSearchBinding;
import com.cappielloantonio.play.helper.recyclerview.ItemlDecoration;
import com.cappielloantonio.play.model.Artist;
import com.cappielloantonio.play.model.RecentSearch;
import com.cappielloantonio.play.model.Song;
import com.cappielloantonio.play.ui.activities.MainActivity;
import com.cappielloantonio.play.viewmodel.SearchViewModel;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {
    private static final String TAG = "SearchFragment";

    private FragmentSearchBinding bind;
    private MainActivity activity;
    private SearchViewModel searchViewModel;

    private RecentSearchAdapter recentSearchAdapter;
    private SongResultSearchAdapter songResultSearchAdapter;
    private AlbumCatalogueAdapter albumResultSearchAdapter;
    private ArtistCatalogueAdapter artistResultSearchAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        bind = FragmentSearchBinding.inflate(inflater, container, false);
        View view = bind.getRoot();
        searchViewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);

        init();
        initRecentSearchView();
        initSearchResultView();
        initSearchView();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void init() {
        bind.clearAllSearchTextViewClickable.setOnClickListener(v -> searchViewModel.deleteAllRecentSearch());
    }

    private void initRecentSearchView() {
        bind.recentlySearchedTracksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.recentlySearchedTracksRecyclerView.setHasFixedSize(true);

        recentSearchAdapter = new RecentSearchAdapter(requireContext(), new ArrayList<>());
        recentSearchAdapter.setClickListener((view, position) -> {
            RecentSearch search = recentSearchAdapter.getItem(position);
            search(search.getSearch());
        });
        bind.recentlySearchedTracksRecyclerView.setAdapter(recentSearchAdapter);

        searchViewModel.getSearchList().observe(requireActivity(), recentSearches -> recentSearchAdapter.setItems(recentSearches));
    }

    private void initSearchResultView() {
        // Songs
        bind.searchResultTracksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.searchResultTracksRecyclerView.setHasFixedSize(true);

        songResultSearchAdapter = new SongResultSearchAdapter(requireContext(), new ArrayList<>());
        bind.searchResultTracksRecyclerView.setAdapter(songResultSearchAdapter);

        // Albums
        bind.searchResultAlbumRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        bind.searchResultAlbumRecyclerView.addItemDecoration(new ItemlDecoration(2, 20, false));
        bind.searchResultAlbumRecyclerView.setHasFixedSize(true);

        albumResultSearchAdapter = new AlbumCatalogueAdapter(requireContext(), new ArrayList<>());
        albumResultSearchAdapter.setClickListener((view, position) -> {
            Toast.makeText(requireContext(), "Album " + position, Toast.LENGTH_SHORT).show();
        });
        bind.searchResultAlbumRecyclerView.setAdapter(albumResultSearchAdapter);

        // Artist
        bind.searchResultArtistRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        bind.searchResultArtistRecyclerView.addItemDecoration(new ItemlDecoration(2, 20, false));
        bind.searchResultArtistRecyclerView.setHasFixedSize(true);

        artistResultSearchAdapter = new ArtistCatalogueAdapter(requireContext(), new ArrayList<>());
        artistResultSearchAdapter.setClickListener((view, position) -> {
            Toast.makeText(requireContext(), "Artist " + position, Toast.LENGTH_SHORT).show();
            Bundle bundle = new Bundle();
            bundle.putString("artistID", artistResultSearchAdapter.getItem(position).id);
            activity.navController.navigate(R.id.action_searchFragment_to_artistPageFragment, bundle);
        });
        bind.searchResultArtistRecyclerView.setAdapter(artistResultSearchAdapter);
    }

    private void initSearchView() {
        bind.persistentSearchView.showRightButton();
        
        bind.persistentSearchView.setOnSearchQueryChangeListener((searchView, oldQuery, newQuery) -> {
        });

        bind.persistentSearchView.setOnLeftBtnClickListener(view -> {
        });

        bind.persistentSearchView.setOnRightBtnClickListener(view -> {
            activity.navController.navigate(R.id.action_searchFragment_to_filterFragment);
        });

        bind.persistentSearchView.setOnSearchConfirmedListener((searchView, query) -> {
            search(query);
        });
    }

    public void search(String query) {
        if (!query.equals("")) {
            searchViewModel.insertNewSearch(query);
            bind.persistentSearchView.collapse();

            bind.persistentSearchView.setInputQuery(query);
            performSearch(query);
        }
    }

    private void performSearch(String query) {
        searchViewModel.searchSong(query).observe(requireActivity(), songs -> songResultSearchAdapter.setItems(songs));
        searchViewModel.searchAlbum(query).observe(requireActivity(), albums -> albumResultSearchAdapter.setItems(albums));
        searchViewModel.searchArtist(query).observe(requireActivity(), artists -> artistResultSearchAdapter.setItems(artists));

        bind.searchResultNestedScrollView.setVisibility(View.VISIBLE);
    }
}
