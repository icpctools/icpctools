load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JAVA_WAR_TAG = "0.1.0"

http_archive(
    name = "io_bazel_rules_java_war",
    strip_prefix = "rules_java_war-%s" % RULES_JAVA_WAR_TAG,
    url = "https://github.com/bmuschko/rules_java_war/archive/%s.tar.gz" % RULES_JAVA_WAR_TAG,
    sha256 = "38011f979713c4aefd43ab56675ce4c6c14bc949b128c3a303f1f57ebe4bfeac",
)
