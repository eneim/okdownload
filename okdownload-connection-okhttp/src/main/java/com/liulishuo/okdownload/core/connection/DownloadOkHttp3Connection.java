/*
 * Copyright (c) 2018 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.okdownload.core.connection;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadOkHttp3Connection implements DownloadConnection, DownloadConnection.Connected {
    @NonNull final OkHttpClient client;
    @NonNull private final Request.Builder requestBuilder;

    private Request request;
    private Response response;

    DownloadOkHttp3Connection(@NonNull OkHttpClient client,
                              @NonNull Request.Builder requestBuilder) {
        this.client = client;
        this.requestBuilder = requestBuilder;
    }

    DownloadOkHttp3Connection(@NonNull OkHttpClient client, @NonNull String url) {
        this(client, new Request.Builder().url(url));
    }

    @Override public void addHeader(String name, String value) {
        this.requestBuilder.addHeader(name, value);
    }

    @Override public Connected execute() throws IOException {
        request = requestBuilder.build();
        response = client.newCall(request).execute();
        return this;
    }

    @Override public void release() {
        request = null;
        if (response != null) response.close();
        response = null;
    }

    @Override public Map<String, List<String>> getRequestProperties() {
        if (request != null) return request.headers().toMultimap();
        else return requestBuilder.build().headers().toMultimap();
    }

    @Override public String getRequestProperty(String key) {
        if (request != null) return request.header(key);
        else return requestBuilder.build().header(key);
    }

    @Override public int getResponseCode() throws IOException {
        if (response == null) throw new IOException("Please invoke execute first!");
        return response.code();
    }

    @Override public InputStream getInputStream() throws IOException {
        if (response == null) throw new IOException("Please invoke execute first!");
        final ResponseBody body = response.body();
        if (body == null) throw new IOException("no body found on response!");
        return body.byteStream();
    }

    @Override public Map<String, List<String>> getResponseHeaderFields() {
        return response == null ? null : response.headers().toMultimap();
    }

    @Override public String getResponseHeaderField(String name) {
        return response == null ? null : response.header(name);
    }

    public static class Factory implements DownloadConnection.Factory {

        private OkHttpClient.Builder builder;
        private volatile OkHttpClient client;

        public Factory setBuilder(@NonNull OkHttpClient.Builder builder) {
            this.builder = builder;
            return this;
        }

        @NonNull public OkHttpClient.Builder builder() {
            if (builder == null) builder = new OkHttpClient.Builder();
            return builder;
        }

        @Override public DownloadConnection create(String url) throws IOException {
            if (client == null) {
                synchronized (Factory.class) {
                    if (client == null) {
                        client = builder != null ? builder.build() : new OkHttpClient();
                        builder = null;
                    }
                }
            }

            return new DownloadOkHttp3Connection(client, url);
        }
    }
}
