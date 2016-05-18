package org.telegram.telegrambots.updatesreceivers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.Constants;
import org.telegram.telegrambots.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.api.objects.RequestResult;
import org.telegram.telegrambots.bots.ITelegramLongPollingBot;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * @brief Thread to request updates with active wait
 * @date 20 of June of 2015
 */
public class BotSession {

    private static final Logger log = LoggerFactory
        .getLogger(BotSession.class.getName());

    private static final int SOCKET_TIMEOUT = 30 * 1000;

    private final ITelegramLongPollingBot callback;
    private final String token;

    //TODO move arguments to startListen
    public BotSession(final String token,
        final ITelegramLongPollingBot callback) {
        this.token = token;
        this.callback = callback;
        startListen();
    }

    private void startListen() {
        try (CloseableHttpClient httpclient = HttpClientBuilder.create()
            .setSSLHostnameVerifier(new NoopHostnameVerifier())
            .setConnectionTimeToLive(20, TimeUnit.SECONDS).build()) {
            int lastReceivedUpdate = 0;
            while (true) {
                final GetUpdates request = new GetUpdates();
                request.setLimit(100);
                request.setTimeout(20);
                request.setOffset(lastReceivedUpdate + 1);

                final String url = Constants.BASEURL + token + "/"
                    + GetUpdates.PATH;
                // config
                final RequestConfig defaultRequestConfig = RequestConfig
                    .custom().build();
                final RequestConfig requestConfig = RequestConfig
                    .copy(defaultRequestConfig).setSocketTimeout(SOCKET_TIMEOUT)
                    .setConnectTimeout(SOCKET_TIMEOUT)
                    .setConnectionRequestTimeout(SOCKET_TIMEOUT).build();
                // http client
                final HttpPost httpPost = new HttpPost(url);
                httpPost.addHeader("charset", StandardCharsets.UTF_8.name());
                httpPost.setConfig(requestConfig);
                httpPost.setEntity(new StringEntity(request.toJson().toString(),
                    ContentType.APPLICATION_JSON));

                try {
                    final HttpResponse response = httpclient.execute(httpPost);
                    final HttpEntity ht = response.getEntity();
                    final BufferedHttpEntity buf = new BufferedHttpEntity(ht);
                    final String responseContent = EntityUtils.toString(buf,
                        StandardCharsets.UTF_8);
                    log.debug("Telegram response:{}", responseContent);
                    final RequestResult rr = new RequestResult(responseContent);

                    callback.onUpdateReceived(rr);
                    lastReceivedUpdate = rr.offset().orElse(lastReceivedUpdate);
                } catch (final ClientProtocolException e) {
                    log.error(e.getMessage(), e);
                } catch (final IOException e) {
                    log.error(e.getMessage(), e);
                }

            }

        } catch (final IOException exc) {
            log.error(exc.getMessage(), exc);
        }
    }

}
