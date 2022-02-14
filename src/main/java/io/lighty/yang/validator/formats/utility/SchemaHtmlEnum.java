package io.lighty.yang.validator.formats.utility;

public enum SchemaHtmlEnum {
    CONTAINER("container") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-folder-open\"></i></span> </td>";
        }
    },
    LIST("list") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-list\"></i></span> </td>";
        }
    },
    LEAF("leaf") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-leaf\"></i></span> </td>";
        }
    },
    LEAF_LIST("leaf-list") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fab fa-pagelines\"></i></span> </td>";
        }
    },
    AUGMENT("augment") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-external-link-alt\"></i></span> </td>";
        }
    },
    RPC("rpc") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-envelope\"></i></span> </td>";
        }
    },
    NOTIFICATION("notification") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-bell\"></i></span> </td>";
        }
    },
    CHOICE("choice") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-tasks\"></i></span> </td>";
        }
    },
    CASE("case") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-check\"></i></span> </td>";
        }
    },
    INPUT("input") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-share\"></i></span> </td>";
        }
    },
    OUTPUT("output") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-reply\"></i></span> </td>";
        }
    },
    ACTION("action") {
        @Override
        public String getHtmlValue() {
            return " <span><i class=\"fas fa-play\"></i></span> </td>";
        }
    },
    EMPTY("") {
        @Override
        public String getHtmlValue() {
            return "";
        }
    };

    private final String schemaName;

    SchemaHtmlEnum(final String schemaName) {
        this.schemaName = schemaName;
    }

    public String getSchemaName() {
        return this.schemaName;
    }

    public static SchemaHtmlEnum getSchemaHtmlEnumByName(final String value) {
        for (final SchemaHtmlEnum schemaHtmlEnum : values()) {
            if (schemaHtmlEnum.getSchemaName().equals(value)) {
                return schemaHtmlEnum;
            }
        }
        return SchemaHtmlEnum.EMPTY;
    }

    public abstract String getHtmlValue();
}