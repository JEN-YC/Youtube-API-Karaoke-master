package com.miller.p2p;

public class mPlayList {
    private String title;
    private String id;
    private String thumbnailURL;
    private Long itemCount;

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }
}
