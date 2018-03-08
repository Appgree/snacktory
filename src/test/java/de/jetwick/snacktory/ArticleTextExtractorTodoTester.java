package de.jetwick.snacktory;

import java.io.BufferedReader;
import java.io.FileReader;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArticleTextExtractorTodoTester {

    ArticleTextExtractor extractor;
    Converter c;

    @Before
    public void setup() throws Exception {
        c = new Converter();
        extractor = new ArticleTextExtractor();
    }

    @Test
    public void testEspn2() throws Exception {
        //String url = "http://sports.espn.go.com/golf/pgachampionship10/news/story?id=5463456";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("espn2.html")));
        assertTrue(article.getText(), article.getText().startsWith("PHILADELPHIA -- Michael Vick missed practice Thursday because of a leg injury and is unlikely to play Sunday wh"));
        assertEquals("http://a.espncdn.com/i/espn/espn_logos/espn_red.png", article.getImageUrl());
    }

    @Test
    public void testWashingtonpost() throws Exception {
        //String url = "http://www.washingtonpost.com/wp-dyn/content/article/2010/12/08/AR2010120803185.html";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("washingtonpost.html")));
        assertTrue(article.getDescription(), article.getDescription().startsWith("The Supreme Court sounded ")); 
    }

    @Test
    public void testBoingboing() throws Exception {
        //String url = "http://www.boingboing.net/2010/08/18/dr-laura-criticism-o.html";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("boingboing.html")));
        assertEquals("http://www.boingboing.net/images/drlaura.jpg", article.getImageUrl());
    }

    @Test
    public void testReadwriteWeb() throws Exception {
        //String url = "http://www.readwriteweb.com/start/2010/08/pagely-headline.php";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("readwriteweb.html")));
        assertTrue(article.getDescription(), article.getDescription().startsWith("In the heart of downtown Chandler, Arizona")); 
    }

    @Test
    public void testYahooNewsEvenThoughTheyFuckedUpDeliciousWeWillTestThemAnyway() throws Exception {
        //String url = "http://news.yahoo.com/s/ap/20110305/ap_on_re_af/af_libya";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("yahoo.html")));
        assertTrue(article.getText(), article.getText().startsWith("TRIPOLI, Libya – Government forces in tanks rolled into the opposition-held city closest ")); 
    }

    @Test
    public void testLifehacker() throws Exception {
        //String url = "http://lifehacker.com/#!5659837/build-a-rocket-stove-to-heat-your-home-with-wood-scraps";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("lifehacker.html")));
        assertEquals("http://betacache.gawkerassets.com/assets/base.v10/img/ui/oauth-facebook.png", article.getImageUrl());
    }

    @Test
    public void testNaturalhomemagazine() throws Exception {
        //String url = "http://www.naturalhomemagazine.com/diy-projects/try-this-papier-mache-ghostly-lanterns.aspx";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("naturalhomemagazine.html")));
        assertTrue(article.getText(), article.getText().startsWith("Guide trick or treaters and other friendly spirits to your front")); 
    }

    @Test
    public void testSfgate() throws Exception {
        //String url = "http://www.sfgate.com/cgi-bin/article.cgi?f=/c/a/2010/10/27/BUD61G2DBL.DTL";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("sfgate.html")));
        assertTrue(article.getText(), article.getText().startsWith("Fewer homes in California and")); 
    }

    @Test
    public void testScientificdaily() throws Exception {
        //String url = "http://www.scientificamerican.com/article.cfm?id=bpa-semen-quality";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("scientificamerican.html")));
        assertTrue(article.getText(), article.getText().startsWith("The common industrial chemical bisphenol A (BPA) "));  
    }

    @Test
    public void testUniverseToday() throws Exception {
        //String url = "http://www.universetoday.com/76881/podcast-more-from-tony-colaprete-on-lcross/";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("universetoday.html")));
        assertTrue(article.getText(), article.getText().startsWith("I had the chance to interview LCROSS"));
        assertEquals("http://www.universetoday.com/wp-content/uploads/2009/10/lcross-impact_01_01.jpg",
                article.getImageUrl());
    }

    @Test
    public void testCNBC() throws Exception {
        //String url = "http://www.cnbc.com/id/40491584";
        JResult article = extractor.extractContent(null, c.streamToString(getClass().getResourceAsStream("cnbc.html")));
        assertTrue(article.getText(), article.getText().startsWith("A prominent expert on Chinese works ")); 
    }
 
    /**
     * @param filePath the name of the file to open. Not sure if it can accept
     * URLs or just filenames. Path handling could be better, and buffer sizes
     * are hardcoded
     */
    public static String readFileAsString(String filePath)
            throws java.io.IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
}
