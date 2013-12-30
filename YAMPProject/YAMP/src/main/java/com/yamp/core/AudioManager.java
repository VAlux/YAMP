package com.yamp.core;
import com.yamp.events.PlaybackListener;
import com.yamp.events.TrackLoadedListener;
import com.yamp.events.SoundControllerBoundedListener;
import com.yamp.library.AudioFile;
import com.yamp.library.AudioLibraryManager;
import com.yamp.library.PlayList;
import com.yamp.sound.SoundController;
import com.yamp.utils.LoopButton;
import com.yamp.utils.Utilities;

import java.util.ArrayList;

/**
 * Created by AdYa on 24.11.13.
 * <p/>
 * Responses for common operations. (Activity?)
 */
public class AudioManager {


    private boolean looped = false;
    private boolean shuffle = false;

    private boolean readyToPlay = false;

    private PlayList trackList; // target playlist
    private SoundController controller;
    private static AudioManager instance;
    private AudioFile trackToPlay; /// TODO: old thing... use trackList.getCurrentTrack()

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    private AudioManager() {
        instance = this;
        YAMPApplication.setOnSoundControllerBoundedListener(new SoundControllerBoundedListener() {
            @Override
            public void onSoundControllerBounded(SoundController controller) {
               AudioManager.this.controller = controller;
                AudioManager.this.controller.setPlaybackListener(new PlaybackListener() {
                    @Override
                    public void onPlayingStarted(boolean causedByUser) {
                        notifyNewTrackLoaded(getCurrent());
                    }

                    @Override
                    public void onPlayingCompleted(boolean causedByUser) {
                        if (!AudioManager.this.controller.isLooped()) {// if track is not looped play next track.
                            next();
                            if (readyToPlay) playTrack();
                        }
                    }

                    @Override
                    public void onPlayingPaused(int currentProgress) {

                    }

                    @Override
                    public void onPlayingResumed(int currentProgress) {

                    }
                });
            }
        });
        trackList = new PlayList();
    }

    private void enablePlayingIndicator(){
        trackToPlay.setPlaying(false);
        trackToPlay = trackList.getCurrentTrack();
        trackToPlay.setPlaying(true);
        AudioLibraryManager.getInstance().notifyAllAdapters(); // TODO: such calls should be moved into one place.
    }

    private void setTrack(AudioFile track){
        String path = track.getPath();
        if (controller.isPlaying())
            controller.play(path);
        else
            controller.setTrack(path);

       // enablePlayingIndicator();
        notifyNewTrackLoaded(track);
    }

    ///TODO: solve this extra function
    public void playTrack(){
      //  enablePlayingIndicator();
        controller.play(trackList.getCurrentTrack().getPath());
        notifyNewTrackLoaded(trackList.getCurrentTrack());
    }

    public void play() {
        if (!readyToPlay){
            readyToPlay = true;
            playTrack();
        }
        controller.play(); // resume
    }
    public void pause() {
        if (isPlaying())
            controller.pause();
    }
    public void stop() {
        controller.stop();
        readyToPlay = false;
    }

    public void next() {
        if (shuffle) {
            int rnd = trackList.getCurrent();
            while(rnd == trackList.getCurrent()){
                rnd =  Utilities.randomInt(0, trackList.size()-1) - 1;
            }
            trackList.setCurrent(rnd);
        }
        notifyNextTrackLoaded(trackList.getNextTrack());
        setTrack(trackList.nextTrack());
    }
    public void prev() {
        if (shuffle) {
            int rnd = trackList.getCurrent();
            while(rnd == trackList.getCurrent()){
                rnd =  Utilities.randomInt(0, trackList.size() - 1) + 1;
            }
            trackList.setCurrent(rnd);
        }
        noyifyPrevTrackLoaded(trackList.getPrevTrack());
        setTrack(trackList.prevTrack());
    }

    public int getVolumeMax(){
        return SoundController.MAX_VOLUME;
    }
    public int getVolume(){
        return controller.getVolume();
    }
    public void setVolume(int volume) {
        controller.setVolume(volume);
    }

    public void setPlayList(PlayList playlist) {
        if (playlist != null && playlist.size() > 0)
            this.trackList = playlist;
    }

    public AudioFile getCurrent(){
        return trackList.getCurrentTrack();
    }

    public void seekTo(int msec) {
        controller.seekTo(msec);
    }
    public int getCurrentProgress(){
        return controller.getProgress();
    }

    public int getCurrentDuration(){
        return controller.getDuration();
    }

    public boolean isPlaying() {
        return controller.isPlaying();
    }

    public void setLoopMode(int loopMode) {
        switch (loopMode){
            case LoopButton.STATE_NONE:
                this.looped = false;
            case LoopButton.STATE_SINGLE:
                controller.setLooping(true);
                break;
            case LoopButton.STATE_ALL:
                controller.setLooping(false);
                this.looped = true;

        }

    }

    public int getLoopMode(){
        return (this.looped ? LoopButton.STATE_ALL : (controller.isLooped() ? LoopButton.STATE_SINGLE : LoopButton.STATE_NONE));
    }

    /**
     * Listeners ....
     **/

    private ArrayList<TrackLoadedListener> trackLoadedListeners = new ArrayList<>();
    public void setTrackLoadedListener(TrackLoadedListener listener){
        trackLoadedListeners.add(listener);
    }

    private void notifyNewTrackLoaded(AudioFile track){
        for (TrackLoadedListener listener : trackLoadedListeners){
            listener.onNewTrackLoaded(track);
        }
    }
    private void notifyNextTrackLoaded(AudioFile track){
        for (TrackLoadedListener listener : trackLoadedListeners){
            listener.onNextTrackLoaded(track);
        }
    }
    private void noyifyPrevTrackLoaded(AudioFile track){
        for (TrackLoadedListener listener : trackLoadedListeners){
            listener.onPrevTrackLoaded(track);
        }
    }

    // SoundController redirection
    public void setPlaybackListener(PlaybackListener listener) {
        controller.setPlaybackListener(listener);
    }

    public boolean isPaused() {
        return controller.isPaused();
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }
}
