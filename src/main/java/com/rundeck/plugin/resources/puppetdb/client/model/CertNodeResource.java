package com.rundeck.plugin.resources.puppetdb.client.model;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by greg on 3/8/16.
 */
public class CertNodeClass extends NodeClass implements Certname{
    public static final Type LIST = new TypeToken<List<CertNodeClass>>() {}.getType();

    public static Function<String, List<CertNodeClass>> listParser(final Gson gson) {
        return new Function<String, List<CertNodeClass>>() {
            @Override
            public List<CertNodeClass> apply(final String input) {
                return gson.fromJson(input, CertNodeClass.LIST);
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
