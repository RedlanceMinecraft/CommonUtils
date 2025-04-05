package org.redlance.common.utils.requester;

import com.github.mizosoft.methanol.MutableRequest;
import webGrude.Webgrude;
import webGrude.http.GetException;
import webGrude.http.LinkVisitor;
import webGrude.mapping.TooManyResultsException;
import webGrude.mapping.annotations.Page;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Like {@link webGrude.OkHttpBrowser} but for native client
 */
public class JavaLinkVisitor implements LinkVisitor {
    protected final Webgrude webgrude = new Webgrude();
    protected final HttpClient httpClient;

    public JavaLinkVisitor(HttpClient client) {
        this.httpClient = client;
    }

    /**
     * Loads content from url from the Page annotation on pageClass onto an instance of pageClass.
     *
     * @param <T>       An instance of the class with {@literal @}Selector annotation
     * @param pageClass A class with a {@literal @}Selector annotation
     * @param params    Optional, if the pageClass has a url with parameters
     * @return The class instantiated and with the fields with the
     * {@literal @}Selector annotation populated.
     * @throws webGrude.http.GetException When calling get on the BrowserClient raises an exception
     */
    public <T> T get(final Class<T> pageClass, final String... params) {
        cryIfNotAnnotated(pageClass);
        final String url = webgrude.url(pageClass, params);
        return get(url, pageClass);
    }

    /***
     * Loads content from given url onto an instance of pageClass.
     *
     * @param <T>       An instance of the class with a {@literal @}Page annotation
     * @param url   The url to load.
     * @param pageClass A class with a {@literal @}Selector annotation
     * @return The class instantiated and with the fields with the
     * {@literal @}Selector annotation populated.
     * @throws webGrude.http.GetException          When calling get on the BrowserClient raises an exception
     * @throws webGrude.mapping.elements.WrongTypeForField When a field have a type incompatible with the page html, example a <p>foo</p> on a float field
     * @throws TooManyResultsException    When a field maps to a type but the css selector returns more than one element
     */
    public <T> T get(final String url, final Class<T> pageClass) {
        final String pageContent = getPage(url);
        return webgrude.map(pageContent, pageClass);
    }

    private static <T> void cryIfNotAnnotated(final Class<T> pageClass) {
        if (!pageClass.isAnnotationPresent(Page.class)) {
            throw new RuntimeException("To be mapped from a page, the class must be annotated  @" + Page.class.getSimpleName());
        }
    }

    /***
     * Returns the page from the url.
     * @param url to do a get request
     * @return the get body response
     */
    public String getPage(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new GetException(e, url);
        }
    }

    @Override
    public String visitLink(String href) {
        return getPage(href);
    }
}
