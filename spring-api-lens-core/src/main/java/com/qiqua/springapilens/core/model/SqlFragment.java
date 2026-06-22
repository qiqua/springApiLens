package com.qiqua.springapilens.core.model;

import java.util.List;

public record SqlFragment(
    String relativeFile,
    String mapperNamespace,
    String mapperMethod,
    String sqlText,
    List<String> tables,
    String operationType
) {
}
