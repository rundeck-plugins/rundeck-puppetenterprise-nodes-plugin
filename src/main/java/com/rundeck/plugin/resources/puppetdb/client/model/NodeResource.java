package com.rundeck.plugin.resources.puppetdb.client.model;

import java.lang.reflect.Type;
import java.util.List;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class NodeClass {

    public static final Type LIST = new TypeToken<List<NodeClass>>() {}.getType();

    public static Function<String, List<NodeClass>> parser(final Gson gson) {
        return new Function<String, List<NodeClass>>() {
            @Override
            public List<NodeClass> apply(final String input) {
                return gson.fromJson(input, NodeClass.LIST);
            }

        };
    }

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "NodeClass{" +
                "title='" + title + '\'' +
                '}';
    }

}

