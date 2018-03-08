package de.jetwick.snacktory;
  
import org.junit.Test;

public class TestAppgreeScrapper {

    @Test
    public void test() throws Exception {
    	
        long startTime = System.currentTimeMillis();
        HtmlFetcher fetcher = new HtmlFetcher();

        //String urlToParser = "https://www.evobanco.com/productos-inteligentes/prevision-y-seguros/plan-inversion-inteligente/?v=1"";
        //String urlToParser = "https://elpais.com/";
        //String urlToParser = "https://verne.elpais.com/especial/el-camino-de-la-igualdad/";
        String urlToParser = "http://www.midgetmanofsteel.com/2011/03/its-only-matter-of-time-before-fox-news.html";
        
        JResult article = fetcher.fetchAndExtract(urlToParser, 10000, true);

        long elapsedTime = System.currentTimeMillis() - startTime;
          
        System.out.println("");
        System.out.println("URL: " + article.getUrl()); 
        System.out.println("Title: " + article.getTitle());
        System.out.println("Description: " + article.getDescription());
        System.out.println("Recommended image: " + (article.getImageUrl().isEmpty() ? "Not detected" : article.getImageUrl()));
        System.out.println("All images detected: ");
        if (!article.getImages().isEmpty()) {
            System.out.println("");
            int i = 1;
	        for (ImageResult img : article.getImages()) {
	            System.out.println("\t" + i + " " + img.src);
	            System.out.println("");
	            i++;
	        }
        }  
        System.out.println("Scapped in: " + elapsedTime + " ms.");
    }
}
