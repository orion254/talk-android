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
package com.nextcloud.talk.utils;

import android.net.Uri;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;

import com.nextcloud.talk.BuildConfig;
import com.nextcloud.talk.R;
import com.nextcloud.talk.application.NextcloudTalkApplication;
import com.nextcloud.talk.models.RetrofitBucket;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Credentials;

public class ApiUtils {
    private static String ocsApiVersion = "/ocs/v2.php";
    private static String spreedApiVersion = "/apps/spreed/api/v1";

    private static String userAgent = "Mozilla/5.0 (Android) Nextcloud-Talk v";

    public static String getUserAgent() {
        return userAgent + BuildConfig.VERSION_NAME;
    }

    public static RetrofitBucket getRetrofitBucketForContactsSearch(String baseUrl, @Nullable String searchQuery) {
        RetrofitBucket retrofitBucket = new RetrofitBucket();
        retrofitBucket.setUrl(baseUrl + ocsApiVersion + "/apps/files_sharing/api/v1/sharees");

        Map<String, String> queryMap = new HashMap<>();

        if (searchQuery == null) {
            searchQuery = "";
        }
        queryMap.put("format", "json");
        queryMap.put("search", searchQuery);
        queryMap.put("itemType", "call");

        retrofitBucket.setQueryMap(queryMap);

        return retrofitBucket;
    }

    public static String getUrlForSettingMyselfAsActiveParticipant(String baseUrl, String token) {
        return getRoom(baseUrl, token) + "/participants/active";
    }


    public static String getUrlForParticipants(String baseUrl, String token) {
        return getRoom(baseUrl, token) + "/participants";
    }

    public static String getUrlForCapabilities(String baseUrl) {
        return baseUrl + ocsApiVersion + "/cloud/capabilities";
    }

    public static String getUrlForGetRooms(String baseUrl) {
        return baseUrl + ocsApiVersion + spreedApiVersion + "/room";
    }

    public static String getRoom(String baseUrl, String token) {
        return baseUrl + ocsApiVersion + spreedApiVersion + "/room/" + token;
    }

    public static RetrofitBucket getRetrofitBucketForCreateRoom(String baseUrl, String roomType,
                                                                @Nullable String invite,
                                                                @Nullable String conversationName) {
        RetrofitBucket retrofitBucket = new RetrofitBucket();
        retrofitBucket.setUrl(baseUrl + ocsApiVersion + spreedApiVersion + "/room");
        Map<String, String> queryMap = new HashMap<>();

        queryMap.put("roomType", roomType);
        if (invite != null) {
            queryMap.put("invite", invite);
        }

        if (conversationName != null) {
            queryMap.put("roomName", conversationName);
        }

        retrofitBucket.setQueryMap(queryMap);

        return retrofitBucket;
    }

    public static RetrofitBucket getRetrofitBucketForAddParticipant(String baseUrl, String token, String user) {
        RetrofitBucket retrofitBucket = new RetrofitBucket();
        retrofitBucket.setUrl(baseUrl + ocsApiVersion + spreedApiVersion + "/room/" + token + "/participants");

        Map<String, String> queryMap = new HashMap<>();

        queryMap.put("newParticipant", user);

        retrofitBucket.setQueryMap(queryMap);

        return retrofitBucket;

    }

    public static String getUrlForRemoveSelfFromRoom(String baseUrl, String token) {
        return baseUrl + ocsApiVersion + spreedApiVersion + "/room/" + token + "/participants/self";
    }

    public static String getUrlForRoomVisibility(String baseUrl, String token) {
        return baseUrl + ocsApiVersion + spreedApiVersion + "/room/" + token + "/public";
    }

    public static String getUrlForCall(String baseUrl, String token) {
        return baseUrl + ocsApiVersion + spreedApiVersion + "/call/" + token;

    }

    public static String getUrlForCallPing(String baseUrl, String token) {
        return getUrlForCall(baseUrl, token) + "/ping";
    }

    public static String getUrlForChat(String baseUrl, String token) {
        return baseUrl + ocsApiVersion + spreedApiVersion + "/chat/" + token;
    }

    public static String getUrlForMentionSuggestions(String baseUrl, String token) {
        return getUrlForChat(baseUrl, token) + "/mentions";
    }

    public static String getUrlForSignaling(String baseUrl, @Nullable String token) {
        String signalingUrl = baseUrl + ocsApiVersion + spreedApiVersion + "/signaling";
        if (token == null) {
            return signalingUrl;
        } else {
            return signalingUrl + "/" + token;
        }
    }

    public static String getUrlForSignalingSettings(String baseUrl) {
        return getUrlForSignaling(baseUrl, null) + "/settings";
    }


    public static String getUrlForUserProfile(String baseUrl) {
        return baseUrl + ocsApiVersion + "/cloud/user";
    }

    public static String getUrlPostfixForStatus() {
        return "/status.php";
    }

    public static String getUrlForAvatarWithName(String baseUrl, String name, @DimenRes int avatarSize) {
        avatarSize = Math.round(NextcloudTalkApplication
                                    .getSharedApplication().getResources().getDimension(avatarSize));

        return baseUrl + "/index.php/avatar/" + Uri.encode(name) + "/" + avatarSize;
    }

    public static String getUrlForPassword(String baseUrl, String token) {
        return baseUrl + ocsApiVersion + spreedApiVersion + "/room/" + token + "/password";
    }

    public static String getCredentials(String username, String token) {
        return Credentials.basic(username, token);
    }

    public static String getUrlNextcloudPush(String baseUrl) {
        return baseUrl + ocsApiVersion + "/apps/notifications/api/v2/push";
    }

    public static String getUrlPushProxy() {
        return NextcloudTalkApplication.getSharedApplication().
                getApplicationContext().getResources().getString(R.string.nc_push_server_url) + "/devices";
    }

    public static String getUrlForConversationPin(String baseUrl, String roomToken) {
        return baseUrl + ocsApiVersion + "/room/" + roomToken + "/pin";
    }
}
