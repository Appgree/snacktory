package de.jetwick.snacktory;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List; 
import java.util.Map; 
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is thread safe.
 *
 * @author Alex P (ifesdjeen from jreadability)
 * @author Peter Karich
 */
public class ArticleTextExtractor {

    private static final int MAX_IMAGE_FROM_BODY = 30;
    private static final int MIN_IMAGE_PIXELS = 5000;
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(ArticleTextExtractor.class);
    // Interessting nodes
    private static final Pattern NODES = Pattern.compile("p|div|td|h1|h2|article|section");
    // Unlikely candidates
    private String unlikelyStr;
    private Pattern UNLIKELY;
    // Most likely positive candidates
    private String positiveStr;
    private Pattern POSITIVE;
    // Most likely negative candidates
    private String negativeStr;
    private Pattern NEGATIVE;
    private static final Pattern NEGATIVE_STYLE = Pattern.compile("hidden|display: ?none|font-size: ?small");
    private static final Pattern IMAGE_SIZE = Pattern.compile("[_-](\\d{1,4})[x_-](\\d{1,4})");
    private static final Pattern CANVAS_IMAGE = Pattern.compile("data:image/|base64,");
    @SuppressWarnings("serial")
    private static final LinkedHashSet<String> IGNORED_TITLE_PARTS = new LinkedHashSet<String>() {
        {
            add("hacker news");
            add("facebook");
        }
    };

    private static final OutputFormatter DEFAULT_FORMATTER = new OutputFormatter();
    private OutputFormatter formatter = DEFAULT_FORMATTER;
    private static final NumberFormat DECIMAL_PARSER = NumberFormat.getInstance();

    public ArticleTextExtractor() {
        setUnlikely("com(bx|ment|munity)|dis(qus|cuss)|e(xtra|[-]?mail)|foot|" + "header|menu|re(mark|ply)|rss|sh(are|outbox)|sponsor"
                        + "a(d|ll|gegate|rchive|ttachment)|(pag(er|ination))|popup|print|" + "login|si(debar|gn|ngle)");
        setPositive("(^(body|content|h?entry|main|page|post|text|blog|story|haupt|contenido))" + "|arti(cle|kel|culo)|instapaper_body");
        setNegative("nav($|igation)|user|com(ment|bx)|(^com-)|contact|"
                        + "foot|pie|masthead|(me(dia|ta))|outbrain|promo|related|scroll|(sho(utbox|pping))|"
                        + "sidebar|sponsor|tags|tool|widget|player|disclaimer|toc|infobox|vcard");
    }

    public ArticleTextExtractor setUnlikely(String unlikelyStr) {
        this.unlikelyStr = unlikelyStr;
        UNLIKELY = Pattern.compile(unlikelyStr);
        return this;
    }

    public ArticleTextExtractor addUnlikely(String unlikelyMatches) {
        return setUnlikely(unlikelyStr + "|" + unlikelyMatches);
    }

    public ArticleTextExtractor setPositive(String positiveStr) {
        this.positiveStr = positiveStr;
        POSITIVE = Pattern.compile(positiveStr);
        return this;
    }

    public ArticleTextExtractor addPositive(String pos) {
        return setPositive(positiveStr + "|" + pos);
    }

    public ArticleTextExtractor setNegative(String negativeStr) {
        this.negativeStr = negativeStr;
        NEGATIVE = Pattern.compile(negativeStr);
        return this;
    }

    public ArticleTextExtractor addNegative(String neg) {
        setNegative(negativeStr + "|" + neg);
        return this;
    }

    public void setOutputFormatter(OutputFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @param html extracts article text from given html string. wasn't tested with improper HTML, although jSoup should be able to handle minor
     *        stuff.
     * @returns extracted article, all HTML tags stripped
     */
    public JResult extractContent(String url, Document doc) throws Exception {
        return extractContent(url, new JResult(), doc, formatter);
    }

    public JResult extractContent(String url, Document doc, OutputFormatter formatter) throws Exception {
        return extractContent(url,new JResult(), doc, formatter);
    }

    public JResult extractContent(String url, String html) throws Exception {
        return extractContent(url, new JResult(), html);
    }

    public JResult extractContent(String url, JResult res, String html) throws Exception {
        return extractContent(url, res, html, formatter);
    }

    public JResult extractContent(String url, JResult res, String html, OutputFormatter formatter) throws Exception {
        if (html.isEmpty())
            throw new IllegalArgumentException("html string is empty!?");

        // http://jsoup.org/cookbook/extracting-data/selector-syntax
        return extractContent(url, res, Jsoup.parse(html), formatter);
    }

    public JResult extractContent(String url, JResult res, Document doc, OutputFormatter formatter) throws Exception {
    	
        if (doc == null)
            throw new NullPointerException("missing document");

        if (url.contains("?")) {
        		url = url.substring(0, url.indexOf("?"));
        }
        
        res.setTitle(extractTitle(doc));
        res.setDescription(extractDescription(doc));
        res.setCanonicalUrl(extractCanonicalUrl(doc));

        // now remove the clutter
        prepareDocument(doc);

        // init elements
        Collection<Element> nodes = getNodes(doc);
        int maxWeight = 0;
        res.setImageUrl(extractImageUrl(url, doc));
        Element bestMatchElement = null;
        for (Element entry : nodes) {
            int currentWeight = getWeight(entry);
            if (currentWeight > maxWeight) {
                maxWeight = currentWeight;
                bestMatchElement = entry;
                if (maxWeight > 200)
                    break;
            }
        }

        if (bestMatchElement != null) {
            List<ImageResult> images = new ArrayList<>();
            int pixels = guessImageSize(res.getImageUrl());
            if (isTooSmall(pixels) || res.getImageUrl().isEmpty()) {
                Element imgEl = determineImageSource(bestMatchElement, images);
                if (imgEl != null && !images.isEmpty()) {
                    if (res.getImageUrl().isEmpty() || images.get(0).weight > pixels) {
                        res.setImageUrl(SHelper.replaceSpaces(imgEl.attr("src")));
                    }
                }
            }

            // clean before grabbing text
            String text = formatter.getFormattedText(bestMatchElement);
            text = removeTitleFromText(text, res.getTitle());
            // this fails for short facebook post and probably tweets: text.length() > res.getDescription().length()
            if (text.length() > res.getTitle().length()) {
                res.setText(text);
            }
            res.setTextList(formatter.getTextList(bestMatchElement));
        }
        
        Map<String, ImageResult> imagesResults = extractAllImages(url, doc);
        if (!imagesResults.isEmpty()) {
        		 
        		String imageUrlKey = res.getImageUrl();
	        	if (StringUtils.isEmpty(imageUrlKey)) { 
	        		imageUrlKey = imagesResults.keySet().iterator().next();
	        		res.setImageUrl(imageUrlKey);
	        	}  
	        	imagesResults.remove(imageUrlKey);
	        	
	        	ArrayList<ImageResult> toOrder = new ArrayList<>(imagesResults.values());
	        	
	        	Collections.sort(toOrder, new ImageComparator());
	        	
        		res.setImages(toOrder);
        }
        
        res.setRssUrl(extractRssUrl(doc));
        res.setVideoUrl(extractVideoUrl(doc));
        res.setFaviconUrl(extractFaviconUrl(url, doc));
        res.setKeywords(extractKeywords(doc));
        return res;
    }

    public boolean isTooSmall(int pixels) {
        return pixels > 0 && pixels < MIN_IMAGE_PIXELS;
    }
  
    private Integer getIntegerValueForAttr(Element e, String attr) throws MalformedURLException {
    		 
    		Integer result = 0;
    		
    		if (e.hasAttr(attr)) {
    			try {
    				result = Integer.parseInt(e.attr(attr).replaceAll("px", "").replaceAll(";", ""));
    			} catch (Exception ex) { }   
    		}
    		 
        return result;
    }

    private int guessImageSize(String imageUrl) throws MalformedURLException {
        if (imageUrl == null || imageUrl.isEmpty() || !imageUrl.contains("http")) {
            return 0;
        }
        URL url = new URL(imageUrl);
        int startImage = url.getPath().lastIndexOf('/');
        int endImage = url.getPath().lastIndexOf('.');
        if (endImage <= startImage) {
            return 0;
        }
        String imageName = url.getPath().substring(startImage, endImage);
        if (imageName == null || imageName.isEmpty()) {
            return 0;
        }
        Matcher matcher = IMAGE_SIZE.matcher(imageName);
        if (!matcher.find()) {
            return 0;
        }
        int width = Integer.parseInt(matcher.group(1));
        int height = Integer.parseInt(matcher.group(2));

        return width * height;
    }

    protected String extractTitle(Document doc) {
        String title = cleanTitle(doc.title());
        if (title.isEmpty()) {
            title = SHelper.innerTrim(doc.select("head title").text());
            if (title.isEmpty()) {
                title = SHelper.innerTrim(doc.select("head meta[name=title]").attr("content"));
                if (title.isEmpty()) {
                    title = SHelper.innerTrim(doc.select("head meta[property=og:title]").attr("content"));
                    if (title.isEmpty()) {
                        title = SHelper.innerTrim(doc.select("head meta[name=twitter:title]").attr("content"));
                    }
                }
            }
        }
        return title;
    }

    protected String extractCanonicalUrl(Document doc) {
        String url = SHelper.replaceSpaces(doc.select("head link[rel=canonical]").attr("href"));
        if (url.isEmpty()) {
            url = SHelper.replaceSpaces(doc.select("head meta[property=og:url]").attr("content"));
            if (url.isEmpty()) {
                url = SHelper.replaceSpaces(doc.select("head meta[name=twitter:url]").attr("content"));
            }
        }
        return url;
    }

    protected String extractDescription(Document doc) {
        String description = SHelper.innerTrim(doc.select("head meta[name=description]").attr("content"));
        if (description.isEmpty()) {
            description = SHelper.innerTrim(doc.select("head meta[property=og:description]").attr("content"));
            if (description.isEmpty()) {
                description = SHelper.innerTrim(doc.select("head meta[name=twitter:description]").attr("content"));
            }
        }
        return description;
    }

    protected Collection<String> extractKeywords(Document doc) {
        String content = SHelper.innerTrim(doc.select("head meta[name=keywords]").attr("content"));

        if (content != null) {
            if (content.startsWith("[") && content.endsWith("]"))
                content = content.substring(1, content.length() - 1);

            String[] split = content.split("\\s*,\\s*");
            if (split.length > 1 || (split.length > 0 && !"".equals(split[0])))
                return Arrays.asList(split);
        }
        return Collections.emptyList();
    }

    /**
     * Tries to extract an image url from metadata if determineImageSource failed
     *
     * @return image url or empty str
     */
    protected String extractImageUrl(String url, Document doc) {

        String imageUrl = SHelper.replaceSpaces(doc.select("head meta[property=og:image]").attr("content"));
        if (imageUrl.isEmpty()) {
            imageUrl = SHelper.replaceSpaces(doc.select("head meta[name=twitter:image]").attr("content"));
            if (imageUrl.isEmpty()) {
                imageUrl = SHelper.replaceSpaces(doc.select("link[rel=image_src]").attr("abs:href"));
                if (imageUrl.isEmpty()) {
                    imageUrl = SHelper.replaceSpaces(doc.select("head meta[name=thumbnail]").attr("content"));
                    if (imageUrl.isEmpty()) {
	            			Elements e = doc.select("link[href*=.png]");
	            			if (e != null && e.size() > 0) {
	            				imageUrl = e.first().attr("href");
	            			} else {
	            				e = doc.select("link[href*=.jpg]");
	            				if (e != null && e.size() > 0) {
	                				imageUrl = e.first().attr("href");
	                			}
	            			}
	            		}
                }
            }
        }
        
        return parseSrcImage(url, imageUrl);
    }
    
    protected HashMap<String, Element> extractAllImageUrl(String url, Document doc) {
    	  
    		HashMap<String, Element> result = new HashMap<>();
    		
        Elements eMetaOgImage = doc.select("head meta[property=og:image]");
        if (!eMetaOgImage.isEmpty()) {
        		for (Element element : eMetaOgImage) {
        			String key = parseSrcImage(url, SHelper.replaceSpaces(element.attr("content"))); 
        			if (!StringUtils.isEmpty(key) && !result.containsKey(key) && (element.attr("content").contains(".jpg") || element.attr("content").contains(".png"))) {
        				result.put(key,element);
        			}
			} 
        }
        
        Elements eMetaTwitterImage = doc.select("head meta[name=twitter:image]");
        if (!eMetaTwitterImage.isEmpty()) {
	    		for (Element element : eMetaTwitterImage) { 
	    			String key = parseSrcImage(url, SHelper.replaceSpaces(element.attr("content"))); 
	    			if (!StringUtils.isEmpty(key)  && !result.containsKey(key) && (element.attr("content").contains(".jpg") || element.attr("content").contains(".png"))) {
        				result.put(key,element);
        			}
			} 
	    }
        
        Elements eMetaThumbnailImage = doc.select("head meta[name=thumbnail]");
        if (!eMetaThumbnailImage.isEmpty()) {
	    		for (Element element : eMetaThumbnailImage) {
	    			String key = parseSrcImage(url, SHelper.replaceSpaces(element.attr("content"))); 
	    			if (!StringUtils.isEmpty(key)  && !result.containsKey(key) && (element.attr("content").contains(".jpg") || element.attr("content").contains(".png"))) {
        				result.put(key,element);
        			}
			} 
	    }
        
        Elements eMetaItemPropImage = doc.select("meta[itemprop=url]");
        if (!eMetaItemPropImage.isEmpty()) {
	    		for (Element element : eMetaItemPropImage) {
	    			String key = parseSrcImage(url, SHelper.replaceSpaces(element.attr("content"))); 
	    			if (!StringUtils.isEmpty(key)  && !result.containsKey(key) && (element.attr("content").contains(".jpg") || element.attr("content").contains(".png"))) {
        				result.put(key,element);
        			}
			} 
	    }
        
        Elements eMetaImgSrcImagePng = doc.select("link[href*=.png]"); 
        if (!eMetaImgSrcImagePng.isEmpty()) {
	    		for (Element element : eMetaImgSrcImagePng) {  
	    			String key = parseSrcImage(url, SHelper.replaceSpaces(element.attr("href"))); 
	    			if (!StringUtils.isEmpty(key)  && !result.containsKey(key) && (element.attr("content").contains(".jpg") || element.attr("content").contains(".png"))) {
        				result.put(key,element);
        			}
			} 
	    }
        
        Elements eMetaImgSrcImageJpg = doc.select("link[href*=.jpg]"); 
        if (!eMetaImgSrcImageJpg.isEmpty()) {
	    		for (Element element : eMetaImgSrcImageJpg) {
	    			String key = parseSrcImage(url, SHelper.replaceSpaces(element.attr("href"))); 
	    			if (!StringUtils.isEmpty(key)  && !result.containsKey(key) && (element.attr("content").contains(".jpg") || element.attr("content").contains(".png"))) {
        				result.put(key,element);
        			}
			} 
	    }
        
        Elements eImageBodyImageJpg = doc.select("body img[src*=.jpg]");
        Elements eImageBodyImagePng = doc.select("body img[src*=.png]");
        Elements eImageBodyImageDataSrcJpg = doc.select("body img[data-src*=.jpg]");
        Elements eImageBodyImageDataSrcPng = doc.select("body img[data-src*=.png]");
        Elements allElements = new Elements(eImageBodyImageJpg);
        allElements.addAll(eImageBodyImagePng);
        allElements.addAll(eImageBodyImageDataSrcJpg);
        allElements.addAll(eImageBodyImageDataSrcPng);
        
        if (!allElements.isEmpty()) {

        		int totalImageFromBody = 1;
        		
	    		for (Element element : allElements) {
	    			
	    			String src = SHelper.replaceSpaces(element.attr("src"));
	    				
	    			if ((!src.contains(".jpg") || !src.contains(".png")) &&
	    				element.hasAttr("data-src") && 
	    				(element.attr("data-src").contains(".jpg") || element.attr("data-src").contains(".png"))) {
	    				src = SHelper.replaceSpaces(element.attr("data-src"));
	    			}
	    			
	    			if (StringUtils.isEmpty(src))
	    				continue;
	    			 
	    			src = parseSrcImage(url, src); 
	    			
	    			if (!StringUtils.isEmpty(src) && !result.containsKey(src)) {
	    				
		    			if (totalImageFromBody >= MAX_IMAGE_FROM_BODY) {
		    				
		    				return result;
		    				
		    			} else {
		    				  
		    				result.put(src,element);
		    				
		    				totalImageFromBody++;
		    			}
	    			}
			} 
	    }
      
        return result;
    }
    
    protected Map<String, ImageResult> extractAllImages(String url, Document doc) throws MalformedURLException {
    	   
        Map<String, ImageResult> imagesResultList = new HashMap<>();

        HashMap<String, Element> imageElements = extractAllImageUrl(url, doc);    
        Iterator<Entry<String, Element>> e = imageElements.entrySet().iterator();
        
        while (e.hasNext()) {
        	
            Map.Entry<String, Element> elementImg = (Map.Entry<String, Element>) e.next(); 
             
	        String src = elementImg.getKey();
	    		Integer weight = getWeight(elementImg.getValue());
	    		String title = null; 
	    		String alt = null;
	    		boolean noFollow = false;
	    		Integer width = getIntegerValueForAttr(elementImg.getValue(), "width");
	    		Integer height = getIntegerValueForAttr(elementImg.getValue(), "height");
		
	    		ImageResult ir = new ImageResult(src, weight, title, height, width, alt, noFollow);
        
	    		imagesResultList.put(ir.src, ir);
        }
        
        return imagesResultList;
    }
    
    protected String extractRssUrl(Document doc) {
        return SHelper.replaceSpaces(doc.select("link[rel=alternate]").select("link[type=application/rss+xml]").attr("href"));
    }

    protected String extractVideoUrl(Document doc) {
        return SHelper.replaceSpaces(doc.select("head meta[property=og:video]").attr("content"));
    }

    protected String extractFaviconUrl(String url, Document doc) {
        String faviconUrl = SHelper.replaceSpaces(doc.select("head link[rel=icon]").attr("href"));
        if (faviconUrl.isEmpty()) {
            faviconUrl = SHelper.replaceSpaces(doc.select("head link[rel^=shortcut],link[rel$=icon]").attr("href"));
        }
        return parseSrcImage(url, faviconUrl);
    }

    private String parseSrcImage(String url, String imageSrc) {
    	
		if (imageSrc.isEmpty()) {
			return null;
		}
		
		if (imageSrc.contains("?")) {
			imageSrc = imageSrc.substring(0, imageSrc.indexOf("?"));
		}
		
		if (imageSrc.startsWith("http")) {
			return imageSrc;
		}
		
		String rootDomain = null;  

		try {
			
			URL urlParsed = new URL(url);
			String protocol = urlParsed.getProtocol();
	        String host = urlParsed.getHost();
	        int port = urlParsed.getPort();
	        
	        rootDomain = String.format("%s://%s%s", protocol, host, "/");
	        if (port != -1) {
	        		rootDomain = String.format("%s://%s:%d%s", protocol, host, port, "/");
	        }
	        
		} catch (MalformedURLException e) { 
			
		}
		
		boolean externalDomain = imageSrc.startsWith("//");
		
		if (externalDomain) {
			
			imageSrc = "http:" + imageSrc;
			
		} else {
		
			if (imageSrc.indexOf("/") == 0) {
				imageSrc = imageSrc.substring(1);
			}
			
			if (imageSrc.startsWith("/.")) {
				imageSrc = imageSrc.replaceFirst("/.", "");
			}
			
			if (!imageSrc.contains("http")){ 
				if (StringUtils.isEmpty(rootDomain)) {
					rootDomain = url;
					if (!url.endsWith("/")) {
						rootDomain += "/";
					}
				}
				imageSrc = rootDomain + imageSrc + "";  
			}  
		}
		
		return imageSrc;
	}
    
    /**
     * Weights current element. By matching it with positive candidates and weighting child nodes. Since it's impossible to predict which exactly
     * names, ids or class names will be used in HTML, major role is played by child nodes
     *
     * @param e Element to weight, along with child nodes
     */
    protected int getWeight(Element e) {
        int weight = calcWeight(e);
        weight += (int) Math.round(e.ownText().length() / 100.0 * 10);
        weight += weightChildNodes(e);
        return weight;
    }

    /**
     * Weights a child nodes of given Element. During tests some difficulties were met. For instance, not every single document has nested paragraph
     * tags inside of the major article tag. Sometimes people are adding one more nesting level. So, we're adding 4 points for every 100 symbols
     * contained in tag nested inside of the current weighted element, but only 3 points for every element that's nested 2 levels deep. This way we
     * give more chances to extract the element that has less nested levels, increasing probability of the correct extraction.
     *
     * @param rootEl Element, who's child nodes will be weighted
     */
    protected int weightChildNodes(Element rootEl) {
        int weight = 0;
        Element caption = null;
        List<Element> pEls = new ArrayList<Element>(5);
        for (Element child : rootEl.children()) {
            String ownText = child.ownText();
            int ownTextLength = ownText.length();
            if (ownTextLength < 20)
                continue;

            if (ownTextLength > 200)
                weight += Math.max(50, ownTextLength / 10);

            if (child.tagName().equals("h1") || child.tagName().equals("h2")) {
                weight += 30;
            } else if (child.tagName().equals("div") || child.tagName().equals("p")) {
                weight += calcWeightForChild(child, ownText);
                if (child.tagName().equals("p") && ownTextLength > 50)
                    pEls.add(child);

                if (child.className().toLowerCase().equals("caption"))
                    caption = child;
            }
        }

        // use caption and image
        if (caption != null)
            weight += 30;

        if (pEls.size() >= 2) {
            for (Element subEl : rootEl.children()) {
                if ("h1;h2;h3;h4;h5;h6".contains(subEl.tagName())) {
                    weight += 20;
                    // headerEls.add(subEl);
                } else if ("table;li;td;th".contains(subEl.tagName())) {
                    addScore(subEl, -30);
                }

                if ("p".contains(subEl.tagName()))
                    addScore(subEl, 30);
            }
        }
        return weight;
    }

    public void addScore(Element el, int score) {
        int old = getScore(el);
        setScore(el, score + old);
    }

    public int getScore(Element el) {
        int old = 0;
        try {
            old = Integer.parseInt(el.attr("gravityScore"));
        } catch (Exception ex) {
        }
        return old;
    }

    public void setScore(Element el, int score) {
        el.attr("gravityScore", Integer.toString(score));
    }

    private int calcWeightForChild(Element child, String ownText) {
        int c = SHelper.count(ownText, "&quot;");
        c += SHelper.count(ownText, "&lt;");
        c += SHelper.count(ownText, "&gt;");
        c += SHelper.count(ownText, "px");
        int val;
        if (c > 5)
            val = -30;
        else
            val = (int) Math.round(ownText.length() / 25.0);

        addScore(child, val);
        return val;
    }

    private int calcWeight(Element e) {
        int weight = 0;
        if (POSITIVE.matcher(e.className()).find())
            weight += 35;

        if (POSITIVE.matcher(e.id()).find())
            weight += 40;

        if (UNLIKELY.matcher(e.className()).find())
            weight -= 20;

        if (UNLIKELY.matcher(e.id()).find())
            weight -= 20;

        if (NEGATIVE.matcher(e.className()).find())
            weight -= 50;

        if (NEGATIVE.matcher(e.id()).find())
            weight -= 50;

        String style = e.attr("style");
        if (style != null && !style.isEmpty() && NEGATIVE_STYLE.matcher(style).find())
            weight -= 50;
        return weight;
    }

    public Element determineImageSource(Element el, List<ImageResult> images) {
    	
        int maxWeight = -1;
        Element maxNode = null;
        Elements els = el.select("img");
        if (els.isEmpty())
            els = el.parent().select("img");

        double score = 1;
        Set<ImageResult> imageSet = new HashSet<ImageResult>();
        for (Element e : els) {
            String sourceUrl = e.attr("src");
            if (sourceUrl.isEmpty() || isAdImage(sourceUrl) || isCanvasImage(sourceUrl))
                continue;

            int weight = 0;
            int height = 0;
            int width = 0;
            try {
                String usemap = e.attr("usemap");
                if (!usemap.isEmpty()) {
                    continue;
                }
                
                height = DECIMAL_PARSER.parse(e.attr("height")).intValue();
                width = DECIMAL_PARSER.parse(e.attr("width")).intValue();
                if (height <= 50 && width <= 50) {
                    continue;
                }
                if (height >= 50)
                    weight += height/2;
                else
                    weight -= 20;

                if (width >= 50)
                    weight += width/2;
                else
                    weight -= 20;
            } catch (Exception ex) {
            }
            String alt = e.attr("alt");
            if (alt.length() > 35)
                weight += 20;

            String title = e.attr("title");
            if (title.length() > 35)
                weight += 20;

            String rel = null;
            boolean noFollow = false;
            if (e.parent() != null) {
                rel = e.parent().attr("rel");
                if (rel != null && rel.contains("nofollow")) {
                    noFollow = rel.contains("nofollow");
                    weight -= 40;
                }
            }

            int distance = StringUtils.getLevenshteinDistance(el.absUrl("img"), sourceUrl);
            if (distance < 100) {
                weight += 100 - distance;
            }
            weight = (int) (weight * score);
            if (weight > maxWeight) {
                maxWeight = weight;
                maxNode = e;
                score = score / 2;
            }

            ImageResult image = new ImageResult(sourceUrl, weight, title, height, width, alt, noFollow);
            imageSet.add(image);
        }

        if (imageSet.isEmpty()) {
            return null;
        }

        images.addAll(imageSet);
        Collections.sort(images, new ImageComparator());
        return maxNode;
    }

    private boolean isCanvasImage(String imageUrl) {
        return CANVAS_IMAGE.matcher(imageUrl).find();
    }

    /**
     * Prepares document. Currently only stipping unlikely candidates, since from time to time they're getting more score than good ones especially in
     * cases when major text is short.
     *
     * @param doc document to prepare. Passed as reference, and changed inside of function
     */
    protected void prepareDocument(Document doc) {
        // stripUnlikelyCandidates(doc);
        removeScriptsAndStyles(doc);
    }

    /**
     * Removes unlikely candidates from HTML. Currently takes id and class name and matches them against list of patterns
     *
     * @param doc document to strip unlikely candidates from
     */
    protected void stripUnlikelyCandidates(Document doc) {
        for (Element child : doc.select("body").select("*")) {
            String className = child.className().toLowerCase();
            String id = child.id().toLowerCase();

            if (NEGATIVE.matcher(className).find() || NEGATIVE.matcher(id).find()) {
                // print("REMOVE:", child);
                child.remove();
            }
        }
    }

    private Document removeScriptsAndStyles(Document doc) {
        Elements scripts = doc.getElementsByTag("script");
        for (Element item : scripts) {
            item.remove();
        }

        Elements noscripts = doc.getElementsByTag("noscript");
        for (Element item : noscripts) {
            item.remove();
        }

        Elements styles = doc.getElementsByTag("style");
        for (Element style : styles) {
            style.remove();
        }

        return doc;
    }

    private boolean isAdImage(String imageUrl) {
        return SHelper.count(imageUrl, "ad") >= 2;
    }

    /**
     * Match only exact matching as longestSubstring can be too fuzzy
     */
    public String removeTitleFromText(String text, String title) {
        // don't do this as its terrible to read
        // int index1 = text.toLowerCase().indexOf(title.toLowerCase());
        // if (index1 >= 0)
        // text = text.substring(index1 + title.length());
        // return text.trim();
        return text;
    }

    /**
     * @return a set of all important nodes
     */
    public Collection<Element> getNodes(Document doc) {
        Map<Element, Object> nodes = new LinkedHashMap<Element, Object>(64);
        int score = 100;
        for (Element el : doc.select("body").select("*")) {
            if (NODES.matcher(el.tagName()).matches()) {
                nodes.put(el, null);
                setScore(el, score);
                score = score / 2;
            }
        }
        return nodes.keySet();
    }

    public String cleanTitle(String title) {
        StringBuilder res = new StringBuilder();
        // int index = title.lastIndexOf("|");
        // if (index > 0 && title.length() / 2 < index)
        // title = title.substring(0, index + 1);

        int counter = 0;
        String[] strs = title.split("\\|");
        for (String part : strs) {
            if (IGNORED_TITLE_PARTS.contains(part.toLowerCase().trim()))
                continue;

            if (counter == strs.length - 1 && res.length() > part.length())
                continue;

            if (counter > 0)
                res.append("|");

            res.append(part);
            counter++;
        }

        return SHelper.innerTrim(res.toString());
    }

    /**
     * Comparator for Image by weight
     *
     * @author Chris Alexander, chris@chris-alexander.co.uk
     *
     */
    public class ImageComparator implements Comparator<ImageResult> {

        @Override
        public int compare(ImageResult o1, ImageResult o2) {
            // Returns the highest weight first
            return o2.weight.compareTo(o1.weight);
        }
    }
}
