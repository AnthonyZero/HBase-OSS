package com.pingjin.oss.common;

import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;

public class OssObject {

    private ObjectMetaData metaData;

    private InputStream content;

    private Response response;

    public OssObject() {

    }

    public OssObject(Response response) {
        this.response = response;
    }

    public void close() {
      try {
          if (content != null) {
              this.content.close();
          }
          if (response != null) {
              this.response.close();
          }
      } catch (IOException ioe) {
          //nothing to do
      }
    }

    public ObjectMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(ObjectMetaData metaData) {
        this.metaData = metaData;
    }

    public InputStream getContent() {
        return content;
    }

    public void setContent(InputStream content) {
        this.content = content;
    }


}
