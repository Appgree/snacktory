package de.jetwick.snacktory;

import java.net.URL;

import org.junit.Test;

public class TestAppgreeScrapper {

    @Test
    public void test() throws Exception {
        long startTime = System.currentTimeMillis();
        HtmlFetcher fetcher = new HtmlFetcher();

        JResult article = fetcher.fetchAndExtract("http://download.wavetlan.com/SVV/Media/HTTP/mkv/H264_mp3(mkvmerge).mkv", 10000, true);

        long elapsedTime = System.currentTimeMillis() - startTime;
        
        URL url = new URL(article.getCanonicalUrl());
        System.out.println("URL:" + url.toString());
        System.out.println("Domain:" + url.getHost());
        System.out.println("Title:" + article.getTitle());
        System.out.println("Image:" + article.getImageUrl());
        for (ImageResult img : article.getImages()) {
            System.out.println("\t -Image:" + img.src);
        }
        System.out.println("Description:" + article.getDescription());
        System.out.println("Scapped in: " + elapsedTime + " ms.");

    }

}
