/*
 * Nextcloud Talk application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2018 Mario Danic <mario@lovelyhq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.talk.controllers;

import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.nextcloud.talk.R;
import com.nextcloud.talk.api.NcApi;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.controllers.base.BaseController;
import com.nextcloud.talk.models.database.UserEntity;
import com.nextcloud.talk.models.json.participants.Participant;
import com.nextcloud.talk.models.json.participants.ParticipantsOverall;
import com.nextcloud.talk.models.json.rooms.Room;
import com.nextcloud.talk.models.json.rooms.RoomsOverall;
import com.nextcloud.talk.utils.ApiUtils;
import com.nextcloud.talk.utils.bundle.BundleKeys;
import com.nextcloud.talk.utils.glide.GlideApp;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import autodagger.AutoInjector;
import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@AutoInjector(NextcloudTalkApplication.class)
public class CallNotificationController extends BaseController {

    @Inject
    NcApi ncApi;

    @BindView(R.id.conversationNameTextView)
    TextView conversationNameTextView;

    @BindView(R.id.avatarImageView)
    ImageView avatarImageView;
    List<Disposable> disposablesList = new ArrayList<>();
    private Bundle originalBundle;
    private String roomId;
    private UserEntity userBeingCalled;
    private String credentials;
    private Room currentRoom;
    private MediaPlayer mediaPlayer;
    private boolean participantsCheckIsRunning;
    private boolean leavingScreen = false;

    public CallNotificationController(Bundle args) {
        super(args);
        NextcloudTalkApplication.getSharedApplication().getComponentApplication().inject(this);

        this.roomId = args.getString(BundleKeys.KEY_ROOM_ID, "");
        this.userBeingCalled = Parcels.unwrap(args.getParcelable(BundleKeys.KEY_USER_ENTITY));

        this.originalBundle = args;

        credentials = ApiUtils.getCredentials(userBeingCalled.getUserId(), userBeingCalled.getToken());
    }

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_call_notification, container, false);
    }

    @OnClick(R.id.callControlHangupView)
    void hangup() {
        leavingScreen = true;
        getRouter().popCurrentController();
    }

    @OnClick(R.id.callAnswerCameraView)
    void answerWithCamera() {
        originalBundle.putBoolean(BundleKeys.KEY_CALL_VOICE_ONLY, false);
        setBackstackAndProceed();
    }

    @OnClick(R.id.callAnswerVoiceOnlyView)
    void answerVoiceOnly() {
        originalBundle.putBoolean(BundleKeys.KEY_CALL_VOICE_ONLY, true);
        setBackstackAndProceed();
    }

    private void setBackstackAndProceed() {
        originalBundle.putString(BundleKeys.KEY_ROOM_TOKEN, currentRoom.getToken());

        List<RouterTransaction> routerTransactions = new ArrayList<>();
        routerTransactions.add(RouterTransaction.with(new MagicBottomNavigationController()));
        routerTransactions.add(RouterTransaction.with(new ChatController(originalBundle)));
        getRouter().setBackstack(routerTransactions, new HorizontalChangeHandler());
    }

    private void checkIfAnyParticipantsRemainInRoom() {
        ncApi.getPeersForCall(credentials, ApiUtils.getUrlForParticipants(userBeingCalled.getBaseUrl(),
                currentRoom.getToken()))
                .subscribeOn(Schedulers.newThread())
                .takeWhile(observable -> !leavingScreen)
                .retry(3)
                .subscribe(new Observer<ParticipantsOverall>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposablesList.add(d);
                        participantsCheckIsRunning = true;
                    }

                    @Override
                    public void onNext(ParticipantsOverall participantsOverall) {
                        boolean hasParticipantsInCall = false;
                        List<Participant> participantList = participantsOverall.getOcs().getData();
                        for (Participant participant : participantList) {
                            if (participant.isInCall()) {
                                hasParticipantsInCall = true;
                                break;
                            }
                        }

                        if (!hasParticipantsInCall) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> hangup());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        if (!leavingScreen) {
                            checkIfAnyParticipantsRemainInRoom();
                        }
                    }
                });

    }

    private void handleFromNotification() {
        ncApi.getRooms(credentials, ApiUtils.getUrlForGetRooms(userBeingCalled.getBaseUrl()))
                .subscribeOn(Schedulers.newThread())
                .retry(3)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RoomsOverall>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposablesList.add(d);
                    }

                    @Override
                    public void onNext(RoomsOverall roomsOverall) {
                        for (Room room : roomsOverall.getOcs().getData()) {
                            if (roomId.equals(room.getRoomId())) {
                                currentRoom = room;
                                conversationNameTextView.setText(room.getDisplayName());
                                loadAvatar();
                                checkIfAnyParticipantsRemainInRoom();
                                break;
                            }
                        }

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);

        getActionBar().hide();

        handleFromNotification();

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        mediaPlayer = MediaPlayer.create(getApplicationContext(), ringtoneUri);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void loadAvatar() {
        int avatarSize = Math.round(NextcloudTalkApplication
                .getSharedApplication().getResources().getDimension(R.dimen.avatar_size_big));

        switch (currentRoom.getType()) {
            case ROOM_TYPE_ONE_TO_ONE_CALL:
                avatarImageView.setVisibility(View.VISIBLE);

                GlideUrl glideUrl = new GlideUrl(ApiUtils.getUrlForAvatarWithName(userBeingCalled.getBaseUrl(),
                        currentRoom.getName(), true), new LazyHeaders.Builder()
                        .setHeader("Accept", "image/*")
                        .setHeader("User-Agent", ApiUtils.getUserAgent())
                        .build());

                GlideApp.with(NextcloudTalkApplication.getSharedApplication().getApplicationContext())
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .load(glideUrl)
                        .centerInside()
                        .override(avatarSize, avatarSize)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(avatarImageView);

                break;
            case ROOM_GROUP_CALL:
                GlideApp.with(NextcloudTalkApplication.getSharedApplication().getApplicationContext())
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .load(R.drawable.ic_group_white_24px)
                        .centerInside()
                        .override(avatarSize, avatarSize)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(avatarImageView);
            case ROOM_PUBLIC_CALL:
                GlideApp.with(NextcloudTalkApplication.getSharedApplication().getApplicationContext())
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .load(R.drawable.ic_link_white_24px)
                        .centerInside()
                        .override(avatarSize, avatarSize)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(avatarImageView);
                break;
            default:
        }
    }

    private void endMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        leavingScreen = true;
        dispose();
        endMediaPlayer();
        super.onDestroy();
    }

    private void dispose() {
        Disposable disposable;
        for (int i = 0; i < disposablesList.size(); i++) {
            if ((disposable = disposablesList.get(i)).isDisposed()) {
                disposable.dispose();
            }
        }
    }
}