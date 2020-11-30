package com.cappielloantonio.play.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.cappielloantonio.play.R;
import com.cappielloantonio.play.databinding.FragmentFilterBinding;
import com.cappielloantonio.play.model.Genre;
import com.cappielloantonio.play.model.Song;
import com.cappielloantonio.play.ui.activities.MainActivity;
import com.cappielloantonio.play.viewmodel.FilterViewModel;
import com.google.android.material.chip.Chip;

public class FilterFragment extends Fragment {
    private static final String TAG = "FilterFragment";

    private MainActivity activity;
    private FragmentFilterBinding bind;
    private FilterViewModel filterViewModel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        bind = FragmentFilterBinding.inflate(inflater, container, false);
        View view = bind.getRoot();
        filterViewModel = new ViewModelProvider(requireActivity()).get(FilterViewModel.class);

        init();
        setFilterChips();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.setBottomNavigationBarVisibility(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void init() {
        Bundle bundle = new Bundle();
        bundle.putString(Song.BY_GENRES, Song.BY_GENRES);
        bundle.putStringArrayList("filters_list", filterViewModel.getFilters());
        bundle.putStringArrayList("filter_name_list", filterViewModel.getFilterNames());
        bind.finishFilteringTextViewClickable.setOnClickListener(v -> {
            if(filterViewModel.getFilters().size() > 1) activity.navController.navigate(R.id.action_filterFragment_to_songListPageFragment, bundle);
            else Toast.makeText(requireContext(), "Select two or more filters", Toast.LENGTH_SHORT).show();
        });
    }

    private void setFilterChips() {
        filterViewModel.getGenreList().observe(requireActivity(), genres -> {
            bind.loadingProgressBar.setVisibility(View.GONE);
            bind.filterContainer.setVisibility(View.VISIBLE);
            for (Genre genre : genres) {
                Chip chip = (Chip) requireActivity().getLayoutInflater().inflate(R.layout.chip_search_filter_genre, null, false);
                chip.setText(genre.getName());
                chip.setChecked(filterViewModel.getFilters().contains(genre.getId()));
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if(isChecked) filterViewModel.addFilter(genre.getId(), buttonView.getText().toString());
                    else filterViewModel.removeFilter(genre.getId(), buttonView.getText().toString());
                });
                bind.filtersChipsGroup.addView(chip);
            }
        });
    }
}