package com.seeyaa.tutor.http;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.util.CharsetUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.seeyaa.tutor.vo.EntityWrapper;
import com.seeyaa.tutor.vo.UploadAvatarWrapper;

public class MultiparttRequest extends Request<UploadAvatarWrapper> {

    MultipartEntityBuilder entity = MultipartEntityBuilder.create();
    HttpEntity httpentity;
    private String FILE_PART_NAME = "files";

    private final Response.Listener<UploadAvatarWrapper> mListener;
    private final File mFilePart;
    private final Map<String, String> mStringPart;
    private Map<String, String> headerParams;
    private final MultipartProgressListener multipartProgressListener;
    private long fileLength = 0L;

    public MultiparttRequest(String url, Response.ErrorListener errorListener,
            Response.Listener<UploadAvatarWrapper> listener, File file, long fileLength,
            Map<String, String> mStringPart,
            final Map<String, String> headerParams, String partName,
            MultipartProgressListener progLitener) {
        super(Method.POST, url, errorListener);

        this.mListener = listener;
        this.mFilePart = file;
        this.fileLength = fileLength;
        this.mStringPart = mStringPart;
        this.headerParams = headerParams;
        this.FILE_PART_NAME = partName;
        this.multipartProgressListener = progLitener;

        entity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        try {
            entity.setCharset(CharsetUtils.get("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        buildMultipartEntity();
        httpentity = entity.build();
    }

    // public void addStringBody(String param, String value) {
    // if (mStringPart != null) {
    // mStringPart.put(param, value);
    // }
    // }

    private void buildMultipartEntity() {
        entity.addPart(FILE_PART_NAME, new FileBody(mFilePart, ContentType.create("image/gif"), mFilePart.getName()));
        if (mStringPart != null) {
            for (Map.Entry<String, String> entry : mStringPart.entrySet()) {
                entity.addTextBody(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public String getBodyContentType() {
        return httpentity.getContentType().getValue();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            httpentity.writeTo(new CountingOutputStream(bos, fileLength,
                    multipartProgressListener));
        } catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream");
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<UploadAvatarWrapper> parseNetworkResponse(NetworkResponse response) {
    	   String parsed;
           try {
               parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
           } catch (UnsupportedEncodingException e) {
               parsed = new String(response.data);
           }
           return Response.success(new Gson().fromJson(parsed, UploadAvatarWrapper.class), 
           		HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(UploadAvatarWrapper response) {
        mListener.onResponse(response);
    }

//Override getHeaders() if you want to put anything in header

    public static interface MultipartProgressListener {
        void transferred(long transfered, int progress);
    }

    public static class CountingOutputStream extends FilterOutputStream {
        private final MultipartProgressListener progListener;
        private long transferred;
        private long fileLength;

        public CountingOutputStream(final OutputStream out, long fileLength,
                final MultipartProgressListener listener) {
            super(out);
            this.fileLength = fileLength;
            this.progListener = listener;
            this.transferred = 0;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            if (progListener != null) {
                this.transferred += len;
                int prog = (int) (transferred * 100 / fileLength);
                this.progListener.transferred(this.transferred, prog);
            }
        }

        public void write(int b) throws IOException {
            out.write(b);
            if (progListener != null) {
                this.transferred++;
                int prog = (int) (transferred * 100 / fileLength);
                this.progListener.transferred(this.transferred, prog);
            }
        }

    }
}