load("@io_bazel_rules_java_war//java_war:defs.bzl", "java_war")

java_war(
    name = "cds",
    java_srcs = glob(["src/**/*.java"]),
    web_app_dir = "WebContent",
    deps = [
        ":javaee",
        "//ContestModel/src:contestModel",
        "//PresContest/src:presentations",
        "//PresCore/lib:tyrus-standalone",
    ],
)

java_import(
    name = "javaee",
    jars = [
        "com.ibm.ws.javaee.jsp.2.2_1.0.0.jar",
        "com.ibm.ws.javaee.servlet.3.0_1.0.1.jar",
        "com.ibm.ws.javaee.websocket.1.1_1.0.12.jar",
    ],
)
