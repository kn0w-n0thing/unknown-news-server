package org.chronusartcenter.news;

import com.alibaba.fastjson.annotation.JSONField;

public class HeadlineModel {
    public enum Language {
        ZH,
        EN,
    }

    @JSONField(name = "index")
    private int index = -1;
    @JSONField(name = "lang")
    private Language language = Language.ZH;

    @JSONField(name = "title")
    private String title;

    @JSONField(name = "translation")
    private String translation;

    @JSONField(name = "author")
    private String author;

    @JSONField(name = "publishDate")
    private String publishDate;

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
