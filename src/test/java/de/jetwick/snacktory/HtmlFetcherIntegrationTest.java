/*
 *  Copyright 2011 Peter Karich 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetwick.snacktory;

import org.junit.Test;
import static org.junit.Assert.*;

public class HtmlFetcherIntegrationTest {

    public HtmlFetcherIntegrationTest() {
    }
  
    @Test
    public void testWithTitle() throws Exception {
    	
	    	JResult res = new HtmlFetcher().fetchAndExtract("http://www.midgetmanofsteel.com/2011/03/its-only-matter-of-time-before-fox-news.html", 10000, true);
	    	assertEquals("It's Only a Matter of Time Before Fox News Takes Out a Restraining Order", res.getTitle());
	    	assertEquals("2011/03", res.getDate());
    }
     
    @Test
    public void testEncoding() throws Exception {
    	
	    	JResult res = new HtmlFetcher().fetchAndExtract("http://www.yomiuri.co.jp/science/20180308-OYT1T50066.html?from=ycont_top_txt", 10000, true);
	    	assertEquals("新燃岳で噴火継続、火口内が円形に盛り上がる : 科学・ＩＴ : 読売新聞（YOMIURI ONLINE）", res.getTitle());
    }
    
    @Test
    public void testImage() throws Exception {
    	
        JResult res = new HtmlFetcher().fetchAndExtract("http://grfx.cstv.com/schools/okla/graphics/auto/20110505_schedule.jpg", 10000, true);
        assertEquals("http://grfx.cstv.com/schools/okla/graphics/auto/20110505_schedule.jpg", res.getImageUrl());
        assertTrue(res.getTitle().isEmpty());
        assertTrue(res.getText().isEmpty());
    }
    
    @Test
    public void testHashbang() throws Exception {
    	
        JResult res = new HtmlFetcher().fetchAndExtract("http://www.facebook.com/democracynow", 10000, true);
        assertTrue(res.getTitle(), res.getTitle().startsWith("Democracy Now! ")); 
    } 
    
    @Test
    public void testDoubleResolve() throws Exception {
    	
        JResult res = new HtmlFetcher().fetchAndExtract("http://t.co/eZRKcEYI", 10000, true);
        assertTrue(res.getTitle(), res.getTitle().contains("teleject/Responsive-Web-Design-Artboards"));
    }
    
    @Test
    public void testRedirection() throws Exception {
    	
    		JResult res = new HtmlFetcher().fetchAndExtract("http://www.tumblr.com/xeb22gs619", 10000, true);
        assertTrue(res.getTitle(), res.getTitle().startsWith("KILAUM | The video"));
    }
    
    @Test
    public void testExceptionsControlled() throws Exception {
    	
    		// Always return the URL original
    	
    		JResult res = new HtmlFetcher().fetchAndExtract("http://bit.ly/gyFxfv", 10000, true);
        assertEquals("http://bit.ly/gyFxfv", res.getUrl());
        
        res = new HtmlFetcher().fetchAndExtract("http://www.faz.net/-01s7fc", 10000, true);
        assertEquals("http://www.faz.net/-01s7fc", res.getUrl()); 
    }
    
    @Test
    public void testNoException() throws Exception { 
    		JResult res = new HtmlFetcher().fetchAndExtract("http://www.google.com/url?sa=x&q=http://www.taz.de/1/politik/asien/artikel/1/anti-atomkraft-nein-danke/&ct=ga&cad=caeqargbiaaoataaoabaltmh7qrialaawabibwrllurf&cd=d5glzns5m_4&usg=afqjcnetx___sph8sjwhjwi-_mmdnhilra&utm_source=twitterfeed&utm_medium=twitter", 10000, true);
        assertEquals("http://www.taz.de/1/politik/asien/artikel/1/anti-atomkraft-nein-danke/", res.getUrl());  
    }
}
