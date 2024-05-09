package com.cappielloantonio.tempo.ui.fragment;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;

import com.cappielloantonio.tempo.databinding.InnerFragmentPlayerLyricsBinding;
import com.cappielloantonio.tempo.service.MediaService;
import com.cappielloantonio.tempo.subsonic.models.LyricsList;
import com.cappielloantonio.tempo.subsonic.models.StructuredLyrics;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.util.OpenSubsonicExtensionsUtil;
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import moe.peanutmelonseedbigalmond.tempo.ui.widget.LrcViewTouchEvent;
import moe.peanutmelonseedbigalmond.tempo.ui.widget.data.StructedLrc;


@OptIn(markerClass = UnstableApi.class)
public class PlayerLyricsFragment extends Fragment {
    private static final String TAG = "PlayerLyricsFragment";

    private InnerFragmentPlayerLyricsBinding bind;
    private PlayerBottomSheetViewModel playerBottomSheetViewModel;
    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;
    private MediaBrowser mediaBrowser;
    private Handler syncLyricsHandler;
    private Runnable syncLyricsRunnable;

    private MainActivity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        bind = InnerFragmentPlayerLyricsBinding.inflate(inflater, container, false);
        View view = bind.getRoot();

        activity=(MainActivity) getActivity();

        playerBottomSheetViewModel = new ViewModelProvider(requireActivity()).get(PlayerBottomSheetViewModel.class);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initPanelContent();
        initLrcView();
    }

    @Override
    public void onStart() {
        super.onStart();
        initializeBrowser();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindMediaController();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseHandler();
    }

    @Override
    public void onStop() {
        releaseBrowser();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind.lrcFragmentLrcView.clearTouchListener();
        bind = null;
    }

    private void initLrcView(){
        bind.lrcFragmentLrcView.setDraggable(true,(view,time)->{
            if (mediaBrowser!=null){
                mediaBrowser.seekTo(time);
            }
            return true;
        });
        bind.lrcFragmentLrcView.setTouchListsner(new LrcViewTouchEvent() {
            @Override
            public void onTouchStart() {
                activity.setBottomSheetDraggableState(false);
                PlayerBottomSheetFragment playerBottomSheetFragment = (PlayerBottomSheetFragment) requireActivity().getSupportFragmentManager().findFragmentByTag("PlayerBottomSheet");
                if (playerBottomSheetFragment!=null){
                    playerBottomSheetFragment.setPlayerControllerVerticalPagerDraggableState(false);
                }
            }

            @Override
            public void onTouchEnd() {
                activity.setBottomSheetDraggableState(true);
                PlayerBottomSheetFragment playerBottomSheetFragment = (PlayerBottomSheetFragment) requireActivity().getSupportFragmentManager().findFragmentByTag("PlayerBottomSheet");
                if (playerBottomSheetFragment!=null){
                    playerBottomSheetFragment.setPlayerControllerVerticalPagerDraggableState(true);
                }
            }
        });
    }

    private void initializeBrowser() {
        mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync();
    }

    private void releaseHandler() {
        if (syncLyricsHandler != null) {
            syncLyricsHandler.removeCallbacks(syncLyricsRunnable);
            syncLyricsHandler = null;
        }
    }

    private void releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture);
    }

    private void bindMediaController() {
        mediaBrowserListenableFuture.addListener(() -> {
            try {
                mediaBrowser = mediaBrowserListenableFuture.get();
                defineProgressHandler();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void initPanelContent() {
        if (OpenSubsonicExtensionsUtil.isSongLyricsExtensionAvailable()) {
            playerBottomSheetViewModel.getLiveLyricsList().observe(getViewLifecycleOwner(), this::loadLyric);
        }
    }

    private void loadLyric(@Nullable LyricsList lyricsList) {
        if (lyricsList == null || lyricsList.getStructuredLyrics() == null) {
            bind.lrcFragmentLrcView.reset();
            playerBottomSheetViewModel.setStructedLrcList(Collections.emptyList());
            return;
        }
        StructuredLyrics structuredLyrics = lyricsList.getStructuredLyrics().get(0);
        if (structuredLyrics == null || structuredLyrics.getLine() == null) {
            bind.lrcFragmentLrcView.reset();
            playerBottomSheetViewModel.setStructedLrcList(Collections.emptyList());
            return;
        }

        int offset = structuredLyrics.getOffset();
        List<StructedLrc> newStructedLrc = structuredLyrics.getLine().stream().map(
                it -> new StructedLrc(it.getStart() + offset, it.getValue())
        ).collect(Collectors.toList());
        if (!playerBottomSheetViewModel.getStructedLrcList().equals(newStructedLrc)) {
            playerBottomSheetViewModel.setStructedLrcList(newStructedLrc);
            bind.lrcFragmentLrcView.loadStructedLyric(newStructedLrc);
        }
    }

    private void defineProgressHandler() {
        playerBottomSheetViewModel.getLiveLyricsList().observe(getViewLifecycleOwner(), lyricsList -> {
            if (lyricsList != null) {

                if (lyricsList.getStructuredLyrics() != null && lyricsList.getStructuredLyrics().get(0) != null && !lyricsList.getStructuredLyrics().get(0).getSynced()) {
                    releaseHandler();
                    return;
                }

                syncLyricsHandler = new Handler();
                syncLyricsRunnable = () -> {
                    if (syncLyricsHandler != null) {
                        if (bind != null) {
                            long currentTime = mediaBrowser.getCurrentPosition();
                            bind.lrcFragmentLrcView.updateTime(currentTime);
                        }

                        syncLyricsHandler.postDelayed(syncLyricsRunnable, 100);
                    }
                };

                syncLyricsHandler.postDelayed(syncLyricsRunnable, 100);
            } else {
                releaseHandler();
            }
        });
    }
}