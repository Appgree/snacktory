package de.jetwick.snacktory;

import org.jsoup.nodes.Element;

/**
 * Class which encapsulates the data from an image found under an element
 *
 * @author Chris Alexander, chris@chris-alexander.co.uk
 */
public class ImageResult {

    public String src;
    public Integer weight;
    public String title;
    public int height;
    public int width;
    public String alt;
    public boolean noFollow;
    public Element element;

    public ImageResult(String src, Integer weight, String title, int height, int width, String alt, boolean noFollow) {
        this.src = src;
        this.weight = weight;
        this.title = title;
        this.height = height;
        this.width = width;
        this.alt = alt;
        this.noFollow = noFollow;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((alt == null) ? 0 : alt.hashCode());
        result = prime * result + ((element == null) ? 0 : element.hashCode());
        result = prime * result + height;
        result = prime * result + (noFollow ? 1231 : 1237);
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((weight == null) ? 0 : weight.hashCode());
        result = prime * result + width;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImageResult other = (ImageResult) obj;
        if (alt == null) {
            if (other.alt != null)
                return false;
        } else if (!alt.equals(other.alt))
            return false;
        if (element == null) {
            if (other.element != null)
                return false;
        } else if (!element.equals(other.element))
            return false;
        if (height != other.height)
            return false;
        if (noFollow != other.noFollow)
            return false;
        if (src == null) {
            if (other.src != null)
                return false;
        } else if (!src.equals(other.src))
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        if (weight == null) {
            if (other.weight != null)
                return false;
        } else if (!weight.equals(other.weight))
            return false;
        if (width != other.width)
            return false;
        return true;
    }
}
