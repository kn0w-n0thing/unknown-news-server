package org.chronusartcenter.news;

import org.chronusartcenter.Context;
import org.chronusartcenter.network.OkHttpWrapper;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

public class NewsService {
    private final Context context;

    public NewsService(Context globalContext) {
        this.context = globalContext;
    }

    private String getRssUrl() throws Exception {
        try {
            return context.loadConfig().getString("rssUrl");
        } catch (Exception exception) {
            throw new Exception("Fail to get RSS URL!");
        }
    }

    public List<HeadlineModel> fetchHeadlines() {
        OkHttpWrapper okHttpWrapper = new OkHttpWrapper();
        List<HeadlineModel> resultList = new LinkedList<>();
        try {
            var response = okHttpWrapper.get(getRssUrl());
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(new StringReader(response));
            HeadlineModel headlineItem = new HeadlineModel();
            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.nextEvent();
                if (nextEvent.isStartElement()) {
                    StartElement startElement = nextEvent.asStartElement();
                    switch (startElement.getName().getLocalPart()) {
                        case "item":
                            headlineItem = new HeadlineModel();
                            headlineItem.setLanguage(HeadlineModel.Language.ZH);
                            break;
                        case "title":
                            nextEvent = reader.nextEvent();
                            headlineItem.setTitle(nextEvent.asCharacters().getData());
                            break;
                        case "author":
                            nextEvent = reader.nextEvent();
                            headlineItem.setAuthor(nextEvent.asCharacters().getData());
                            break;
                        case "pubDate":
                            nextEvent = reader.nextEvent();
                            headlineItem.setPublishDate(nextEvent.asCharacters().getData());
                            break;
                    }
                }
                if (nextEvent.isEndElement()) {
                    EndElement endElement = nextEvent.asEndElement();
                    if (endElement.getName().getLocalPart().equals("item")) {
                        resultList.add(headlineItem);
                    }
                }
            }
        } catch (Exception exception) {
            // TODO: log
        }

        return resultList;
    }

    public List<HeadlineModel> translateHeadlines(List<HeadlineModel> headlineItemList) {
        BaiduTranslate baiduTranslate = new BaiduTranslate(context);
        var translateTitleList
                = baiduTranslate.translate(headlineItemList.stream().map(HeadlineModel::getTitle).toList());
        for (int i = 0; i < headlineItemList.size(); i++) {
            headlineItemList.get(i).setTranslation(translateTitleList.get(i));
        }
        return headlineItemList.stream().filter(
                headlineItem -> {
                    return
                            !headlineItem.getTranslation().isEmpty() && !headlineItem.getTranslation().equals(" ");
                }).toList();

    }
}
