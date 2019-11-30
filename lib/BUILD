java_library(
    name = "guava_junit_truth",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = [
        ":guava",
        ":junit4",
        ":truth",
    ],
)

java_import(
    name = "guava",
    jars = ["guava-28.1-jre.jar"],
    exports = [
        ":error_prone_annotations",
        ":jsr305",
    ],
)

java_import(
    name = "error_prone_annotations",
    jars = ["error_prone_annotations-2.3.1.jar"],
)

java_import(
    name = "jsr305",
    jars = ["jsr305-3.0.2.jar"],
)

java_import(
    name = "junit4",
    jars = ["junit-4.12.jar"],
)

java_import(
    name = "truth",
    jars = ["truth-1.0.jar"],
    deps = [
        ":diffutils",
    ],
)

java_import(
    name = "diffutils",
    jars = ["diffutils-1.3.0.jar"],
)
