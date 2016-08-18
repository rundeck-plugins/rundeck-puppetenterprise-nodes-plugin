package com.rundeck.plugin.resources.puppetdb.client.model;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by greg on 3/8/16.
 */
public class CertNodeResource extends NodeResource implements Certname{
    public static final Type LIST = new TypeToken<List<CertNodeResource>>() {}.getType();

    public static Function<String, List<CertNodeResource>> listParser(final Gson gson) {
        return new Function<String, List<CertNodeResource>>() {
            @Override
            public List<CertNodeResource> apply(final String input) {
                return gson.fromJson(input, CertNodeResource.LIST);
            }

        };
    }

    private String certname;

    public String getCertname() {
        return certname;
    }

    public void setCertname(String certname) {
        this.certname = certname;
    }

    @Override
    public String toString() {
        return "com.rundeck.plugin.resources.puppetdb.client.model.CertNodeClass{" +
               "certname='" + certname + '\'' +
               '}';
    }
}
