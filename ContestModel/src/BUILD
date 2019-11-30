java_library(
    name = "contestModel",
    srcs = glob(["**/*.java"], exclude = ["**/*Test.java"]),
    deps = ["//ContestModel/lib:snakeyaml"],
    visibility = ["//visibility:public"],
    resources = ["org/icpc/tools/contest/model/util/messages.properties"],
)

java_test(
    name = "YamlParserTest",
    srcs = ["org/icpc/tools/contest/model/internal/YamlParserTest.java"],
    deps = [
        ":contestModel",
        "//lib:guava_junit_truth",
    ],
)
