package com.miller.p2p;

import android.content.Context;
import android.util.Log;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class YoutubeAPI
{
    private YouTube youtube;
    private YouTube.Search.List search_query;
    private YouTube.Playlists.List playlist_query;
    private YouTube.PlaylistItems.List items_query;
    private static final long NUMBER_OF_VIDEOS_RETURNED = 50;

    // Replace your Browser Key
    public static final String KEY = "AIzaSyDEISryny8JXMMlk5tLc_7mZE3Y-kyZ9lo";

    public YoutubeAPI(Context context)
    {
        // This object is used to make YouTube Data API requests. The last
        // argument is required, but since we don't need anything
        // initialized when the HttpRequest is initialized, we override
        // the interface and provide a no-op function.
        youtube = new YouTube.Builder(new NetHttpTransport(),
                new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest hr) throws IOException {
            }
        }).setApplicationName(context.getString(R.string.app_name))
                .build();
        try {
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("part", "snippet, contentDetails");
            parameters.put("id", "PLLdu0waEYd9IFjVgANYjdrNXdmdQw_sI3, PLY57tYhYG_uKXqG32lYy4CBVD-asGdYGJ,PLFgquLnL59amN9tYr7o2a60yFUfzQO3sU, PLDcnymzs18LU4Kexrs91TVdfnplU3I5zs, PLTDluH66q5mpm-Bsq3GlwjMOHITt2bwXE");

            playlist_query = youtube.playlists().list(parameters.get("part"));
            playlist_query.setId(parameters.get("id"));
            playlist_query.setKey(KEY);

            items_query = youtube.playlistItems().list(parameters.get("part"));
            items_query.setKey(KEY);
            items_query.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

            search_query = youtube.search().list("id,snippet");
            search_query.setKey(KEY);
            search_query.setType("video");
            search_query.setFields("items(id/videoId,snippet/title,snippet/description,snippet/thumbnails/default/url)");
            search_query.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

        } catch (IOException e) {
            Log.d("YoutubeAPI", "Could not initialize: " + e);
        }
    }

    public List<mPlayList> getPopular(){
        try {
            PlaylistListResponse response = playlist_query.execute();

            List<Playlist> playlists = response.getItems();
            List<mPlayList> result = new ArrayList<>();
            for (Playlist pl : playlists){
                mPlayList mPL = new mPlayList();
                mPL.setId(pl.getId());
                mPL.setItemCount(pl.getContentDetails().getItemCount());
                mPL.setThumbnailURL(pl.getSnippet().getThumbnails().getDefault().getUrl());
                mPL.setTitle(pl.getSnippet().getTitle());
                result.add(mPL);
            }
            return result;
        }
        catch (IOException e){
            Log.d("YoutubeAPI", "Could not search: " + e);
            return null;
        }
    }

    public List<VideoPojo> get_items(String list_id){
        items_query.setPlaylistId(list_id);
        try{
            PlaylistItemListResponse response = items_query.execute();
            List<PlaylistItem> results = response.getItems();
            Log.e("YoutubeAPI", "Lenght -" + results.size());

            List<VideoPojo> items = new ArrayList<VideoPojo>();
            for (PlaylistItem result : results) {
                VideoPojo item = new VideoPojo();
                item.setTitle(result.getSnippet().getTitle());
                if(result.getSnippet().getDescription().length() > 50) {
                    item.setDescription(result.getSnippet().getDescription().substring(0, 50));
                }
                else{
                    item.setDescription(result.getSnippet().getDescription());
                }
                item.setThumbnailURL(result.getSnippet().getThumbnails()
                        .getDefault().getUrl());
                item.setId(result.getContentDetails().getVideoId());
                items.add(item);
            }
            return items;
        }
        catch(IOException e){
            Log.d("YoutubeAPI", "Could not search: " + e);
            return null;
        }
    }
    public List<VideoPojo> search(String keywords) {

        search_query.setQ(keywords);
        try {
            SearchListResponse response = search_query.execute();

            List<SearchResult> results = response.getItems();
            Log.e("YoutubeAPI", "Lenght -" + results.size());

            List<VideoPojo> items = new ArrayList<VideoPojo>();
            for (SearchResult result : results) {
                VideoPojo item = new VideoPojo();
                item.setTitle(result.getSnippet().getTitle());
                if(result.getSnippet().getDescription().length() > 50) {
                    item.setDescription(result.getSnippet().getDescription().substring(0, 50));
                }
                else{
                    item.setDescription(result.getSnippet().getDescription());
                }
                item.setThumbnailURL(result.getSnippet().getThumbnails()
                        .getDefault().getUrl());
                item.setId(result.getId().getVideoId());
                items.add(item);
            }
            return items;
        } catch (IOException e) {
            Log.d("YoutubeAPI", "Could not search: " + e);
            return null;
        }
    }
}
