package com.aye10032.foundation.utils;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @program: communismbot
 * @className: RSSUtil
 * @Description: RSS订阅器
 * @version: v1.0
 * @author: Aye10032
 * @date: 2022/7/10 上午 11:24
 */
public class RSSUtil {

    public static List<String> getRSSUpdate(String url, Date date, boolean use_proxy) {
        List<String> result = new ArrayList<>();
        try {
            URLConnection connection;
            if (use_proxy) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080));
                connection = new URL(url).openConnection(proxy);
            } else {
                connection = new URL(url).openConnection();
            }
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(connection));
            List<SyndEntry> list = feed.getEntries();
            for (SyndEntry entry : list) {
                if (entry.getPublishedDate().compareTo(date) > 0) {
                    String builder = entry.getTitle() + "\n" +
                            "源地址：" + entry.getLink() + "\n" +
                            "种子下载：" + entry.getEnclosures().get(0).getUrl();
                    result.add(builder);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FeedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
