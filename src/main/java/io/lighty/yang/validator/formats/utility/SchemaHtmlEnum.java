package io.lighty.yang.validator.formats.utility;

import static java.util.Objects.requireNonNull;

public enum SchemaHtmlEnum {
    CONTAINER("container",       " <span><i class=\"fas fa-folder-open\"></i></span> </td>"),
    LIST("list",                 " <span><i class=\"fas fa-list\"></i></span> </td>"),
    LEAF("leaf",                 " <span><i class=\"fas fa-leaf\"></i></span> </td>"),
    LEAF_LIST("leaf-list",       " <span><i class=\"fab fa-pagelines\"></i></span> </td>"),
    AUGMENT("augment",           " <span><i class=\"fas fa-external-link-alt\"></i></span> </td>"),
    RPC("rpc",                   " <span><i class=\"fas fa-envelope\"></i></span> </td>"),
    NOTIFICATION("notification", " <span><i class=\"fas fa-bell\"></i></span> </td>"),
    CHOICE("choice",             " <span><i class=\"fas fa-tasks\"></i></span> </td>"),
    CASE("case",                 " <span><i class=\"fas fa-check\"></i></span> </td>"),
    INPUT("input",               " <span><i class=\"fas fa-share\"></i></span> </td>"),
    OUTPUT("output",             " <span><i class=\"fas fa-reply\"></i></span> </td>"),
    ACTION("action",             " <span><i class=\"fas fa-play\"></i></span> </td>"),
    EMPTY("", "");

    private final String schemaName;
    private final String htmlValue;

    SchemaHtmlEnum(final String schemaName, final String htmlValue) {
        this.schemaName = requireNonNull(schemaName);
        this.htmlValue = requireNonNull(htmlValue);
    }

    public String getSchemaName() {
        return schemaName;
    }

    public static SchemaHtmlEnum getSchemaHtmlEnumByName(final String value) {
        for (final SchemaHtmlEnum schemaHtmlEnum : values()) {
            if (schemaHtmlEnum.getSchemaName().equals(value)) {
                return schemaHtmlEnum;
            }
        }
        return SchemaHtmlEnum.EMPTY;
    }

    public String getHtmlValue() {
        return htmlValue;
    }
}